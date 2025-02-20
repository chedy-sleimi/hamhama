import mysql.connector
import json
import ast
import os
import requests
import time
import re

# Configuration
GEMINI_API_KEY = "AIzaSyA1rIiPUSVC4ynPc28PKZ5XRhFc1cd-viE"
API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
HEADERS = {'Content-Type': 'application/json'}

# MySQL Configuration
db_config = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "hamhama_db"
}

# Unit normalization mapping
UNIT_NORMALIZATION = {
    'tbs': 'Tbsp', 'tbsp': 'Tbsp', 'tablespoon': 'Tbsp', 'tablespoons': 'Tbsp',
    'tsp': 'tsp', 'teaspoon': 'tsp', 'teaspoons': 'tsp',
    'lb': 'lb', 'pound': 'lb', 'lbs': 'lb', '#': 'lb',
    'oz': 'oz', 'ounce': 'oz', 'ounces': 'oz',
    'cup': 'cup', 'cups': 'cup', 'c': 'cup',
    'pt': 'pint', 'pint': 'pint', 'pints': 'pint',
    'qt': 'quart', 'quart': 'quart', 'quarts': 'quart',
    'gal': 'gallon', 'gallon': 'gallon',
    'mg': 'mg', 'milligram': 'mg', 'milligrams': 'mg',
    'g': 'g', 'gram': 'g', 'grams': 'g',
    'kg': 'kg', 'kilogram': 'kg',
    'ml': 'ml', 'milliliter': 'ml', 'milliliters': 'ml',
    'l': 'l', 'liter': 'l', 'liters': 'l',
    'pinch': 'pinch', 'dash': 'dash', 
    'whole': 'whole', 'clove': 'clove', 'cloves': 'clove',
    'bunch': 'bunch', 'stalk': 'stalk', 'leaf': 'leaf',
    'slice': 'slice', 'can': 'can', 'jar': 'jar'
}

PROMPT = """You are an expert recipe parser. Transform ingredients into JSON with EXACTLY this structure:
{
  "Ingredients": [
    {"quantity": number|null, "unit": "unit"|null, "name": "clean_name"}
  ]
}

RULES:
1. Quantity: 
   - Extract only numeric values (1, 0.5, 0.25)
   - Use null if no quantity (e.g., "salt" → null)
   - Convert fractions to decimals ("¼" → 0.25)

2. Unit:
   - Use standard units from this list only: Tbsp, tsp, cup, lb, oz, g, kg, ml, l, pinch, dash, whole, clove, stalk, leaf, slice, can, jar
   - Default to "pinch" for spices/herbs without units
   - Use "whole" for complete items ("1 chicken")
   - null for non-spice ingredients without units

3. Name:
   - Remove ALL measurements, sizes, and preparation notes
   - Delete parentheticals and descriptive clauses
   - Keep only base ingredient ("2 small acorn squash" → "acorn squash")

EXAMPLE INPUT:
["1 (3½–4-lb.) whole chicken", "Freshly ground black pepper"]

EXAMPLE OUTPUT:
{
  "Ingredients": [
    {"quantity": 1, "unit": "whole", "name": "chicken"},
    {"quantity": null, "unit": "pinch", "name": "black pepper"}
  ]
}

NOW PROCESS THIS INPUT:"""

def normalize_unit(unit):
    """Standardize unit variations with case-insensitive check"""
    if not unit:
        return None
    unit = unit.lower().rstrip('s.')
    return UNIT_NORMALIZATION.get(unit, None)

def parse_ingredients_with_gemini(ingredients):
    """Send ingredients to Gemini API for parsing with retry logic"""
    full_prompt = f"{PROMPT}\n{json.dumps(ingredients, indent=2)}"
    
    payload = {
        "contents": [{
            "parts": [{"text": full_prompt}]
        }],
        "safetySettings": [{
            "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
            "threshold": "BLOCK_NONE"
        }]
    }

    for attempt in range(3):
        try:
            response = requests.post(
                f"{API_ENDPOINT}?key={GEMINI_API_KEY}",
                headers=HEADERS,
                json=payload,
                timeout=60
            )
            response.raise_for_status()
            
            # Extract and clean JSON from response
            response_text = response.json()['candidates'][0]['content']['parts'][0]['text']
            cleaned_response = re.sub(r'(?s)^.*?({.*}).*$', r'\1', response_text)
            return json.loads(cleaned_response)
            
        except Exception as e:
            if attempt < 2:
                print(f"⚠️ Retrying... Attempt {attempt+1}/3")
                time.sleep(2 ** attempt)
            else:
                print(f"❌ Gemini API Error: {str(e)}")
                return None

def clean_ingredient_name(name):
    """Thorough cleaning of ingredient names"""
    # Remove measurements, units, and special characters
    name = re.sub(
        r'''(?xi)
        \([^)]*\)|          # Parentheticals
        [\d½¼¾⅓⅔⅛⅜⅝⅞]|      # Numbers and fractions
        tsp|tbsp|cup|lb|oz|  # Common units
        pinch|dash|whole|     # Other units
        small|medium|large|   # Sizes
        chopped|diced|        # Preparations
        to taste|optional|    # Descriptors
        ["'<>]                # Special characters
        ''',
        '',
        name
    ).strip()
    
    # Remove extra whitespace and commas
    return re.sub(r'\s{2,}|,\s*$', ' ', name).strip()

def main():
    conn = None
    try:
        # Connect to MySQL
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Load recipe data
        json_file = os.path.join(os.path.dirname(__file__), "recipe_data.json")
        with open(json_file, "r", encoding="utf-8") as f:
            recipes = json.load(f)

        for recipe in recipes:
            recipe_name = recipe["Title"].strip()
            
            # Check if recipe already exists
            cursor.execute(
                "SELECT id FROM recipes WHERE name = %s",
                (recipe_name,)
            )
            existing_recipe = cursor.fetchone()
            
            if existing_recipe:
                print(f"⏩ Skipping existing recipe: {recipe_name}")
                continue

            ingredients_list = ast.literal_eval(recipe["Cleaned_Ingredients"])

            print(f"\n🔹 Processing: {recipe_name}")
            
            # Get parsed ingredients from Gemini
            parsed_data = parse_ingredients_with_gemini(ingredients_list)
            if not parsed_data or 'Ingredients' not in parsed_data:
                print(f"❌ Failed to parse ingredients for {recipe_name}")
                continue

            # Insert new recipe
            cursor.execute(
                "INSERT INTO recipes (name, description) VALUES (%s, '')",
                (recipe_name,)
            )
            recipe_id = cursor.lastrowid

            # Process ingredients
            for ingredient in parsed_data['Ingredients']:
                # Clean and validate
                clean_name = clean_ingredient_name(ingredient['name'])
                if not clean_name:
                    continue

                # Normalize unit
                normalized_unit = normalize_unit(ingredient.get('unit'))
                
                # Validate quantity
                try:
                    quantity = float(ingredient['quantity']) if ingredient['quantity'] is not None else None
                except (TypeError, ValueError):
                    quantity = None

                # Insert ingredient
                cursor.execute(
                    "INSERT IGNORE INTO ingredients (name) VALUES (%s)",
                    (clean_name,)
                )
                conn.commit()
                
                # Get ingredient ID
                cursor.execute(
                    "SELECT id FROM ingredients WHERE name = %s",
                    (clean_name,)
                )
                result = cursor.fetchone()
                
                if result:
                    # Insert recipe-ingredient relationship
                    cursor.execute(
                        """INSERT INTO recipe_ingredients 
                        (recipe_id, ingredient_id, quantity, unit)
                        VALUES (%s, %s, %s, %s)""",
                        (
                            recipe_id,
                            result[0],
                            quantity,
                            normalized_unit
                        )
                    )
                    print(f"   ✅ {clean_name}: {quantity or '--'} {normalized_unit or ''}")
                
            conn.commit()  # Commit after each recipe
            time.sleep(2)  # Rate limiting

        print("\n🎉 Database population complete!")

    except Exception as e:
        print(f"❌ Critical Error: {str(e)}")
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()

if __name__ == "__main__":
    main()
import mysql.connector
import json
import os
import requests
import time
import re
import math

# --- Configuration ---
# IMPORTANT: Use environment variables or a secure config file in production!
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "AIzaSyA1rIiPUSVC4ynPc28PKZ5XRhFc1cd-viE") # Replace or set env var
# Using a model known for better instruction following and JSON output
API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
HEADERS = {'Content-Type': 'application/json'}
REQUEST_TIMEOUT = 120 # Increase timeout for potentially larger batch responses

# MySQL Configuration
db_config = {
    "host": "localhost",
    "user": "root",
    "password": "",         # Replace with your DB password if needed
    "database": "hamhama_db" # Your database name
}

# --- Batching and Rate Limit Configuration ---
DB_FETCH_BATCH_SIZE = 1000   # How many recipes to fetch from DB at once
GEMINI_PROMPT_BATCH_SIZE = 1000 # How many recipes to include in *one* Gemini API prompt (Adjust based on token limits & performance)
RATE_LIMIT_DELAY = 3      # Delay between Gemini API calls (in seconds)

# --- Category Configuration ---
ALLOWED_CATEGORIES = [
    "BREAKFAST", "LUNCH", "DINNER", "APPETIZER", "SALAD",
    "SOUP", "SIDE_DISH", "DESSERT", "SNACK", "BEVERAGE",
    "CONDIMENT_SAUCE"
]
UNKNOWN_CATEGORY_MARKER = "UNKNOWN" # Marker for Gemini if it cannot classify

# --- Gemini Batch Classification Prompt ---
BATCH_CLASSIFICATION_PROMPT = f"""
You are an expert recipe classifier. You will be given a JSON list of recipes, each with an 'id' and a 'name'.
Your task is to classify EACH recipe based *only* on its 'name' into ONE of the following categories:

{', '.join(ALLOWED_CATEGORIES)}

RULES:
1. Analyze each recipe 'name' individually within the provided JSON list.
2. Choose the single BEST category from the allowed list.
3. If you cannot determine a category from the name alone, use "{UNKNOWN_CATEGORY_MARKER}".
4. Respond ONLY with a valid JSON object containing a single key \"classifications\".
5. The value of \"classifications\" MUST be a JSON list.
6. Each item in the \"classifications\" list MUST be a JSON object containing EXACTLY two keys:
    - \"recipe_id\": The original integer ID of the recipe.
    - \"category\": The determined category string (must be one of the allowed categories or \"{UNKNOWN_CATEGORY_MARKER}\").
7. Ensure the output JSON is well-formed and contains classifications for ALL recipe IDs provided in the input.

EXAMPLE INPUT JSON (part of the prompt text):
```json
[
  {{\"recipe_id\": 101, \"name\": \"Classic Roast Chicken with Vegetables\"}},
  {{\"recipe_id\": 102, \"name\": \"Fluffy Blueberry Pancakes\"}},
  {{\"recipe_id\": 103, \"name\": \"Ingredient Prep Guide\"}}
]
```

EXAMPLE OUTPUT JSON (Your entire response):
```json
{{
  \"classifications\": [
    {{\"recipe_id\": 101, \"category\": \"DINNER\"}},
    {{\"recipe_id\": 102, \"category\": \"BREAKFAST\"}},
    {{\"recipe_id\": 103, \"category\": \"{UNKNOWN_CATEGORY_MARKER}\"}}
  ]
}}
```

NOW PROCESS THE FOLLOWING JSON INPUT LIST OF RECIPES:
"""

def prepare_batch_input_json(recipe_batch):
    """Formats a list of (id, name) tuples into the JSON string for the prompt."""
    input_list = [{"recipe_id": r_id, "name": name} for r_id, name in recipe_batch]
    # Using separators removes extra whitespace, making the JSON more compact
    return json.dumps(input_list, separators=(',', ':'))

def parse_gemini_batch_response(response_text, expected_ids):
    """Parses the JSON response from Gemini, validates it, and returns a mapping."""
    try:
        # Attempt to find JSON block, robust against potential markdown fences json ...
        json_match = re.search(r'json\s*({.*?})\s*', response_text, re.DOTALL | re.IGNORECASE)
        if json_match:
            cleaned_response = json_match.group(1)
        else:
            # Fallback: Assume the entire text might be JSON, try cleaning extraneous text
            cleaned_response = re.sub(r'(?s)^.?({.}).*$', r'\1', response_text)

        data = json.loads(cleaned_response)

        if not isinstance(data, dict) or "classifications" not in data:
            print(f"   ❌ Gemini Error: Response missing 'classifications' key. Response: {response_text[:200]}...")
            return None

        classifications = data["classifications"]
        if not isinstance(classifications, list):
            print(f"   ❌ Gemini Error: 'classifications' is not a list. Response: {response_text[:200]}...")
            return None

        results = {}
        processed_ids = set()
        valid_classifications = 0

        for item in classifications:
            if not isinstance(item, dict) or "recipe_id" not in item or "category" not in item:
                print(f"   ⚠️ Gemini Warning: Invalid item structure in classifications list: {item}")
                continue

            recipe_id = item["recipe_id"]
            category = str(item["category"]).strip().upper() # Standardize category output

            if not isinstance(recipe_id, int):
                print(f"   ⚠️ Gemini Warning: Invalid recipe_id type in item: {item}")
                continue

            if recipe_id not in expected_ids:
                print(f"   ⚠️ Gemini Warning: Received unexpected recipe_id {recipe_id}")
                continue

            # Validate the category
            if category not in ALLOWED_CATEGORIES and category != UNKNOWN_CATEGORY_MARKER:
                print(f"   ⚠️ Gemini Warning: Received invalid category '{category}' for recipe_id {recipe_id}")
                category = UNKNOWN_CATEGORY_MARKER # Treat invalid as unknown

            if recipe_id in results:
                print(f"   ⚠️ Gemini Warning: Duplicate recipe_id {recipe_id} in response. Using first occurrence.")
                continue # Avoid overwriting if duplicate IDs are sent back

            results[recipe_id] = category
            processed_ids.add(recipe_id)
            if category != UNKNOWN_CATEGORY_MARKER:
                valid_classifications += 1


        # Check if all expected IDs were processed
        missing_ids = expected_ids - processed_ids
        if missing_ids:
            print(f"   ⚠️ Gemini Warning: Missing classifications for recipe IDs: {missing_ids}")
            # Add missing IDs with UNKNOWN category
            for missing_id in missing_ids:
                results[missing_id] = UNKNOWN_CATEGORY_MARKER


        print(f"   📊 Parsed Gemini Response: {len(results)} results ({valid_classifications} valid categories).")
        return results

    except json.JSONDecodeError as e:
        print(f"   ❌ Gemini Error: Failed to decode JSON response. Error: {e}. Response: {response_text[:200]}...")
        return None
    except Exception as e:
        print(f"   ❌ Unexpected Error parsing Gemini response: {e}")
        return None
    
def get_batch_categories_from_gemini(recipe_batch):
    """Sends a batch of recipes to Gemini for classification."""
    if not recipe_batch:
        return None
    
    expected_ids = {r_id for r_id, name in recipe_batch}
    input_json_str = prepare_batch_input_json(recipe_batch)
    full_prompt = f"{BATCH_CLASSIFICATION_PROMPT}\n{input_json_str}"

    payload = {
        "contents": [{"parts": [{"text": full_prompt}]}],
        "safetySettings": [ # Keep safety settings reasonable
            {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
            {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
            {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"},
            {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_MEDIUM_AND_ABOVE"}
        ],
        "generationConfig": {
            "responseMimeType": "application/json", # Explicitly request JSON output if model supports it
            "temperature": 0.2, # Lower temperature for more deterministic classification
            "maxOutputTokens": 8192 # Allow larger response for batches (check model limits)
        }
    }

    for attempt in range(3):
        try:
            print(f"   📞 Calling Gemini for batch of {len(recipe_batch)} recipes (Attempt {attempt+1})...")
            response = requests.post(
                f"{API_ENDPOINT}?key={GEMINI_API_KEY}",
                headers=HEADERS,
                json=payload,
                timeout=REQUEST_TIMEOUT
            )
            response.raise_for_status()

            response_data = response.json()

            if not response_data.get('candidates'):
                print(f"   ❌ Gemini Error: No candidates found in response for batch. Response: {response_data}")
                if response_data.get('promptFeedback', {}).get('blockReason'):
                    print(f"      Block Reason: {response_data['promptFeedback']['blockReason']}")
                # Add retry logic here? For now, return None on block/no candidates.
                if attempt < 2:
                    print(f"   ⏳ Retrying after error...")
                    time.sleep( (2 ** attempt) + 1 )
                    continue
                else:
                    return None # Failed after retries


            try:
                response_text = response_data['candidates'][0]['content']['parts'][0]['text']
                parsed_results = parse_gemini_batch_response(response_text, expected_ids)
                if parsed_results is not None: # Check if parsing itself was successful
                    return parsed_results
                # If parsing failed, fall through to retry logic
                print(f"   ⚠️ Parsing failed for attempt {attempt+1}, retrying...")

            except (KeyError, IndexError) as e:
                print(f"   ❌ Gemini Error: Could not extract text from response structure. Error: {e}. Response: {response_data}")
                # Fall through to retry logic


        except requests.exceptions.RequestException as e:
            print(f"   ❌ Gemini API Request Error (Attempt {attempt+1}): {str(e)}")
        except Exception as e:
            print(f"   ❌ Unexpected Error during Gemini call (Attempt {attempt+1}): {str(e)}")

        # If loop continues (error occurred), wait before retrying
        if attempt < 2:
            print(f"   ⏳ Retrying after error...")
            time.sleep( (2 ** attempt) + 1 ) # Exponential backoff

    print(f"   ❌ Failed to get categories for batch after multiple retries.")
    return None # Failed after all retries

def main():
    conn = None
    total_updated_count = 0
    total_failed_count = 0
    total_processed = 0
    db_offset = 0
    if GEMINI_API_KEY == "YOUR_API_KEY_HERE" or not GEMINI_API_KEY:
        print("❌ ERROR: Please set your Gemini API Key in the script or environment variable 'GEMINI_API_KEY'.")
        return

    try:
        # Connect to MySQL
        conn = mysql.connector.connect(**db_config)
        # Using dictionary cursor makes accessing columns by name easier
        cursor = conn.cursor(dictionary=True)
        print("✅ Connected to database.")

        while True:
            # Fetch a batch of recipes from DB
            print(f"\n🔹 Fetching DB batch starting from offset {db_offset} (Size: {DB_FETCH_BATCH_SIZE})...")
            # Order by ID for consistent processing if script is restarted
            cursor.execute(
                "SELECT id, name FROM recipes WHERE category IS NULL ORDER BY id LIMIT %s OFFSET %s",
                (DB_FETCH_BATCH_SIZE, db_offset)
            )
            db_batch = cursor.fetchall() # List of dictionaries

            if not db_batch:
                print("\n✅ No more recipes found needing category updates.")
                break

            print(f"   Fetched {len(db_batch)} recipes from DB.")
            db_offset += len(db_batch) # Increment offset for the next DB fetch

            # Process the DB batch in smaller chunks suitable for Gemini API
            num_gemini_batches = math.ceil(len(db_batch) / GEMINI_PROMPT_BATCH_SIZE)
            print(f"   Splitting DB batch into {num_gemini_batches} Gemini prompt batches (Size: {GEMINI_PROMPT_BATCH_SIZE}).")

            for i in range(num_gemini_batches):
                start_index = i * GEMINI_PROMPT_BATCH_SIZE
                end_index = start_index + GEMINI_PROMPT_BATCH_SIZE
                gemini_batch_data = db_batch[start_index:end_index] # Slice the list of dicts
                # Convert list of dicts to list of tuples (id, name) for the API function
                gemini_batch_tuples = [(item['id'], item['name']) for item in gemini_batch_data]

                if not gemini_batch_tuples:
                    continue # Should not happen, but safeguard

                print(f"\n   ✨ Processing Gemini Batch {i+1}/{num_gemini_batches} ({len(gemini_batch_tuples)} recipes)...")
                total_processed += len(gemini_batch_tuples)

                # Get classifications from Gemini for this smaller batch
                batch_results = get_batch_categories_from_gemini(gemini_batch_tuples)

                updates_to_commit = []
                batch_update_count = 0
                batch_fail_count = 0

                if batch_results:
                    # Prepare updates for the database
                    for recipe_id, category in batch_results.items():
                        if category != UNKNOWN_CATEGORY_MARKER:
                            updates_to_commit.append((category, recipe_id))
                            batch_update_count += 1
                        else:
                            print(f"   ℹ️ Skipping update for R{recipe_id} (marked as {UNKNOWN_CATEGORY_MARKER}).")
                            batch_fail_count += 1
                else:
                    # API call failed for the entire batch
                    print(f"   ❌ API call failed for Gemini Batch {i+1}. Marking all {len(gemini_batch_tuples)} recipes as failed for this attempt.")
                    batch_fail_count += len(gemini_batch_tuples)


                # --- Update the database for the processed Gemini batch ---
                if updates_to_commit:
                    update_sql = "UPDATE recipes SET category = %s WHERE id = %s"
                    try:
                        # Use a standard cursor for executemany
                        update_cursor = conn.cursor()
                        update_cursor.executemany(update_sql, updates_to_commit)
                        conn.commit() # Commit after processing the Gemini batch
                        update_cursor.close()
                        total_updated_count += len(updates_to_commit)
                        print(f"   💾 Committed {len(updates_to_commit)} updates for Gemini Batch {i+1}.")
                    except mysql.connector.Error as err:
                        print(f"   ❌ Database Error during batch update: {err}")
                        conn.rollback() # Rollback batch on error
                        total_failed_count += len(updates_to_commit) # Count committed updates as failed if commit fails
                        batch_fail_count += batch_update_count # Adjust local count
                        batch_update_count = 0 # Reset local count
                    except Exception as e:
                        print(f"   ❌ Unexpected error during DB update commit: {e}")
                        conn.rollback()
                        total_failed_count += len(updates_to_commit)
                        batch_fail_count += batch_update_count
                        batch_update_count = 0
                else:
                    print(f"   ℹ️ No successful category updates to commit for Gemini Batch {i+1}.")

                total_failed_count += batch_fail_count
                print(f"   📊 Gemini Batch {i+1} Summary: {batch_update_count} succeeded, {batch_fail_count} failed/unknown.")

                # Rate limit API calls between Gemini batches
                if i < num_gemini_batches - 1: # Don't sleep after the last batch in the DB set
                    print(f"   ⏳ Sleeping for {RATE_LIMIT_DELAY} seconds before next Gemini batch...")
                    time.sleep(RATE_LIMIT_DELAY)


        print("\n🎉 Category Update Process Complete!")
        print(f"📊 Total Recipes Processed: {total_processed}")
        print(f"✅ Successfully Updated Categories: {total_updated_count}")
        print(f"❌ Failed / Unknown Categories: {total_failed_count}")

    except mysql.connector.Error as err:
        print(f"❌ Database Connection/Query Error: {err}")
    except Exception as e:
        print(f"❌ An unexpected critical error occurred: {str(e)}")
        if conn and conn.is_connected():
            conn.rollback() # Rollback nypending transaction on critical error
    finally:
        if conn and conn.is_connected():
            if 'cursor' in locals() and cursor:
                cursor.close()
            conn.close()
            print("🔒 Database connection closed.")

if __name__ == "__main__":
    main()

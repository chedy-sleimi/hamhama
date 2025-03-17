import json
import os
import shutil

# Load metadata
with open('recipe_data.json', 'r', encoding='utf-8') as f:
    recipes = json.load(f)

# Create destination directory if it doesn't exist
os.makedirs('../uploads/recipe_pictures', exist_ok=True)

# Process each recipe
for index, recipe in enumerate(recipes, start=1):
    image_base = recipe['Image_Name']
    source_path = os.path.join('images', f'{image_base}.jpg')
    dest_path = os.path.join('../uploads', 'recipe-pictures', f'{index}.jpg')
    
    if os.path.exists(source_path):
        shutil.move(source_path, dest_path)
        print(f"Moved {image_base}.jpg to uploads/recipe-pictures/{index}.jpg")
    else:
        print(f"Warning: Image not found for recipe {index} - {image_base}.jpg")
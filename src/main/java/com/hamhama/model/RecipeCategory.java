package com.hamhama.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
// If your deserializer just handles case-insensitivity or simple mapping,
// Spring Boot's default enum handling might be sufficient.
// Consider if RecipeCategoryDeserializer is still strictly needed.
// If it is, ensure it's updated to handle these new values.
// import com.hamhama.config.RecipeCategoryDeserializer; // Example path

/**
 * Represents the primary classification of a recipe, typically by meal type or course.
 * A recipe should belong to only one primary category.
 * Other attributes like dietary restrictions (vegetarian, vegan, gluten-free)
 * or nutritional profiles (high-protein, low-carb) should be handled
 * by separate fields (e.g., boolean flags or a list of tags) if needed.
 */
// Keep the deserializer if it provides necessary custom logic (e.g., case-insensitivity)
@JsonDeserialize(using = RecipeCategoryDeserializer.class) // Make sure this deserializer exists and handles the new values
public enum RecipeCategory {
    // Core Meal Types
    BREAKFAST,      // Recipes typically eaten for breakfast
    LUNCH,          // Recipes suitable for a main lunch meal
    DINNER,         // Recipes suitable for a main dinner meal
    APPETIZER,      // Starters, finger foods, small bites before a main course
    SALAD,          // Recipes where the primary component is salad greens/vegetables
    SOUP,           // Liquid-based dishes, served hot or cold
    SIDE_DISH,      // Dishes meant to accompany a main course (e.g., vegetables, grains)
    DESSERT,        // Sweet dishes typically served after a main course
    SNACK,          // Recipes for eating between meals
    BEVERAGE,       // Drinks, smoothies, juices, etc.
    CONDIMENT_SAUCE // Sauces, dressings, dips, spreads meant to accompany other food

    // --- Removed Values ---
    // VEGETARIAN, NON_VEGETARIAN -> Handle via a boolean isVegetarian field or tags
    // HIGH_PROTEIN, LOW_CARB -> Handle via tags or calculated nutritional info
}
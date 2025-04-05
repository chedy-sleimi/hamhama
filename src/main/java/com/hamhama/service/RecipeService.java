package com.hamhama.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamhama.dto.IngredientDTO;
import com.hamhama.dto.NutritionRequestDTO;
import com.hamhama.dto.RecipeDTO;
import com.hamhama.dto.RecipeResponseDTO;
import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import com.hamhama.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final GeminiService geminiService; // Assuming GeminiService exists

    public RecipeService(RecipeRepository recipeRepository, GeminiService geminiService) {
        this.recipeRepository = recipeRepository;
        this.geminiService = geminiService;
    }

    /**
     * Adds a new recipe. Ensures category is provided.
     * Assumes RecipeDTO contains a getCategory() method.
     */
    public Recipe addRecipe(RecipeDTO recipeDTO) {
        if (recipeDTO.getCategory() == null) {
            throw new IllegalArgumentException("Recipe category is required");
        }

        Recipe recipe = new Recipe();
        recipe.setName(recipeDTO.getName());
        recipe.setDescription(recipeDTO.getDescription());
        recipe.setCategory(recipeDTO.getCategory());
        // Note: Handling user association and ingredients would typically happen here too.
        // Example: recipe.setUser(userService.getCurrentUser());
        // Example: map and set recipeDTO.getIngredients() to recipe.setRecipeIngredients(...)

        return recipeRepository.save(recipe);
    }

    /**
     * Updates an existing recipe. Ensures category is updated.
     */
    public Recipe updateRecipe(Long id, Recipe recipeDetails) {
        // It's generally better practice to take a DTO here too,
        // but we'll stick to the provided signature.
        return recipeRepository.findById(id).map(existingRecipe -> {
            existingRecipe.setName(recipeDetails.getName());
            existingRecipe.setDescription(recipeDetails.getDescription());
            // Be careful when updating collections like this. Usually requires merging logic.
            // existingRecipe.setRecipeIngredients(recipeDetails.getRecipeIngredients());
            // You might need specific logic to add/remove/update RecipeIngredient entities.
            existingRecipe.setUser(recipeDetails.getUser()); // Ensure user details are handled correctly
            existingRecipe.setCategory(recipeDetails.getCategory()); // Update category
            return recipeRepository.save(existingRecipe);
        }).orElse(null); // Or throw RecipeNotFoundException
    }

    /**
     * Deletes a recipe by ID.
     */
    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            // Consider throwing a specific RecipeNotFoundException
            throw new IllegalArgumentException("Recipe with ID " + id + " not found.");
        }
        recipeRepository.deleteById(id);
    }

    /**
     * Searches recipes by optional criteria: name, description, ingredient, category.
     * If multiple non-category criteria are provided, only the first one encountered is used in this implementation.
     * If category is provided, it filters by category first, potentially combined with one other criterion.
     */
    public List<Recipe> searchRecipes(String name, String description, String ingredient, RecipeCategory category) {
        boolean hasName = StringUtils.hasText(name);
        boolean hasDescription = StringUtils.hasText(description);
        boolean hasIngredient = StringUtils.hasText(ingredient);

        if (category != null) {
            // Category is provided, combine with other criteria if present
            if (hasName) {
                return recipeRepository.findByCategoryAndNameContainingIgnoreCase(category, name);
            }
            if (hasDescription) {
                return recipeRepository.findByCategoryAndDescriptionContainingIgnoreCase(category, description);
            }
            if (hasIngredient) {
                return recipeRepository.findByCategoryAndIngredientsNameContainingIgnoreCase(category, ingredient);
            }
            // Only category is provided
            return recipeRepository.findByCategory(category);
        } else {
            // No category provided, use original logic
            if (hasName) {
                return recipeRepository.findByNameContainingIgnoreCase(name);
            }
            if (hasDescription) {
                return recipeRepository.findByDescriptionContainingIgnoreCase(description);
            }
            if (hasIngredient) {
                return recipeRepository.findByIngredientsNameContainingIgnoreCase(ingredient);
            }
        }

        // No filters provided, return all
        // Consider adding pagination here for performance if the dataset can grow large.
        return recipeRepository.findAll();
    }

    /**
     * Gets recipes belonging to a specific category.
     */
    public List<Recipe> getRecipesByCategory(RecipeCategory category) {
        return recipeRepository.findByCategory(category);
    }

    /**
     * Gets recipes belonging to any of the specified categories.
     */
    public List<Recipe> getRecipesByCategories(List<RecipeCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of(); // Return empty list if input is empty/null
        }
        return recipeRepository.findByCategoryIn(categories);
    }

    /**
     * Generates nutritional facts for a recipe using GeminiService.
     */
    public String generateNutritionalFacts(Long recipeId) throws Exception {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with ID: " + recipeId));

        NutritionRequestDTO requestDTO = new NutritionRequestDTO();
        requestDTO.setRecipeName(recipe.getName());

        List<IngredientDTO> ingredients = recipe.getRecipeIngredients().stream()
                .map(ri -> {
                    IngredientDTO dto = new IngredientDTO();
                    dto.setName(ri.getIngredient().getName());
                    // Handle potential nulls gracefully
                    dto.setQuantity(ri.getQuantity() != null ? ri.getQuantity() : 0.0);
                    dto.setUnit(ri.getUnit() != null ? ri.getUnit() : "");
                    return dto;
                })
                .collect(Collectors.toList());

        requestDTO.setIngredients(ingredients);

        ObjectMapper mapper = new ObjectMapper();
        String nutritionData = mapper.writeValueAsString(requestDTO);

        // Assuming GeminiService has this method
        return geminiService.generateNutritionLabel(nutritionData);
    }

    /**
     * Converts a Recipe entity to a RecipeResponseDTO.
     */
    private RecipeResponseDTO convertToResponseDTO(Recipe recipe) {
        RecipeResponseDTO dto = new RecipeResponseDTO();
        dto.setId(recipe.getId());
        dto.setName(recipe.getName());
        dto.setDescription(recipe.getDescription());
        dto.setCategory(recipe.getCategory()); // Category is included
        dto.setAverageRating(recipe.getAverageRating());
        // Assuming a standard pattern for image URLs
        // This might need adjustment based on your static resource handling/storage solution
        dto.setImageUrl("/recipe-pictures/" + recipe.getId() + ".jpg");
        // Add other fields if needed (e.g., author username)
        // if (recipe.getUser() != null) {
        //    dto.setAuthorUsername(recipe.getUser().getUsername());
        // }
        return dto;
    }

    /**
     * Gets all recipes, converted to DTOs.
     */
    public List<RecipeResponseDTO> getAllRecipes() {
        // Add pagination here for real-world applications (e.g., using Pageable)
        return recipeRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single recipe by ID, converted to a DTO.
     */
    public Optional<RecipeResponseDTO> getRecipeById(Long id) {
        return recipeRepository.findById(id).map(this::convertToResponseDTO);
    }

    // --- Getters for dependencies (useful for testing) ---
    public RecipeRepository getRecipeRepository() {
        return recipeRepository;
    }

    public GeminiService getGeminiService() {
        return geminiService;
    }
}
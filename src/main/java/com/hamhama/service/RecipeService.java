package com.hamhama.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamhama.dto.IngredientDTO;
import com.hamhama.dto.NutritionRequestDTO;
import com.hamhama.dto.RecipeDTO;
import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import com.hamhama.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RecipeService {
    private final RecipeRepository recipeRepository;

    private final GeminiService geminiService;

    public RecipeService(RecipeRepository recipeRepository, GeminiService geminiService) {
        this.recipeRepository = recipeRepository;
        this.geminiService = geminiService;
    }

    //  Get all recipes
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAll();
    }

    //  Get a recipe by ID
    public Optional<Recipe> getRecipeById(Long id) {
        return recipeRepository.findById(id);
    }

    //  Add recipe (must have a category)
    public Recipe addRecipe(RecipeDTO recipeDTO) {
        if (recipeDTO.getCategory() == null) {
            throw new IllegalArgumentException("Recipe category is required");
        }

        Recipe recipe = new Recipe();
        recipe.setName(recipeDTO.getName());
        recipe.setDescription(recipeDTO.getDescription());
        recipe.setCategory(recipeDTO.getCategory());

        return recipeRepository.save(recipe);
    }

    //  Update recipe and ensure category is updated
    public Recipe updateRecipe(Long id, Recipe recipeDetails) {
        Optional<Recipe> existingRecipe = recipeRepository.findById(id);
        if (existingRecipe.isPresent()) {
            Recipe updatedRecipe = existingRecipe.get();
            updatedRecipe.setName(recipeDetails.getName());
            updatedRecipe.setDescription(recipeDetails.getDescription());
            updatedRecipe.setRecipeIngredients(recipeDetails.getRecipeIngredients());
            updatedRecipe.setUser(recipeDetails.getUser());
            updatedRecipe.setCategory(recipeDetails.getCategory()); // Ensure category is updated
            return recipeRepository.save(updatedRecipe);
        }
        return null; // Or throw an exception if not found
    }

    //  Delete a recipe
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
    }

    // Search recipes
    public List<Recipe> searchRecipes(String name, String description, String ingredient) {
        if (name != null && !name.isEmpty()) {
            return recipeRepository.findByNameContainingIgnoreCase(name);
        }
        if (description != null && !description.isEmpty()) {
            return recipeRepository.findByDescriptionContainingIgnoreCase(description);
        }
        if (ingredient != null && !ingredient.isEmpty()) {
            return recipeRepository.findByIngredientsNameContainingIgnoreCase(ingredient);
        }
        return recipeRepository.findAll(); // If no filters, return all recipes
    }

    //  Get recipes by category
    public List<Recipe> getRecipesByCategory(RecipeCategory category) {
        return recipeRepository.findByCategory(category);
    }

    //  Get recipes by multiple categories
    public List<Recipe> getRecipesByCategories(List<RecipeCategory> categories) {
        return recipeRepository.findByCategoryIn(categories);  // Fixed repository method name
    }

    public String generateNutritionalFacts(Long recipeId) throws Exception {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found"));

        NutritionRequestDTO requestDTO = new NutritionRequestDTO();
        requestDTO.setRecipeName(recipe.getName());

        List<IngredientDTO> ingredients = recipe.getRecipeIngredients().stream()
                .map(ri -> {
                    IngredientDTO dto = new IngredientDTO();
                    dto.setName(ri.getIngredient().getName());
                    dto.setQuantity(ri.getQuantity() != null ? ri.getQuantity() : 0.0);
                    dto.setUnit(ri.getUnit() != null ? ri.getUnit() : "");
                    return dto;
                })
                .collect(Collectors.toList());

        requestDTO.setIngredients(ingredients);

        ObjectMapper mapper = new ObjectMapper();
        String nutritionData = mapper.writeValueAsString(requestDTO);

        return geminiService.generateNutritionLabel(nutritionData);
    }
}

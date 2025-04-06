package com.hamhama.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamhama.dto.IngredientDTO;
import com.hamhama.dto.NutritionRequestDTO;
import com.hamhama.dto.RecipeDTO;
import com.hamhama.dto.RecipeResponseDTO;
import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import com.hamhama.model.RecipeIngredient; // Assuming this exists
import com.hamhama.model.User;
import com.hamhama.repository.RecipeRepository;
// Assuming IngredientRepository and RecipeIngredientRepository exist if managing ingredients here
// import com.hamhama.repository.IngredientRepository;
// import com.hamhama.repository.RecipeIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize; // Import
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeService {
    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;
    private final GeminiService geminiService; // Assuming GeminiService exists
    // Inject IngredientRepository etc. if needed for ingredient mapping
    // private final IngredientRepository ingredientRepository;

    /**
     * Adds a new recipe, associating it with the currently authenticated user.
     *
     * @param recipeDTO DTO containing recipe details.
     * @return The saved Recipe entity.
     */
    public Recipe addRecipe(RecipeDTO recipeDTO) {
        if (recipeDTO.getCategory() == null) {
            throw new IllegalArgumentException("Recipe category is required");
        }

        User currentUser = getCurrentUser();

        Recipe recipe = new Recipe();
        recipe.setName(recipeDTO.getName());
        recipe.setDescription(recipeDTO.getDescription());
        recipe.setCategory(recipeDTO.getCategory());
        recipe.setUser(currentUser); // Assign the authenticated user

        // TODO: Handle ingredients mapping from DTO if present
        // mapAndSetIngredients(recipe, recipeDTO.getIngredients());

        Recipe savedRecipe = recipeRepository.save(recipe);
        log.info("User '{}' added recipe '{}' (ID: {})", currentUser.getUsername(), savedRecipe.getName(), savedRecipe.getId());
        return savedRecipe;
    }

    /**
     * Updates an existing recipe. Only the owner or an ADMIN can update.
     *
     * @param id            ID of the recipe to update.
     * @param recipeDetails DTO or Entity containing updated details.
     * @return The updated Recipe entity.
     */
    @PreAuthorize("hasRole('ADMIN') or @recipeRepository.findById(#id).orElse(null)?.user?.username == principal.username")
    public Recipe updateRecipe(Long id, Recipe recipeDetails) { // Consider using RecipeDTO here
        // @PreAuthorize handles the ownership/admin check

        Recipe existingRecipe = recipeRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Recipe ID {} not found for update", id);
                    return new RuntimeException("Recipe not found");
                });

        // Update fields from recipeDetails (use DTO preferably)
        existingRecipe.setName(recipeDetails.getName());
        existingRecipe.setDescription(recipeDetails.getDescription());
        existingRecipe.setCategory(recipeDetails.getCategory());
        // TODO: Handle ingredient updates carefully (merge logic often needed)
        // mapAndSetIngredients(existingRecipe, recipeDetails.getIngredients());

        Recipe updatedRecipe = recipeRepository.save(existingRecipe);
        log.info("Recipe ID {} updated successfully by user '{}' or ADMIN.", id, getCurrentUsername());
        return updatedRecipe;
    }

    /**
     * Deletes a recipe by ID. Only the owner or an ADMIN can delete.
     *
     * @param id ID of the recipe to delete.
     */
    @PreAuthorize("hasRole('ADMIN') or @recipeRepository.findById(#id).orElse(null)?.user?.username == principal.username")
    public void deleteRecipe(Long id) {
        // @PreAuthorize handles the ownership/admin check
        if (!recipeRepository.existsById(id)) {
            log.warn("Recipe ID {} not found for deletion", id);
            throw new RuntimeException("Recipe not found with ID: " + id);
        }
        recipeRepository.deleteById(id);
        log.info("Recipe ID {} deleted successfully by user '{}' or ADMIN.", id, getCurrentUsername());
    }

    // --- Read Operations (Mostly unchanged, assuming public visibility or handled by controller access) ---

    @Transactional(readOnly = true)
    public List<Recipe> searchRecipes(String name, String description, String ingredient, RecipeCategory category) {
        // This logic might need refinement based on exact search requirements & indexing
        boolean hasName = StringUtils.hasText(name);
        boolean hasDescription = StringUtils.hasText(description);
        boolean hasIngredient = StringUtils.hasText(ingredient);

        if (category != null) {
            if (hasName) return recipeRepository.findByCategoryAndNameContainingIgnoreCase(category, name);
            if (hasDescription) return recipeRepository.findByCategoryAndDescriptionContainingIgnoreCase(category, description);
            if (hasIngredient) return recipeRepository.findByCategoryAndIngredientsNameContainingIgnoreCase(category, ingredient);
            return recipeRepository.findByCategory(category);
        } else {
            if (hasName) return recipeRepository.findByNameContainingIgnoreCase(name);
            if (hasDescription) return recipeRepository.findByDescriptionContainingIgnoreCase(description);
            if (hasIngredient) return recipeRepository.findByIngredientsNameContainingIgnoreCase(ingredient);
        }
        log.debug("No specific search criteria provided, returning all recipes.");
        return recipeRepository.findAll(); // Consider Pagination
    }

    @Transactional(readOnly = true)
    public List<Recipe> getRecipesByCategory(RecipeCategory category) {
        return recipeRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Recipe> getRecipesByCategories(List<RecipeCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return recipeRepository.findByCategoryIn(categories);
    }

    @Transactional(readOnly = true)
    public String generateNutritionalFacts(Long recipeId) throws Exception {
        // Ensure recipe exists before proceeding
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with ID: " + recipeId));

        // Map to DTO for Gemini Service
        NutritionRequestDTO requestDTO = new NutritionRequestDTO();
        requestDTO.setRecipeName(recipe.getName());

        // Assuming Recipe entity has a collection like List<RecipeIngredient> recipeIngredients
        if (recipe.getRecipeIngredients() != null) {
            List<IngredientDTO> ingredients = recipe.getRecipeIngredients().stream()
                    .map(ri -> {
                        IngredientDTO dto = new IngredientDTO();
                        if(ri.getIngredient() != null) dto.setName(ri.getIngredient().getName());
                        dto.setQuantity(ri.getQuantity() != null ? ri.getQuantity() : 0.0);
                        dto.setUnit(ri.getUnit() != null ? ri.getUnit() : "");
                        return dto;
                    })
                    .collect(Collectors.toList());
            requestDTO.setIngredients(ingredients);
        } else {
            requestDTO.setIngredients(List.of()); // Empty list if no ingredients mapped
        }

        ObjectMapper mapper = new ObjectMapper();
        String nutritionData = mapper.writeValueAsString(requestDTO);
        log.debug("Generated NutritionRequestDTO JSON for recipe ID {}: {}", recipeId, nutritionData);

        return geminiService.generateNutritionLabel(nutritionData);
    }


    @Transactional(readOnly = true)
    public List<RecipeResponseDTO> getAllRecipes() {
        log.debug("Fetching all recipes and converting to DTOs.");
        // Add pagination in real app: recipeRepository.findAll(pageable).stream()...
        return recipeRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public Optional<RecipeResponseDTO> getRecipeById(Long id) {
        log.debug("Fetching recipe by ID: {}", id);
        return recipeRepository.findById(id).map(this::convertToResponseDTO);
    }

    // --- DTO Conversion ---
    private RecipeResponseDTO convertToResponseDTO(Recipe recipe) {
        RecipeResponseDTO dto = new RecipeResponseDTO();
        dto.setId(recipe.getId());
        dto.setName(recipe.getName());
        dto.setDescription(recipe.getDescription());
        dto.setCategory(recipe.getCategory());
        dto.setAverageRating(recipe.getAverageRating());
        // Standardize Image URL generation - adjust if needed
        dto.setImageUrl("/recipe-pictures/" + recipe.getId() + ".jpg"); // Example path
        if (recipe.getUser() != null) {
            dto.setAuthorUsername(recipe.getUser().getUsername());
        }
        // TODO: Include ingredient list in DTO if needed
        // dto.setIngredients(... map recipe.getRecipeIngredients() ...);
        return dto;
    }

    // --- Helper Methods ---
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        return (User) authentication.getPrincipal();
    }
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    // TODO: Implement ingredient mapping logic if RecipeDTO contains ingredients
    // private void mapAndSetIngredients(Recipe recipe, List<IngredientInRecipeDTO> ingredientDTOs) { ... }
}
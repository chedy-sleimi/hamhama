package com.hamhama.controller;

import com.hamhama.dto.RecipeDTO;
import com.hamhama.dto.RecipeResponseDTO;
import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import com.hamhama.service.RecipeService;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // Keep if you use validation annotations on DTO
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class); // Add logger
    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * POST /api/recipes : Add a new recipe.
     * Expects category in the request body (within RecipeDTO).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRecipe(@RequestBody @Validated RecipeDTO recipeDTO) { // Add @Validated if DTO has constraints
        try {
            log.info("Request received to add recipe: {}", recipeDTO.getName());
            Recipe savedRecipe = recipeService.addRecipe(recipeDTO);
            // Return 201 Created status with the created recipe (or DTO)
            // Potentially return RecipeResponseDTO instead of Recipe entity
            RecipeResponseDTO responseDTO = recipeService.getRecipeById(savedRecipe.getId()).orElse(null);
            if (responseDTO == null) {
                // Should not happen if save was successful, but good practice
                log.error("Failed to retrieve saved recipe with ID: {}", savedRecipe.getId());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve saved recipe");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add recipe: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding recipe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    /**
     * PUT /api/recipes/{id} : Update an existing recipe.
     * Expects category in the request body (within Recipe).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long id, @RequestBody @Validated Recipe recipeDetails) { // Consider using a RecipeUpdateDTO
        try {
            log.info("Request received to update recipe ID: {}", id);
            // Ensure the ID in the path matches the ID in the body if present
            if (recipeDetails.getId() != null && !recipeDetails.getId().equals(id)) {
                return ResponseEntity.badRequest().body("ID mismatch between path variable and request body.");
            }
            // Setting the ID from the path variable is safer
            recipeDetails.setId(id);

            Recipe updatedRecipe = recipeService.updateRecipe(id, recipeDetails);
            if (updatedRecipe != null) {
                RecipeResponseDTO responseDTO = recipeService.getRecipeById(updatedRecipe.getId()).orElse(null);
                if (responseDTO == null) {
                    log.error("Failed to retrieve updated recipe with ID: {}", updatedRecipe.getId());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve updated recipe");
                }
                log.info("Recipe ID {} updated successfully", id);
                return ResponseEntity.ok(responseDTO);
            } else {
                log.warn("Recipe ID {} not found for update", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error updating recipe ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during update.");
        }
    }

    /**
     * DELETE /api/recipes/{id} : Delete a recipe.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        try {
            log.info("Request received to delete recipe ID: {}", id);
            recipeService.deleteRecipe(id);
            log.info("Recipe ID {} deleted successfully", id);
            return ResponseEntity.noContent().build(); // 204 No Content is standard for successful delete
        } catch (IllegalArgumentException e) { // Catch specific "not found" exception if RecipeService throws it
            log.warn("Recipe ID {} not found for deletion", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found with ID: " + id, e);
        } catch (Exception e) {
            log.error("Error deleting recipe ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during deletion.", e);
        }
    }

    /**
     * GET /api/recipes/search : Search recipes by name, description, ingredient, or category.
     */
    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponseDTO>> searchRecipes( // Return DTOs
                                                                  @RequestParam(required = false) String name,
                                                                  @RequestParam(required = false) String description,
                                                                  @RequestParam(required = false) String ingredient,
                                                                  @RequestParam(required = false) RecipeCategory category) { // Add category parameter
        log.info("Searching recipes with criteria - Name: '{}', Description: '{}', Ingredient: '{}', Category: '{}'",
                name, description, ingredient, category);
        List<Recipe> results = recipeService.searchRecipes(name, description, ingredient, category);
        // Convert results to DTOs before sending response
        List<RecipeResponseDTO> responseDTOs = results.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null)) // Reuse conversion logic
                .filter(java.util.Objects::nonNull) // Filter out any nulls if conversion fails (shouldn't happen)
                .collect(Collectors.toList());
        log.debug("Found {} recipes matching search criteria.", responseDTOs.size());
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * GET /api/recipes/category/{category} : Get recipes by a specific category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<RecipeResponseDTO>> getRecipesByCategory(@PathVariable RecipeCategory category) { // Return DTOs
        log.info("Request received to get recipes by category: {}", category);
        List<Recipe> recipes = recipeService.getRecipesByCategory(category);
        List<RecipeResponseDTO> responseDTOs = recipes.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Found {} recipes for category {}.", responseDTOs.size(), category);
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * POST /api/recipes/categories : Get recipes by multiple categories.
     * Using POST here because GET might have limitations with list parameters in URLs.
     */
    @PostMapping("/categories")
    public ResponseEntity<List<RecipeResponseDTO>> getRecipesByCategories(@RequestBody List<RecipeCategory> categories) { // Return DTOs
        log.info("Request received to get recipes by categories: {}", categories);
        if (categories == null || categories.isEmpty()) {
            return ResponseEntity.badRequest().body(List.of()); // Or return empty list with OK status? Depends on API contract.
        }
        List<Recipe> recipes = recipeService.getRecipesByCategories(categories);
        List<RecipeResponseDTO> responseDTOs = recipes.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Found {} recipes for categories {}.", responseDTOs.size(), categories);
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * GET /api/recipes/{id}/nutrition : Get nutritional facts SVG for a recipe.
     */
    @GetMapping(value = "/{id}/nutrition", produces = "image/svg+xml")
    public ResponseEntity<String> getNutritionFacts(@PathVariable Long id) {
        log.info("Request received for nutrition facts for recipe ID: {}", id);
        try {
            String svgData = recipeService.generateNutritionalFacts(id);
            log.debug("Successfully generated nutrition facts SVG for recipe ID: {}", id);
            return ResponseEntity.ok(svgData);
        } catch (IllegalArgumentException e) {
            log.warn("Recipe not found (ID: {}) for nutrition facts generation: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating nutrition facts for recipe ID: {}", id, e);
            // Return a more informative SVG error or a plain text error response
            String errorSvg = String.format(
                    "<svg viewBox=\"0 0 200 50\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                            "  <text x=\"10\" y=\"20\" font-family=\"Arial\" font-size=\"12\" fill=\"red\">\n" +
                            "    Error generating nutrition facts.\n" +
                            "  </text>\n" +
                            "  <text x=\"10\" y=\"40\" font-family=\"Arial\" font-size=\"10\" fill=\"red\">\n" +
                            "    %s\n" + // Placeholder for the message
                            "  </text>\n" +
                            "</svg>",
                    e.getMessage() != null ? e.getMessage().replace("<", "<").replace(">", ">") : "Unknown error" // Basic escaping
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.valueOf("image/svg+xml")) // Ensure correct content type for SVG error
                    .body(errorSvg);
        }
    }

    /**
     * GET /api/recipes : Get all recipes.
     */
    @GetMapping
    public ResponseEntity<List<RecipeResponseDTO>> getAllRecipes() {
        // Consider adding Pageable parameter here for pagination
        log.info("Request received to get all recipes");
        List<RecipeResponseDTO> recipes = recipeService.getAllRecipes();
        log.debug("Returning {} recipes.", recipes.size());
        return ResponseEntity.ok(recipes);
    }

    /**
     * GET /api/recipes/{id} : Get a specific recipe by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponseDTO> getRecipeById(@PathVariable Long id) {
        log.info("Request received to get recipe by ID: {}", id);
        return recipeService.getRecipeById(id)
                .map(recipe -> {
                    log.debug("Recipe found for ID: {}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    log.warn("Recipe not found for ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
package com.hamhama.controller;

import com.hamhama.dto.RecipeDTO; // Request DTO for creating recipes
import com.hamhama.dto.RecipeResponseDTO; // Response DTO for sending recipe details
import com.hamhama.model.Recipe; // Assuming Recipe entity exists
import com.hamhama.model.RecipeCategory; // Enum for categories
import com.hamhama.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
// No alias import needed, use fully qualified name directly
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // For protected endpoints
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects; // Import Objects for filter
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipe Management", description = "Endpoints for creating, reading, updating, deleting, and searching recipes.")
// Note: Security requirements are added per-method for this controller, assuming some are public and some require auth.
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);
    private final RecipeService recipeService;

    // Constructor injection is handled by @RestController implicitly if only one constructor,
    // or you can add @Autowired or keep the constructor explicit.
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @Operation(summary = "Add a new recipe", description = "Creates a new recipe based on the provided data. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recipe created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecipeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid recipe data provided (e.g., missing fields, invalid category)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token is missing or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Failed to save or retrieve the recipe")
    })
    @SecurityRequirement(name = "bearerAuth") // This endpoint requires authentication
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRecipe(
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "DTO containing the details of the recipe to be created", required = true,
                    content = @Content(schema = @Schema(implementation = RecipeDTO.class)))
            // Spring's RequestBody annotation remains on the parameter
            @RequestBody @Validated RecipeDTO recipeDTO) {
        try {
            log.info("Request received to add recipe: {}", recipeDTO.getName());
            Recipe savedRecipe = recipeService.addRecipe(recipeDTO);
            RecipeResponseDTO responseDTO = recipeService.getRecipeById(savedRecipe.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve saved recipe")); // Throw if retrieval fails
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add recipe: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage()); // Return specific error message
        } catch (ResponseStatusException rse) {
            log.error("Error retrieving saved recipe: {}", rse.getMessage());
            return ResponseEntity.status(rse.getStatusCode()).body(rse.getReason());
        } catch (Exception e) {
            log.error("Unexpected error adding recipe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @Operation(summary = "Update an existing recipe", description = "Updates the details of a recipe specified by its ID. Requires authentication (and likely ownership/admin rights, handled by service).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecipeResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid data or ID mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not allowed to update this recipe"), // If service checks ownership
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecipe(
            @Parameter(description = "ID of the recipe to update", required = true) @PathVariable Long id,
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Recipe object with updated details. **Using a specific RecipeUpdateDTO is highly recommended.**", required = true,
                    content = @Content(schema = @Schema(implementation = Recipe.class))) // Ideally RecipeUpdateDTO.class
            // Spring's RequestBody annotation remains on the parameter
            @RequestBody @Validated Recipe recipeDetails) {
        try {
            log.info("Request received to update recipe ID: {}", id);
            if (recipeDetails.getId() != null && !recipeDetails.getId().equals(id)) {
                log.warn("Update failed for recipe ID {}: Mismatch between path ID and body ID ({}).", id, recipeDetails.getId());
                return ResponseEntity.badRequest().body("ID mismatch between path variable and request body.");
            }
            recipeDetails.setId(id); // Ensure ID from path is used

            Recipe updatedRecipe = recipeService.updateRecipe(id, recipeDetails);
            // updateRecipe should throw exception if not found, so null check might be redundant
            RecipeResponseDTO responseDTO = recipeService.getRecipeById(updatedRecipe.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve updated recipe"));
            log.info("Recipe ID {} updated successfully", id);
            return ResponseEntity.ok(responseDTO);
        } catch (ResponseStatusException rse) { // Catch specific exceptions from service
            log.warn("Failed to update recipe ID {}: {}", id, rse.getReason());
            return ResponseEntity.status(rse.getStatusCode()).body(rse.getReason());
        } catch (Exception e) { // Catch unexpected errors
            log.error("Error updating recipe ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during update.");
        }
    }


    @Operation(summary = "Delete a recipe", description = "Deletes a recipe specified by its ID. Requires authentication (and likely ownership/admin rights, handled by service).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recipe deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User not allowed to delete this recipe"), // If service checks ownership
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @Parameter(description = "ID of the recipe to delete", required = true) @PathVariable Long id) {
        try {
            log.info("Request received to delete recipe ID: {}", id);
            recipeService.deleteRecipe(id); // Assuming this throws if not found or not authorized
            log.info("Recipe ID {} deleted successfully", id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException rse) { // Catch exceptions from service
            log.warn("Failed to delete recipe ID {}: {}", id, rse.getReason());
            throw rse; // Re-throw to let Spring handle standard error responses
        } catch (Exception e) { // Catch unexpected errors
            log.error("Error deleting recipe ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during deletion.", e);
        }
    }

    @Operation(summary = "Search recipes", description = "Searches for recipes based on optional criteria: name, description, ingredient name, and/or category. All criteria are combined with AND logic if multiple are provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search successful, returning matching recipes (list may be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))) // Schema for List<RecipeResponseDTO>
    })
    // This endpoint is likely public, so no @SecurityRequirement
    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponseDTO>> searchRecipes(
            @Parameter(description = "Part of the recipe name to search for (case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "Part of the recipe description to search for (case-insensitive)") @RequestParam(required = false) String description,
            @Parameter(description = "Name of an ingredient to search for within recipes (case-insensitive)") @RequestParam(required = false) String ingredient,
            @Parameter(description = "Category to filter recipes by") @RequestParam(required = false) RecipeCategory category) {
        log.info("Searching recipes with criteria - Name: '{}', Description: '{}', Ingredient: '{}', Category: '{}'",
                name, description, ingredient, category);
        List<Recipe> results = recipeService.searchRecipes(name, description, ingredient, category);
        List<RecipeResponseDTO> responseDTOs = results.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Found {} recipes matching search criteria.", responseDTOs.size());
        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(summary = "Get recipes by category", description = "Retrieves all recipes belonging to a specific category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recipes for the category (list may be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))) // Schema for List<RecipeResponseDTO>
    })
    // Public endpoint
    @GetMapping("/category/{category}")
    public ResponseEntity<List<RecipeResponseDTO>> getRecipesByCategory(
            @Parameter(description = "The category to retrieve recipes for", required = true) @PathVariable RecipeCategory category) {
        log.info("Request received to get recipes by category: {}", category);
        List<Recipe> recipes = recipeService.getRecipesByCategory(category);
        List<RecipeResponseDTO> responseDTOs = recipes.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Found {} recipes for category {}.", responseDTOs.size(), category);
        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(summary = "Get recipes by multiple categories", description = "Retrieves recipes belonging to any of the specified categories. Uses POST to allow sending a list of categories in the request body.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recipes for the categories (list may be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // Schema for List<RecipeResponseDTO>
            @ApiResponse(responseCode = "400", description = "Bad Request - Category list is empty or invalid")
    })
    // Public endpoint, changed to POST for standard list handling in body
    @GetMapping("/categories")
    public ResponseEntity<List<RecipeResponseDTO>> getRecipesByCategories(
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON list of recipe categories to filter by", required = true,
                    content = @Content(schema = @Schema(implementation = List.class))) // List<RecipeCategory>
            // Spring's RequestBody annotation remains on the parameter
            @RequestBody List<RecipeCategory> categories) {
        log.info("Request received to get recipes by categories: {}", categories);
        if (categories == null || categories.isEmpty()) {
            log.warn("Received empty category list for /categories endpoint.");
            return ResponseEntity.badRequest().body(List.of()); // Return empty list with 400 Bad Request
        }
        List<Recipe> recipes = recipeService.getRecipesByCategories(categories);
        List<RecipeResponseDTO> responseDTOs = recipes.stream()
                .map(recipe -> recipeService.getRecipeById(recipe.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("Found {} recipes for categories {}.", responseDTOs.size(), categories);
        return ResponseEntity.ok(responseDTOs);
    }

    @Operation(summary = "Get nutritional facts SVG", description = "Generates and returns an SVG image representing the nutritional facts for a specific recipe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved nutritional facts SVG",
                    content = @Content(mediaType = "image/svg+xml")), // Specific content type
            @ApiResponse(responseCode = "404", description = "Recipe not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error generating SVG",
                    content = @Content(mediaType = "image/svg+xml")) // Error might also be SVG
    })
    // Public endpoint
    @GetMapping(value = "/{id}/nutrition", produces = "image/svg+xml")
    public ResponseEntity<String> getNutritionFacts(
            @Parameter(description = "ID of the recipe for which to generate nutrition facts", required = true) @PathVariable Long id) {
        log.info("Request received for nutrition facts for recipe ID: {}", id);
        try {
            String svgData = recipeService.generateNutritionalFacts(id);
            log.debug("Successfully generated nutrition facts SVG for recipe ID: {}", id);
            return ResponseEntity.ok(svgData);
        } catch (IllegalArgumentException e) { // Assuming service throws this for not found
            log.warn("Recipe not found (ID: {}) for nutrition facts generation: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error generating nutrition facts for recipe ID: {}", id, e);
            String errorSvg = String.format( // Simple error SVG
                    "<svg viewBox=\"0 0 200 50\" xmlns=\"http://www.w3.org/2000/svg\"><text x=\"10\" y=\"20\" font-family=\"Arial\" font-size=\"12\" fill=\"red\">Error generating nutrition facts.</text><text x=\"10\" y=\"40\" font-family=\"Arial\" font-size=\"10\" fill=\"red\">%s</text></svg>",
                    e.getMessage() != null ? e.getMessage().replace("<", "<").replace(">", ">") : "Unknown error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .body(errorSvg);
        }
    }


    @Operation(summary = "Get all recipes", description = "Retrieves a list of all recipes. Consider adding pagination parameters (page, size) in the future.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all recipes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))) // Schema for List<RecipeResponseDTO>
    })
    // Public endpoint
    @GetMapping
    public ResponseEntity<List<RecipeResponseDTO>> getAllRecipes() {
        // TODO: Implement pagination using Pageable parameter for scalability
        log.info("Request received to get all recipes");
        List<RecipeResponseDTO> recipes = recipeService.getAllRecipes();
        log.debug("Returning {} recipes.", recipes.size());
        return ResponseEntity.ok(recipes);
    }

    @Operation(summary = "Get recipe by ID", description = "Retrieves the details of a specific recipe by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recipe found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecipeResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found")
    })
    // Public endpoint
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponseDTO> getRecipeById(
            @Parameter(description = "ID of the recipe to retrieve", required = true) @PathVariable Long id) {
        log.info("Request received to get recipe by ID: {}", id);
        return recipeService.getRecipeById(id)
                .map(recipe -> {
                    log.debug("Recipe found for ID: {}", id);
                    return ResponseEntity.ok(recipe);
                })
                .orElseGet(() -> {
                    log.warn("Recipe not found for ID: {}", id);
                    return ResponseEntity.notFound().build(); // Returns 404
                });
    }
}
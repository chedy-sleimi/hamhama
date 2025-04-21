package com.hamhama.controller;

import com.hamhama.dto.SubstituteDTO;
import com.hamhama.model.Ingredient;
import com.hamhama.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; // Added Lombok
import org.slf4j.Logger; // Added logger
import org.slf4j.LoggerFactory; // Added logger
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // If using validation on DTOs
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Added for error handling

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor // Use Lombok for constructor injection
@Tag(name = "Ingredient Management", description = "Endpoints for managing ingredients, finding substitutes, and generating images.")
public class IngredientController {

    private static final Logger log = LoggerFactory.getLogger(IngredientController.class); // Added logger
    private final IngredientService ingredientService;

    // Constructor removed as @RequiredArgsConstructor handles it

    @Operation(summary = "Get all ingredients", description = "Retrieves a list of all available ingredients. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of ingredients",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))) // List<Ingredient>
    })
    // Public - no @SecurityRequirement
    @GetMapping
    public List<Ingredient> getAllIngredients() {
        log.info("Request received to get all ingredients");
        // Note: Returning entities directly might expose too much data. Consider an IngredientSummaryDTO.
        return ingredientService.getAllIngredients();
    }

    @Operation(summary = "Get ingredient by ID", description = "Retrieves details for a specific ingredient by its ID. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingredient found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Ingredient.class))), // Consider IngredientDTO
            @ApiResponse(responseCode = "404", description = "Ingredient not found")
    })
    // Public - no @SecurityRequirement
    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getIngredientById( // Changed return type to ResponseEntity
                                                         @Parameter(description = "ID of the ingredient to retrieve", required = true) @PathVariable Long id) {
        log.info("Request received to get ingredient by ID: {}", id);
        // Handle Optional return from service
        return ingredientService.getIngredientById(id)
                .map(ingredient -> {
                    log.debug("Ingredient found for ID: {}", id);
                    return ResponseEntity.ok(ingredient);
                })
                .orElseGet(() -> {
                    log.warn("Ingredient not found for ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @Operation(summary = "Add a new ingredient (Admin Only)", description = "Creates a new ingredient. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ingredient created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Ingredient.class))), // Consider IngredientDTO
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid ingredient data provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication and ADMIN role (checked by SecurityConfig)
    @PostMapping
    public ResponseEntity<Ingredient> addIngredient( // Changed return type
                                                     // Use fully qualified name for Swagger RequestBody annotation
                                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ingredient object for the new ingredient.", required = true,
                                                             content = @Content(schema = @Schema(implementation = Ingredient.class))) // Ideally IngredientCreateDTO
                                                     // Spring's RequestBody annotation remains on the parameter
                                                     @RequestBody @Validated Ingredient ingredient) { // Consider @Validated if DTO has constraints
        try {
            // *** CORRECTED LOG LINE ***
            log.info("Request received to add ingredient: {}", ingredient.getName()); // Use getName()
            Ingredient savedIngredient = ingredientService.addIngredient(ingredient);
            // *** CORRECTED LOG LINE ***
            log.info("Ingredient '{}' added successfully with ID: {}", savedIngredient.getName(), savedIngredient.getId()); // Use getName()
            // Return 201 Created status
            return ResponseEntity.status(HttpStatus.CREATED).body(savedIngredient);
        } catch (Exception e) { // Catch potential service layer exceptions (e.g., validation)
            // *** CORRECTED LOG LINE ***
            log.error("Error adding ingredient '{}': {}", ingredient.getName(), e.getMessage(), e); // Use getName()
            // Consider more specific error handling based on service exceptions
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to add ingredient: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "Update an ingredient (Admin Only)", description = "Updates an existing ingredient specified by ID. Requires ADMIN role. **Warning: Currently accepts full Ingredient object, recommend using a specific DTO.**")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ingredient updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Ingredient.class))), // Consider IngredientDTO
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid data or ID mismatch"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Ingredient not found")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication and ADMIN role
    @PutMapping("/{id}")
    public ResponseEntity<Ingredient> updateIngredient( // Changed return type
                                                        @Parameter(description = "ID of the ingredient to update", required = true) @PathVariable Long id,
                                                        // Use fully qualified name for Swagger RequestBody annotation
                                                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ingredient object with updated details. **Recommend IngredientUpdateDTO.**", required = true,
                                                                content = @Content(schema = @Schema(implementation = Ingredient.class))) // Ideally IngredientUpdateDTO
                                                        // Spring's RequestBody annotation remains on the parameter
                                                        @RequestBody @Validated Ingredient ingredientDetails) { // Consider @Validated if DTO has constraints
        // Note: Service layer should handle ID mismatch check and validation logic
        log.info("Request received to update ingredient ID: {}", id);
        try {
            // The service method should handle the update logic, including setting ID if needed.
            Ingredient updatedIngredient = ingredientService.updateIngredient(id, ingredientDetails);
            log.info("Ingredient ID {} updated successfully", id);
            return ResponseEntity.ok(updatedIngredient);
        } catch (IllegalArgumentException e) { // Example: if service throws this for not found
            log.warn("Ingredient ID {} not found for update.", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating ingredient ID {}: {}", id, e.getMessage(), e);
            // Handle other potential exceptions like validation errors if needed
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update ingredient: " + e.getMessage(), e);
        }

    }

    @Operation(summary = "Delete an ingredient (Admin Only)", description = "Deletes an ingredient specified by its ID. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ingredient deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Ingredient not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication and ADMIN role
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIngredient( // Changed return type
                                                  @Parameter(description = "ID of the ingredient to delete", required = true) @PathVariable Long id) {
        log.info("Request received to delete ingredient ID: {}", id);
        try {
            ingredientService.deleteIngredient(id); // Assuming this throws if not found
            log.info("Ingredient ID {} deleted successfully", id);
            return ResponseEntity.noContent().build(); // Return 204 No Content
        } catch (IllegalArgumentException e) { // Example: if service throws this for not found
            log.warn("Ingredient ID {} not found for deletion.", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting ingredient ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during deletion.", e);
        }
    }

    @Operation(summary = "Get substitutes for an ingredient", description = "Finds potential substitutes for a given ingredient ID. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Substitutes found (response may contain an empty list or error message)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubstituteDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Original ingredient not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error finding substitutes")
    })
    @SecurityRequirement(name = "bearerAuth") // Assumed authentication needed
    @PostMapping("/{id}/substitutes") // Using POST as it might trigger external API calls or complex logic
    public ResponseEntity<SubstituteDTO> getSubstitutes(
            @Parameter(description = "ID of the ingredient to find substitutes for", required = true) @PathVariable Long id) {
        log.info("Request received to find substitutes for ingredient ID: {}", id);
        try {
            SubstituteDTO response = ingredientService.getSubstitutes(id);
            // Service might return null or throw exception if ingredient not found.
            if (response == null) {
                log.warn("Ingredient ID {} not found for substitute lookup.", id);
                return ResponseEntity.notFound().build(); // Explicit 404 if service returns null
            }
            log.debug("Successfully found substitutes/info for ingredient ID {}", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) { // Catch specific "not found" exception from service
            log.warn("Ingredient ID {} not found for substitute lookup: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error finding substitutes for ingredient ID {}: {}", id, e.getMessage(), e);
            // Consider returning a specific error DTO or status code based on the exception
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while finding substitutes.", e);
        }
    }

    @Operation(summary = "Generate image based on ingredients", description = "Generates a PNG image representing a dish composed of the specified ingredients. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image generated successfully",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid or empty list of ingredient IDs"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error generating image")
    })
    @SecurityRequirement(name = "bearerAuth") // Assumed authentication needed
    @PostMapping(value = "/generate-image", produces = MediaType.IMAGE_PNG_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generateRecipeImage(
            // Use fully qualified name for Swagger RequestBody annotation
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON list of ingredient IDs to include in the generated image.", required = true,
                    content = @Content(schema = @Schema(implementation = List.class))) // List<Long>
            // Spring's RequestBody annotation remains on the parameter
            @RequestBody List<Long> ingredientIds) {
        log.info("Request received to generate image for ingredient IDs: {}", ingredientIds);
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            log.warn("Received empty ingredient ID list for image generation.");
            // Return 400 Bad Request for empty input
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient ID list cannot be empty.");
        }
        try {
            byte[] imageBytes = ingredientService.generateRecipeImage(ingredientIds);
            if (imageBytes == null || imageBytes.length == 0) {
                log.error("Image generation returned empty result for ingredient IDs: {}", ingredientIds);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Image generation failed.");
            }
            log.debug("Successfully generated image for ingredient IDs: {}", ingredientIds);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Error generating image for ingredient IDs {}: {}", ingredientIds, e.getMessage(), e);
            // Consider more specific error handling based on service exceptions
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during image generation.", e);
        }
    }
}
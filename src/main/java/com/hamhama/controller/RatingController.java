package com.hamhama.controller;

import com.hamhama.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; // Using Lombok for consistency
import org.slf4j.Logger; // Added logger
import org.slf4j.LoggerFactory; // Added logger
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // For better error handling

@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor // Use Lombok constructor injection
@Tag(name = "Rating Management", description = "Endpoints for managing recipe ratings.")
public class RatingController {

    private static final Logger log = LoggerFactory.getLogger(RatingController.class); // Added logger
    private final RatingService ratingService;

    // Constructor removed as @RequiredArgsConstructor handles it

    @Operation(summary = "Rate a recipe", description = "Allows an authenticated user to add or update their rating for a specific recipe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating added/updated successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid rating value (must be 1-5) or recipe ID missing"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"), // If service checks recipe existence
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication to know who is rating
    @PostMapping("/rate")
    public ResponseEntity<String> rateRecipe(
            @Parameter(description = "ID of the recipe to rate", required = true) @RequestParam Long recipeId,
            @Parameter(description = "The rating value (integer between 1 and 5)", required = true) @RequestParam int ratingValue) {

        log.info("Request received to rate recipe ID {} with value {}", recipeId, ratingValue);
        // Validate rating value (moved from service for early exit)
        if (ratingValue < 1 || ratingValue > 5) {
            log.warn("Invalid rating value {} received for recipe ID {}", ratingValue, recipeId);
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }

        try {
            // Service likely needs the authenticated user, assuming it gets it from SecurityContextHolder
            ratingService.addRating(recipeId, ratingValue);
            log.info("Rating added/updated successfully for recipe ID {}", recipeId);
            return ResponseEntity.ok("Rating added successfully");
        } catch (IllegalArgumentException e) { // Example: If service throws this for Recipe not found
            log.warn("Failed to add rating for recipe ID {}: {}", recipeId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) { // Catch other unexpected errors
            log.error("Error adding rating for recipe ID {}: {}", recipeId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while adding the rating.", e);
        }
    }

    @Operation(summary = "Get average rating for a recipe", description = "Retrieves the calculated average rating for a specific recipe. This is a public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Average rating retrieved successfully (can be 0.0 if no ratings)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Double.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found") // If service needs to validate recipe ID exists
    })
    // No @SecurityRequirement - Assuming this is public
    @GetMapping("/recipe/{recipeId}/average")
    public ResponseEntity<Double> getAverageRating(
            @Parameter(description = "ID of the recipe to get the average rating for", required = true) @PathVariable Long recipeId) {
        log.info("Request received for average rating for recipe ID {}", recipeId);
        try {
            double average = ratingService.getAverageRating(recipeId);
            log.debug("Average rating for recipe ID {} is {}", recipeId, average);
            return ResponseEntity.ok(average);
        } catch (IllegalArgumentException e) { // Example: If service throws this for Recipe not found
            log.warn("Failed to get average rating for recipe ID {}: {}", recipeId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error getting average rating for recipe ID {}: {}", recipeId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while calculating the average rating.", e);
        }
    }

    @Operation(summary = "Delete a rating", description = "Allows an authenticated user to delete their rating for a specific recipe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating deleted successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in"),
            @ApiResponse(responseCode = "404", description = "Recipe not found or user hasn't rated this recipe"), // If service checks these
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication to know whose rating to delete
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteRating(
            @Parameter(description = "ID of the recipe whose rating should be deleted by the current user", required = true) @RequestParam Long recipeId) {
        log.info("Request received to delete rating for recipe ID {}", recipeId);
        try {
            // Service likely needs the authenticated user, assuming it gets it from SecurityContextHolder
            ratingService.deleteRating(recipeId);
            log.info("Rating deleted successfully for recipe ID {}", recipeId);
            return ResponseEntity.ok("Rating deleted successfully");
        } catch (IllegalArgumentException e) { // Example: If service throws this for Recipe/Rating not found
            log.warn("Failed to delete rating for recipe ID {}: {}", recipeId, e.getMessage());
            // Distinguish between Recipe not found and Rating not found if possible
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting rating for recipe ID {}: {}", recipeId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while deleting the rating.", e);
        }
    }
}
package com.hamhama.service;

import com.hamhama.model.Rating;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.RatingRepository;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize; // Import
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RecipeRepository recipeRepository;
    private final RatingRepository ratingRepository;
    // UserRepository might not be strictly needed if we rely on the principal
    // private final UserRepository userRepository;

    /**
     * Adds or updates a rating for a recipe by the currently authenticated user.
     *
     * @param recipeId    ID of the recipe being rated.
     * @param ratingValue The rating value (1-5).
     */
    public Rating addRating(Long recipeId, int ratingValue) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.warn("Recipe not found with ID: {}", recipeId);
                    return new RuntimeException("Recipe not found");
                });

        // Check if the user has already rated the recipe
        Rating existingRating = ratingRepository.findByUserAndRecipe(currentUser, recipe);
        Rating savedRating;

        if (existingRating != null) {
            log.debug("User '{}' updating existing rating for recipe ID {}", currentUser.getUsername(), recipeId);
            existingRating.setRatingValue(ratingValue);
            savedRating = ratingRepository.save(existingRating);
        } else {
            log.debug("User '{}' adding new rating for recipe ID {}", currentUser.getUsername(), recipeId);
            Rating newRating = new Rating();
            newRating.setRatingValue(ratingValue);
            newRating.setRecipe(recipe);
            newRating.setUser(currentUser); // Use authenticated user
            savedRating = ratingRepository.save(newRating);
        }

        // Update the average rating of the recipe (could be optimized)
        updateAverageRating(recipe);
        log.info("Rating added/updated successfully for recipe ID {} by user '{}'. New average: {}", recipeId, currentUser.getUsername(), recipe.getAverageRating());
        return savedRating;
    }

    /**
     * Updates the average rating for a given recipe.
     * (Consider if this needs to be public or can remain private)
     *
     * @param recipe The recipe to update.
     */
    // Made public in case it needs to be triggered elsewhere, but consider if private is sufficient
    public void updateAverageRating(Recipe recipe) {
        // Refresh the recipe to get the latest ratings list, especially if run in a separate transaction
        Recipe freshRecipe = recipeRepository.findById(recipe.getId()).orElse(recipe); // Fallback to passed-in recipe
        List<Rating> ratings = freshRecipe.getRatings(); // Use potentially refreshed list

        if (ratings == null || ratings.isEmpty()) {
            freshRecipe.setAverageRating(0.0);
        } else {
            double totalRating = ratings.stream().mapToInt(Rating::getRatingValue).sum();
            freshRecipe.setAverageRating(totalRating / ratings.size());
        }
        log.debug("Updating average rating for recipe ID {} to {}", freshRecipe.getId(), freshRecipe.getAverageRating());
        recipeRepository.save(freshRecipe); // Save the updated recipe
    }

    /**
     * Gets the average rating for a recipe. (Publicly accessible)
     *
     * @param recipeId ID of the recipe.
     * @return The average rating, or 0.0 if no ratings exist.
     */
    @Transactional(readOnly = true)
    public double getAverageRating(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.warn("Recipe not found with ID: {}", recipeId);
                    return new RuntimeException("Recipe not found");
                });

        // If recipe.getAverageRating() returns primitive double, it cannot be null.
        // The updateAverageRating method should ensure it's 0.0 if no ratings exist.
        return recipe.getAverageRating(); // CORRECTED: Directly return the double value.
    }

    /**
     * Deletes a specific rating. Only the rating owner or an ADMIN can delete.
     *
     * @param recipeId ID of the recipe the rating belongs to.
     */
    // We find the rating based on current user and recipeId, then PreAuthorize checks ownership of *that* rating.
    // Simpler approach: Delete based on user and recipe, assuming only owner calls this endpoint.
    // Let's use the user ID and recipe ID approach first.
    public void deleteRating(Long recipeId) {
        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Rating existingRating = ratingRepository.findByUserAndRecipe(currentUser, recipe);
        if (existingRating == null) {
            log.warn("User '{}' attempted to delete non-existent rating for recipe ID {}", currentUser.getUsername(), recipeId);
            throw new RuntimeException("Rating not found for this user and recipe");
        }

        // No need for @PreAuthorize here as we fetched the rating FOR the current user.
        // If an admin needed to delete *any* rating by ID, a different method with @PreAuthorize would be needed.

        ratingRepository.delete(existingRating);
        log.info("Rating deleted successfully for recipe ID {} by user '{}'.", recipeId, currentUser.getUsername());

        // Update the average rating
        updateAverageRating(recipe); // Pass the fetched recipe
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
}
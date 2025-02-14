package com.hamhama.service;

import com.hamhama.model.Rating;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.RatingRepository;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingService {

    private final RecipeRepository recipeRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    public RatingService(RecipeRepository recipeRepository, RatingRepository ratingRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
    }

    public void addRating(Long userId, Long recipeId, int ratingValue) {
        // Check if the user and recipe exist
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Check if the user has already rated the recipe
        Rating existingRating = ratingRepository.findByUserAndRecipe(user, recipe);

        if (existingRating != null) {
            // Update the existing rating
            existingRating.setRatingValue(ratingValue);
            ratingRepository.save(existingRating);
        } else {
            // Create a new rating
            Rating newRating = new Rating();
            newRating.setRatingValue(ratingValue);
            newRating.setRecipe(recipe);
            newRating.setUser(user);
            ratingRepository.save(newRating);
        }

        // Update the average rating of the recipe
        updateAverageRating(recipe);
    }

    private void updateAverageRating(Recipe recipe) {
        // Get all ratings for the recipe
        List<Rating> ratings = recipe.getRatings();

        // Calculate the average rating
        double totalRating = 0;
        for (Rating rating : ratings) {
            totalRating += rating.getRatingValue();
        }
        double averageRating = totalRating / ratings.size();

        // Update the recipe's average rating
        recipe.setAverageRating(averageRating);

        // Save the updated recipe
        recipeRepository.save(recipe);
    }
    public double getAverageRating(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (recipe.getRatings().isEmpty()) {
            return 0.0; // If no ratings yet, return 0
        }

        double sum = recipe.getRatings().stream().mapToInt(Rating::getRatingValue).sum();
        return sum / recipe.getRatings().size(); // Calculate the average
    }
    public void deleteRating(Long userId, Long recipeId) {
        // Check if the user and recipe exist
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Find the existing rating
        Rating existingRating = ratingRepository.findByUserAndRecipe(user, recipe);
        if (existingRating == null) {
            throw new RuntimeException("Rating not found for this user and recipe");
        }

        // Delete the rating
        ratingRepository.delete(existingRating);

        // Update the average rating of the recipe
        updateAverageRating(recipe);
    }
}

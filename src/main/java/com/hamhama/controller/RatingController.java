package com.hamhama.controller;

import com.hamhama.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/rate")
    public ResponseEntity<String> rateRecipe(@RequestParam Long userId, @RequestParam Long recipeId, @RequestParam int ratingValue) {
        // Validate rating value
        if (ratingValue < 1 || ratingValue > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5");
        }

        // Add rating for the recipe
        ratingService.addRating(userId, recipeId, ratingValue);

        return ResponseEntity.ok("Rating added successfully");
    }
    @GetMapping("/recipe/{recipeId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long recipeId) {
        double average = ratingService.getAverageRating(recipeId);
        return ResponseEntity.ok(average);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteRating(@RequestParam Long userId, @RequestParam Long recipeId) {
        ratingService.deleteRating(userId, recipeId);
        return ResponseEntity.ok("Rating deleted successfully");
    }
}


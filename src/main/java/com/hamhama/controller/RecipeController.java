package com.hamhama.controller;

import com.hamhama.dto.RecipeDTO;
import com.hamhama.dto.RecipeResponseDTO;
import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import com.hamhama.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    //  Add a new recipe (with category validation)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addRecipe(@RequestBody RecipeDTO recipe) {
        try {
            Recipe savedRecipe = recipeService.addRecipe(recipe);
            return ResponseEntity.ok(savedRecipe);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //  Update a recipe
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe) {
        Recipe updatedRecipe = recipeService.updateRecipe(id, recipe);
        if (updatedRecipe != null) {
            return ResponseEntity.ok(updatedRecipe);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //  Delete a recipe
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    //  Search recipes (by name, description, or ingredient)
    @GetMapping("/search")
    public ResponseEntity<List<Recipe>> searchRecipes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String ingredient) {
        List<Recipe> results = recipeService.searchRecipes(name, description, ingredient);
        return ResponseEntity.ok(results);
    }

    //  Get recipes by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Recipe>> getRecipesByCategory(@PathVariable RecipeCategory category) {
        List<Recipe> recipes = recipeService.getRecipesByCategory(category);
        return ResponseEntity.ok(recipes);
    }

    //  Get recipes by multiple categories
    @PostMapping("/categories")
    public ResponseEntity<List<Recipe>> getRecipesByCategories(@RequestBody List<RecipeCategory> categories) {
        List<Recipe> recipes = recipeService.getRecipesByCategories(categories);
        return ResponseEntity.ok(recipes);
    }

    @GetMapping(value = "/{id}/nutrition", produces = "image/svg+xml")
    public ResponseEntity<String> getNutritionFacts(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(recipeService.generateNutritionalFacts(id));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError()
                    .body("""
                <svg viewBox="0 0 200 50">
                    <text x="10" y="20" font-family="Arial" font-size="12" fill="red">
                        Error: ${e.getMessage()}
                    </text>
                </svg>
                """.replace("${e.getMessage()}", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<RecipeResponseDTO>> getAllRecipes() {
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponseDTO> getRecipeById(@PathVariable Long id) {
        return recipeService.getRecipeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}

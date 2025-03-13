package com.hamhama.controller;

import com.hamhama.dto.SubstituteDTO;
import com.hamhama.model.Ingredient;
import com.hamhama.service.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {
    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public List<Ingredient> getAllIngredients() {
        return ingredientService.getAllIngredients();
    }

    @GetMapping("/{id}")
    public Optional<Ingredient> getIngredientById(@PathVariable Long id) {
        return ingredientService.getIngredientById(id);
    }

    @PostMapping
    public Ingredient addIngredient(@RequestBody Ingredient ingredient) {
        return ingredientService.addIngredient(ingredient);
    }
    // Updated to handle only 'name' field for update
    @PutMapping("/{id}")
    public Ingredient updateIngredient(@PathVariable Long id, @RequestBody Ingredient ingredient) {
        return ingredientService.updateIngredient(id, ingredient);
    }

    @DeleteMapping("/{id}")
    public void deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
    }

    // In IngredientController.java
    @PostMapping("/{id}/substitutes")
    public ResponseEntity<SubstituteDTO> getSubstitutes(@PathVariable Long id) {
        try {
            SubstituteDTO response = ingredientService.getSubstitutes(id);

            if (response != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // Create error response DTO
            SubstituteDTO errorResponse = new SubstituteDTO();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/generate-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateRecipeImage(@RequestBody List<Long> ingredientIds) {
        try {
            byte[] imageBytes = ingredientService.generateRecipeImage(ingredientIds);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

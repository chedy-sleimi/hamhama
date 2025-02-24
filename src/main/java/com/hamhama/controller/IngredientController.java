package com.hamhama.controller;

import com.hamhama.model.Ingredient;
import com.hamhama.service.IngredientService;
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
}

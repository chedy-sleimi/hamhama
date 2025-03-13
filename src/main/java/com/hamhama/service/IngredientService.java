package com.hamhama.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamhama.dto.SubstituteDTO;
import com.hamhama.model.Ingredient;
import com.hamhama.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final GeminiService geminiService;

    public IngredientService(IngredientRepository ingredientRepository , GeminiService geminiService) {
        this.ingredientRepository = ingredientRepository;
        this.geminiService = geminiService;
    }

    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }
    
    public Optional<Ingredient> getIngredientById(Long id) {
        return ingredientRepository.findById(id);
    }

    public Ingredient addIngredient(Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    // Updated to handle only 'name' field for update
    public Ingredient updateIngredient(Long id, Ingredient ingredientDetails) {
        Optional<Ingredient> existingIngredient = ingredientRepository.findById(id);
        if (existingIngredient.isPresent()) {
            Ingredient updatedIngredient = existingIngredient.get();
            updatedIngredient.setName(ingredientDetails.getName());  // Only update the 'name'
            return ingredientRepository.save(updatedIngredient);
        }
        return null; // Or throw an exception if not found
    }

    // Delete method
    public void deleteIngredient(Long id) {
        ingredientRepository.deleteById(id);
    }

    public SubstituteDTO getSubstitutes(Long id) throws Exception {
        // Directly return the result from GeminiService
        Optional<Ingredient> ingredient = ingredientRepository.findById(id);

        if (ingredient.isPresent()) {
            Ingredient i = ingredient.get();
            return geminiService.getSubstitutes(i.getName());
        }

        return null;
    }

    public byte[] generateRecipeImage(List<Long> ingredientIds) throws Exception {
        List<Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds);
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("No ingredients found");
        }

        List<String> ingredientNames = ingredients.stream()
                .map(Ingredient::getName)
                .toList();

        return geminiService.generateRecipeImage(ingredientNames);
    }

}
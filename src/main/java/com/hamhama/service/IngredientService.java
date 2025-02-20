package com.hamhama.service;

import com.hamhama.model.Ingredient;
import com.hamhama.repository.IngredientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class IngredientService {

    private final String SPOONACULAR_API_KEY = "aa40fef2d8fc437693cf7ee37c821c4c"; // Replace with your Spoonacular API key
    private final String SPOONACULAR_URL = "https://api.spoonacular.com/food/ingredients/substitutes";

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
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

    public List<String>




























    getIngredientSubstitutes(String ingredientName) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SPOONACULAR_URL + "?ingredientName=" + ingredientName + "&apiKey=" + SPOONACULAR_API_KEY;

        try {
            // Call Spoonacular API
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // Check if substitutes exist in the response
            if (response == null || !response.containsKey("substitutes")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No substitutes found for " + ingredientName);
            }

            // Extract substitutes from the response
            List<String> substitutes = (List<String>) response.get("substitutes");
            return substitutes;
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), "Error calling Spoonacular API: " + e.getMessage());
        }
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
}
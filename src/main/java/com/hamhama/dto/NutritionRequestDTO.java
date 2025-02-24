package com.hamhama.dto;

import java.util.List;

public class NutritionRequestDTO {
    private String recipeName;
    private List<IngredientDTO> ingredients;

    // getters and setters

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public List<IngredientDTO> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientDTO> ingredients) {
        this.ingredients = ingredients;
    }
}
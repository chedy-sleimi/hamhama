package com.hamhama.dto;

import com.hamhama.model.RecipeCategory; // Ensure this import exists
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List; // If ingredients are added

@Data // Includes getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponseDTO {
    private Long id;
    private String name;
    private String description;
    private RecipeCategory category;
    private Double averageRating; // Use Double wrapper type if average can be conceptually null before calculation
    private String imageUrl;
    private String authorUsername; // <<< ADDED FIELD

    // Optional: Add ingredients if needed in response
    // private List<IngredientInRecipeDTO> ingredients;
}
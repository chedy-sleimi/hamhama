package com.hamhama.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = RecipeCategoryDeserializer.class)
public enum RecipeCategory {
    @JsonProperty("VEGETARIAN")  VEGETARIAN,
    @JsonProperty("NON_VEGETARIAN") NON_VEGETARIAN,
    @JsonProperty("HIGH_PROTEIN") HIGH_PROTEIN,
    @JsonProperty("LOW_CARB") LOW_CARB
}

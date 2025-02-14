package com.hamhama.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

public class RecipeCategoryDeserializer extends JsonDeserializer<RecipeCategory> {
    @Override
    public RecipeCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return RecipeCategory.valueOf(p.getText().toUpperCase());
    }
}

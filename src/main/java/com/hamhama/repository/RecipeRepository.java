package com.hamhama.repository;

import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    // --- Existing Search Methods ---
    List<Recipe> findByNameContainingIgnoreCase(String name);
    List<Recipe> findByDescriptionContainingIgnoreCase(String description);

    @Query("SELECT DISTINCT r FROM Recipe r " +
            "LEFT JOIN r.recipeIngredients ri " + // Use LEFT JOIN if a recipe might have no ingredients
            "LEFT JOIN ri.ingredient i " +
            "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :ingredient, '%'))")
    List<Recipe> findByIngredientsNameContainingIgnoreCase(@Param("ingredient") String ingredient);

    // --- Category Specific Methods ---
    List<Recipe> findByCategory(RecipeCategory category);
    List<Recipe> findByCategoryIn(List<RecipeCategory> categories);

    // --- New Combined Search Methods ---
    List<Recipe> findByCategoryAndNameContainingIgnoreCase(RecipeCategory category, String name);
    List<Recipe> findByCategoryAndDescriptionContainingIgnoreCase(RecipeCategory category, String description);

    @Query("SELECT DISTINCT r FROM Recipe r " +
            "LEFT JOIN r.recipeIngredients ri " +
            "LEFT JOIN ri.ingredient i " +
            "WHERE r.category = :category AND LOWER(i.name) LIKE LOWER(CONCAT('%', :ingredient, '%'))")
    List<Recipe> findByCategoryAndIngredientsNameContainingIgnoreCase(@Param("category") RecipeCategory category, @Param("ingredient") String ingredient);

}
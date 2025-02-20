package com.hamhama.repository;

import com.hamhama.model.Recipe;
import com.hamhama.model.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByNameContainingIgnoreCase(String name);

    List<Recipe> findByDescriptionContainingIgnoreCase(String description);

    @Query("SELECT DISTINCT r FROM Recipe r " +
            "JOIN r.recipeIngredients ri " +
            "JOIN ri.ingredient i " +
            "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :ingredient, '%'))")
    List<Recipe> findByIngredientsNameContainingIgnoreCase(@Param("ingredient") String ingredient);

    List<Recipe> findByCategory(RecipeCategory category);

    List<Recipe> findByCategoryIn(List<RecipeCategory> categories);
}
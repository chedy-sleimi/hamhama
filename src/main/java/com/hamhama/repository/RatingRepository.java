package com.hamhama.repository;

import com.hamhama.model.Rating;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Rating findByUserAndRecipe(User user, Recipe recipe);
}

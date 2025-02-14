package com.hamhama.repository;

import com.hamhama.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find all comments for a specific recipe
    List<Comment> findByRecipeId(Long recipeId);

    // Find all comments by a specific user
    List<Comment> findByUserId(Long userId);
}

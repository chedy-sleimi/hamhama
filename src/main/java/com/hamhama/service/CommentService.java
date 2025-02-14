package com.hamhama.service;

import com.hamhama.model.Comment;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.CommentRepository;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, RecipeRepository recipeRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    // Add a new comment
    public void addComment(Long userId, Long recipeId, String content) {
        // Check if the user and recipe exist
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Create and save the comment
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setRecipe(recipe);
        comment.setUser(user);
        commentRepository.save(comment);
    }

    // Get all comments for a specific recipe
    public List<Comment> getCommentsByRecipe(Long recipeId) {
        return commentRepository.findByRecipeId(recipeId);
    }

    // Delete a comment by its ID
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}

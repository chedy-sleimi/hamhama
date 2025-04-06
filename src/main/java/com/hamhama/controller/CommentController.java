package com.hamhama.controller;

import com.hamhama.model.Comment;
import com.hamhama.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Add a new comment
    @PostMapping("/add")
    public ResponseEntity<String> addComment(@RequestParam Long recipeId, @RequestParam String content) {
        try {
            commentService.addComment(recipeId, content);
            return ResponseEntity.ok("Comment added successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get all comments for a specific recipe
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<List<Comment>> getCommentsByRecipe(@PathVariable Long recipeId) {
        List<Comment> comments = commentService.getCommentsByRecipe(recipeId);
        return ResponseEntity.ok(comments);
    }

    // Delete a comment by its ID
    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);
            return ResponseEntity.ok("Comment deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

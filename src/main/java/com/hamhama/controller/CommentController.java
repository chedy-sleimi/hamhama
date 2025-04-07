package com.hamhama.controller;

import com.hamhama.dto.CommentDTO; // Import DTO
import com.hamhama.model.Comment;
import com.hamhama.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    public ResponseEntity<List<CommentDTO>> getCommentsByRecipe(@PathVariable Long recipeId) { // Return DTO list
        List<CommentDTO> comments = commentService.getCommentsByRecipe(recipeId);
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

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId,
            @RequestParam String content
    ) {
        try {
            Comment updatedDto = commentService.updateComment(commentId, content);
            return ResponseEntity.ok(updatedDto);
        } catch (RuntimeException e) {
            if (e instanceof AccessDeniedException) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
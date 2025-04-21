package com.hamhama.controller;

import com.hamhama.dto.CommentDTO; // Import DTO
import com.hamhama.model.Comment; // Only needed if returned directly, which updateComment does
import com.hamhama.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; // Use Lombok
import org.slf4j.Logger; // Add Logger
import org.slf4j.LoggerFactory; // Add LoggerFactory
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Use for standard errors

import java.util.List;

@RestController
@RequestMapping("/comments") // Consider prefixing with /api like others, e.g., /api/comments
@RequiredArgsConstructor // Use Lombok for constructor injection
@Tag(name = "Comment Management", description = "Endpoints for managing recipe comments.")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class); // Added Logger
    private final CommentService commentService;

    // Constructor removed as @RequiredArgsConstructor handles it

    @Operation(summary = "Add a comment to a recipe", description = "Allows an authenticated user to add a comment to a specific recipe.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment added successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input (e.g., missing recipeId, empty content)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in"),
            @ApiResponse(responseCode = "404", description = "Recipe not found"), // If service checks recipe existence
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication
    @PostMapping("/add")
    public ResponseEntity<String> addComment(
            @Parameter(description = "ID of the recipe to comment on", required = true) @RequestParam Long recipeId,
            @Parameter(description = "The content of the comment", required = true) @RequestParam String content) {
        log.info("Request received to add comment to recipe ID: {}", recipeId);
        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempted to add empty comment to recipe ID: {}", recipeId);
            return ResponseEntity.badRequest().body("Comment content cannot be empty");
        }
        try {
            // Assuming commentService gets the user from SecurityContextHolder
            commentService.addComment(recipeId, content);
            log.info("Comment added successfully to recipe ID: {}", recipeId);
            return ResponseEntity.ok("Comment added successfully");
        } catch (IllegalArgumentException e) { // Catch specific service exceptions if possible
            log.warn("Failed to add comment to recipe ID {}: {}", recipeId, e.getMessage());
            // Distinguish between Recipe not found and other issues if service throws specific exceptions
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding comment to recipe ID {}: {}", recipeId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while adding the comment.", e);
        }
    }

    @Operation(summary = "Get comments for a recipe", description = "Retrieves all comments associated with a specific recipe ID. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved comments (list may be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))), // List<CommentDTO>
            @ApiResponse(responseCode = "404", description = "Recipe not found") // If service validates recipe ID
    })
    // Public - no @SecurityRequirement
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByRecipe(
            @Parameter(description = "ID of the recipe whose comments are to be retrieved", required = true) @PathVariable Long recipeId) {
        log.info("Request received to get comments for recipe ID: {}", recipeId);
        try {
            List<CommentDTO> comments = commentService.getCommentsByRecipe(recipeId);
            log.debug("Found {} comments for recipe ID: {}", comments.size(), recipeId);
            return ResponseEntity.ok(comments);
        } catch (IllegalArgumentException e) { // Example: If service throws for Recipe not found
            log.warn("Recipe not found when retrieving comments for ID: {}", recipeId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving comments for recipe ID {}: {}", recipeId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while retrieving comments.", e);
        }
    }

    @Operation(summary = "Delete a comment", description = "Allows an authenticated user to delete their own comment, or an admin to delete any comment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment deleted successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to delete this comment"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication
    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<String> deleteComment(
            @Parameter(description = "ID of the comment to delete", required = true) @PathVariable Long commentId) {
        log.info("Request received to delete comment ID: {}", commentId);
        try {
            // Assuming commentService handles ownership/admin checks and throws exceptions
            commentService.deleteComment(commentId);
            log.info("Comment deleted successfully for ID: {}", commentId);
            return ResponseEntity.ok("Comment deleted successfully");
        } catch (AccessDeniedException e) {
            log.warn("Access denied attempt to delete comment ID {}: {}", commentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Example: If service throws this for Comment not found
            log.warn("Comment not found for deletion with ID: {}", commentId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error deleting comment ID {}: {}", commentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while deleting the comment.", e);
        }
    }

    @Operation(summary = "Update a comment", description = "Allows an authenticated user to update the content of their own comment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Comment.class))), // Should be CommentDTO if service returns DTO
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input (e.g., empty content)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not logged in"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to update this comment"),
            @ApiResponse(responseCode = "404", description = "Comment not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @SecurityRequirement(name = "bearerAuth") // Requires authentication
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment( // Return type could be CommentDTO
                                            @Parameter(description = "ID of the comment to update", required = true) @PathVariable Long commentId,
                                            @Parameter(description = "The updated content for the comment", required = true) @RequestParam String content) {
        log.info("Request received to update comment ID: {}", commentId);
        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempted to update comment ID {} with empty content", commentId);
            return ResponseEntity.badRequest().body("Comment content cannot be empty");
        }
        try {
            // Assuming commentService handles ownership checks and returns updated entity/DTO
            // The current service returns Comment entity, adjust schema if it changes to DTO
            Comment updatedComment = commentService.updateComment(commentId, content);
            log.info("Comment ID {} updated successfully", commentId);
            return ResponseEntity.ok(updatedComment); // Ideally return CommentDTO
        } catch (AccessDeniedException e) {
            log.warn("Access denied attempt to update comment ID {}: {}", commentId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Example: If service throws this for Comment not found
            log.warn("Comment not found for update with ID: {}", commentId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating comment ID {}: {}", commentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while updating the comment.", e);
        }
    }
}
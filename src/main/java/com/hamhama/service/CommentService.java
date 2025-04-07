package com.hamhama.service;

import com.hamhama.dto.CommentDTO; // Import DTO
import com.hamhama.model.Comment;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.CommentRepository;
import com.hamhama.repository.RecipeRepository;
// No need for UserRepository if getting user from Comment entity
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors; // Import Collectors

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    // private final UserRepository userRepository; // Removed if not needed directly

    // --- addComment and deleteComment remain mostly the same ---
    // (Ensure they fetch the current user correctly as before)
    public Comment addComment(Long recipeId, String content) {
        User currentUser = getCurrentUser();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.warn("Recipe not found with ID: {}", recipeId);
                    return new RuntimeException("Recipe not found");
                });

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setRecipe(recipe);
        comment.setUser(currentUser);

        Comment savedComment = commentRepository.save(comment);
        log.info("User '{}' added comment ID {} to recipe ID {}", currentUser.getUsername(), savedComment.getId(), recipeId);
        return savedComment;
    }

    /**
     * Get all comments for a specific recipe as DTOs.
     *
     * @param recipeId ID of the recipe.
     * @return List of CommentDTOs.
     */
    @Transactional(readOnly = true) // Read-only transaction
    public List<CommentDTO> getCommentsByRecipe(Long recipeId) {
        if (!recipeRepository.existsById(recipeId)) {
            log.warn("Attempted to get comments for non-existent recipe ID: {}", recipeId);
            // Depending on requirements, could return empty list or throw exception
            // throw new RuntimeException("Recipe not found");
            return List.of(); // Return empty list if recipe doesn't exist
        }
        List<Comment> comments = commentRepository.findByRecipeId(recipeId);
        log.debug("Found {} comments for recipe ID {}", comments.size(), recipeId);
        // Convert to DTOs
        return comments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    // --- Existing Helper Methods ---
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        return (User) authentication.getPrincipal();
    }

    public CommentDTO convertToDto(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setTimestamp(comment.getTimestamp());
        dto.setRecipeId(comment.getRecipe() != null ? comment.getRecipe().getId() : null);

        if (comment.getUser() != null) {
            User author = comment.getUser();
            dto.setUsername(author.getUsername());
            dto.setUserId(author.getId());
            // Construct profile picture URL (adjust path if needed)
            // Assuming pictures are named {userId}.jpg and served from /profile-pictures/
            // We don't check existence here, frontend will handle 404 fallback
            dto.setAuthorProfilePictureUrl("/profile-pictures/" + author.getId() + ".jpg");
        } else {
            dto.setUsername("Unknown User");
            dto.setUserId(null);
            dto.setAuthorProfilePictureUrl(null); // No picture for unknown user
        }
        return dto;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    /**
     * Updates an existing comment. Only the owner can update.
     * @param commentId ID of the comment to update.
     * @param newContent The new text content for the comment.
     */
    @PreAuthorize("@commentRepository.findById(#commentId).orElse(null)?.user?.username == principal.username")
    public Comment updateComment(Long commentId, String newContent) {
        // @PreAuthorize handles the ownership check before the method body executes.
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found for update with ID: {}", commentId);
                    return new RuntimeException("Comment not found");
                });

        log.info("User '{}' updating comment ID {}", getCurrentUsername(), commentId);
        comment.setContent(newContent);
        // Timestamp is usually not updated on edit, but could be if needed
        // comment.setTimestamp(LocalDateTime.now());
        return commentRepository.save(comment);
    }


    @PreAuthorize("hasRole('ADMIN') or @commentRepository.findById(#commentId).orElse(null)?.user?.username == principal.username")
    public void deleteComment(Long commentId) {
        // @PreAuthorize handles the ownership/admin check.
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found for deletion with ID: {}", commentId);
                    return new RuntimeException("Comment not found");
                });
        commentRepository.deleteById(commentId);
        log.info("Comment ID {} deleted successfully by user '{}' or ADMIN.", commentId, getCurrentUsername());
    }
}
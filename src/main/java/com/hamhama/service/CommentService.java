package com.hamhama.service;

import com.hamhama.model.Comment;
import com.hamhama.model.Recipe;
import com.hamhama.model.User;
import com.hamhama.repository.CommentRepository;
import com.hamhama.repository.RecipeRepository;
import com.hamhama.repository.UserRepository;
import lombok.RequiredArgsConstructor; // Use Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException; // Import
import org.springframework.security.access.prepost.PreAuthorize; // Import
import org.springframework.security.core.Authentication; // Import
import org.springframework.security.core.context.SecurityContextHolder; // Import
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import

import java.util.List;
import java.util.Objects; // Import

@Service
@RequiredArgsConstructor // Use Lombok for constructor injection
@Transactional // Add Transactional to service methods modifying data
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository; // Keep for fetching user if needed, but rely on principal

    /**
     * Add a new comment. Associates comment with the currently authenticated user.
     *
     * @param recipeId ID of the recipe to comment on.
     * @param content  The comment text.
     */
    public Comment addComment(Long recipeId, String content) {
        // Get the currently authenticated user principal
        User currentUser = getCurrentUser();

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    log.warn("Recipe not found with ID: {}", recipeId);
                    return new RuntimeException("Recipe not found");
                });

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setRecipe(recipe);
        comment.setUser(currentUser); // Set the currently logged-in user

        Comment savedComment = commentRepository.save(comment);
        log.info("User '{}' added comment ID {} to recipe ID {}", currentUser.getUsername(), savedComment.getId(), recipeId);
        return savedComment;
    }

    /**
     * Get all comments for a specific recipe. (Publicly accessible as per SecurityConfig)
     *
     * @param recipeId ID of the recipe.
     * @return List of comments.
     */
    @Transactional(readOnly = true) // Read-only transaction
    public List<Comment> getCommentsByRecipe(Long recipeId) {
        // Optional: Check if recipe exists first
        if (!recipeRepository.existsById(recipeId)) {
            log.warn("Attempted to get comments for non-existent recipe ID: {}", recipeId);
            throw new RuntimeException("Recipe not found");
        }
        return commentRepository.findByRecipeId(recipeId);
    }

    /**
     * Delete a comment by its ID. Only the comment owner or an ADMIN can delete.
     *
     * @param commentId ID of the comment to delete.
     */
    @PreAuthorize("hasRole('ADMIN') or @commentRepository.findById(#commentId).orElse(null)?.user?.username == principal.username")
    public void deleteComment(Long commentId) {
        // @PreAuthorize handles the check. If the user is not authorized, AccessDeniedException is thrown.
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found for deletion with ID: {}", commentId);
                    return new RuntimeException("Comment not found");
                });

        // Additional check just in case (belt and suspenders) - @PreAuthorize should cover this
        // User currentUser = getCurrentUser();
        // if (!isOwnerOrAdmin(currentUser, comment.getUser().getId())) {
        //    log.warn("User '{}' attempted to delete comment ID {} without permission.", currentUser.getUsername(), commentId);
        //    throw new AccessDeniedException("You do not have permission to delete this comment.");
        // }

        commentRepository.deleteById(commentId);
        log.info("Comment ID {} deleted successfully by user '{}' or ADMIN.", commentId, getCurrentUsername());
    }

    // Helper method to get the current User object
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            throw new AccessDeniedException("User is not authenticated."); // Or handle anonymous users differently
        }
        return (User) authentication.getPrincipal();
    }

    // Helper method to get the current username
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    // Example helper for manual check (if not using @PreAuthorize directly on repository)
    // private boolean isOwnerOrAdmin(User currentUser, Long ownerId) {
    //     boolean isAdmin = currentUser.getAuthorities().stream()
    //                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    //     return isAdmin || Objects.equals(currentUser.getId(), ownerId);
    // }
}
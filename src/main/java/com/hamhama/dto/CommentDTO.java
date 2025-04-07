// src/main/java/com/hamhama/dto/CommentDTO.java
package com.hamhama.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String username;
    private Long userId;
    private Long recipeId;
    private String authorProfilePictureUrl; // <-- Add this
}
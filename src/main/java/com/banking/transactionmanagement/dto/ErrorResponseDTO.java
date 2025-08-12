package com.banking.transactionmanagement.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response structure for API error handling.
 */
@Schema(description = "Standardized error response structure")
@Data
@NoArgsConstructor
public class ErrorResponseDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Error timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type", example = "Bad Request")
    private String error;
    
    @Schema(description = "Error message", example = "Validation failed")
    private String message;
    
    @Schema(description = "Request path", example = "/api/v1/transactions")
    private String path;
    
    @Schema(description = "Detailed validation errors")
    private List<ValidationErrorDetail> details;

    public ErrorResponseDTO(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Nested class for validation error details.
     */
    @Schema(description = "Validation error detail")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorDetail {
        @Schema(description = "Field name that failed validation", example = "amount")
        private String field;
        
        @Schema(description = "Validation error message", example = "Amount is required")
        private String message;
    }
}
package com.banking.transactionmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Data Transfer Object for paginated transaction responses.
 * Contains transaction list and pagination metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response containing transactions and pagination metadata")
public class PagedTransactionResponseDTO {

    @Schema(description = "List of transactions in current page")
    private List<TransactionResponseDTO> content;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    
    @Schema(description = "Number of items per page", example = "20")
    private int size;
    
    @Schema(description = "Total number of transactions", example = "100")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;
    
    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;
    
    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;
    
    @Schema(description = "Whether the page is empty", example = "false")
    private boolean empty;


}
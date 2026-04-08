package com.rag.knowledgehub.dto.kb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 500)
    private String description;
}

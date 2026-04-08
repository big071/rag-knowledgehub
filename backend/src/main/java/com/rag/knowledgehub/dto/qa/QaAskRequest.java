package com.rag.knowledgehub.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QaAskRequest {

    @NotNull
    private Long knowledgeBaseId;

    @NotBlank
    private String question;
}

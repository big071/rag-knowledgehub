package com.rag.knowledgehub.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUsageVO {

    private Long documentId;
    private String documentName;
    private Long count;
}

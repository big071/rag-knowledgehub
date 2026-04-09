package com.rag.knowledgehub.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSnippet {

    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String snippet;
    private Double score;
    private String sourceUrl;
}

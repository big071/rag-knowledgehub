package com.rag.knowledgehub.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaAnswerResponse {

    private String answer;
    private String summary;
    private String conversationId;
    private boolean cached;
    private long latencyMs;
    private List<SourceSnippet> sources;
}

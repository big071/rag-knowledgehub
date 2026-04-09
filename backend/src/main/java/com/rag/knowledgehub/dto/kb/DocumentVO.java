package com.rag.knowledgehub.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {

    private Long id;
    private Long knowledgeBaseId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private String reviewStatus;
    private Integer versionNo;
    private Long parentDocumentId;
    private Boolean latest;
    private Boolean sensitiveHit;
    private String sensitiveTip;
    private String tags;
    private String previewUrl;
    private LocalDateTime createdAt;
}

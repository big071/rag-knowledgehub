package com.rag.knowledgehub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private Long userId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String storagePath;

    private String sourceHash;

    private String parseStatus;

    private String reviewStatus;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewComment;

    private Integer versionNo;

    private Long parentDocumentId;

    private Boolean latest;

    private Boolean sensitiveHit;

    private String sensitiveTip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

package com.rag.knowledgehub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_tag_rel")
public class DocumentTagRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long tagId;

    private LocalDateTime createdAt;
}

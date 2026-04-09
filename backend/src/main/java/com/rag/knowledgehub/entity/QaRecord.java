package com.rag.knowledgehub.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_record")
public class QaRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeBaseId;

    private String question;

    private String answer;

    private String summary;

    private String conversationId;

    private String sourceJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

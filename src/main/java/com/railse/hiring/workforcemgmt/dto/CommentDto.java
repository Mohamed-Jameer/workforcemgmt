package com.railse.hiring.workforcemgmt.dto;

import lombok.Data;

@Data
public class CommentDto {
    private Long userId;
    private String comment;
    private Long timestamp;
}

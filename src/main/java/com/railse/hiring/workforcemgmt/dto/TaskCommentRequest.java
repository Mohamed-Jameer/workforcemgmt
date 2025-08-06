package com.railse.hiring.workforcemgmt.dto;


import lombok.Data;

@Data
public class TaskCommentRequest {
    private Long userId;
    private String comment;
}

package com.railse.hiring.workforcemgmt.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskActivity {
    private String activity;
    private Long timestamp;
}

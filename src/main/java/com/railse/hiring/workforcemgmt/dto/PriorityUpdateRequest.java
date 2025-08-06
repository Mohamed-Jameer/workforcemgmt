package com.railse.hiring.workforcemgmt.dto;


import com.railse.hiring.workforcemgmt.model.Priority;
import lombok.Data;

@Data
public class PriorityUpdateRequest {
    private Priority priority;
}

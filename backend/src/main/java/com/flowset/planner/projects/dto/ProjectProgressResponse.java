package com.flowset.planner.projects.dto;

import java.util.List;

public record ProjectProgressResponse(
        int percent,
        int completed,
        int total,
        List<ProjectProgressTaskResponse> tasks
) {
    public record ProjectProgressTaskResponse(String id, String label, boolean done) {
    }
}

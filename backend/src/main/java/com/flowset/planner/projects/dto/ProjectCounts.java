package com.flowset.planner.projects.dto;

public record ProjectCounts(
        long productCount,
        long vendorCount,
        long moodBoardItemCount,
        long noteCount,
        long fileCount
) {
    public static ProjectCounts empty() {
        return new ProjectCounts(0, 0, 0, 0, 0);
    }
}

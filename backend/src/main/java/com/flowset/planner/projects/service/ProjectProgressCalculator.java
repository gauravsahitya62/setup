package com.flowset.planner.projects.service;

import com.flowset.planner.projects.dto.ProjectCounts;
import com.flowset.planner.projects.dto.ProjectProgressResponse;
import com.flowset.planner.projects.dto.ProjectProgressResponse.ProjectProgressTaskResponse;
import com.flowset.planner.projects.entity.ProjectEntity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Planning progress is the share of completed setup tasks — never a hardcoded
 * percentage. Ten equally weighted tasks:
 * <ol>
 *   <li>Event date</li>
 *   <li>Guest count &gt; 0</li>
 *   <li>Venue name or city</li>
 *   <li>Description</li>
 *   <li>Cover image</li>
 *   <li>At least one product</li>
 *   <li>At least one saved vendor</li>
 *   <li>At least one mood board image</li>
 *   <li>At least one note</li>
 *   <li>At least one file</li>
 * </ol>
 * {@code percent = round(100 * completed / 10)}.
 */
@Component
public class ProjectProgressCalculator {

    public static final int TOTAL_TASKS = 10;

    public ProjectProgressResponse calculate(ProjectEntity project, ProjectCounts counts) {
        List<ProjectProgressTaskResponse> tasks = new ArrayList<>();
        tasks.add(task("eventDate", "Event date set", project.getEventDate() != null));
        tasks.add(task("guestCount", "Guest count set", project.getGuestCount() != null && project.getGuestCount() > 0));
        tasks.add(task(
                "venue",
                "Venue or city set",
                StringUtils.hasText(project.getVenueName()) || StringUtils.hasText(project.getCity())));
        tasks.add(task("description", "Description added", StringUtils.hasText(project.getDescription())));
        tasks.add(task("cover", "Cover image uploaded", StringUtils.hasText(project.getCoverImageUrl())));
        tasks.add(task("products", "At least one product", counts.productCount() > 0));
        tasks.add(task("vendors", "At least one vendor", counts.vendorCount() > 0));
        tasks.add(task("moodBoard", "Mood board started", counts.moodBoardItemCount() > 0));
        tasks.add(task("notes", "At least one note", counts.noteCount() > 0));
        tasks.add(task("files", "At least one file", counts.fileCount() > 0));

        int completed = (int) tasks.stream().filter(ProjectProgressTaskResponse::done).count();
        int percent = (int) Math.round(100.0 * completed / TOTAL_TASKS);
        return new ProjectProgressResponse(percent, completed, TOTAL_TASKS, List.copyOf(tasks));
    }

    private static ProjectProgressTaskResponse task(String id, String label, boolean done) {
        return new ProjectProgressTaskResponse(id, label, done);
    }
}

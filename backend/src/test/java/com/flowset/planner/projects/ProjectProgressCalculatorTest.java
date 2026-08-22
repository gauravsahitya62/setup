package com.flowset.planner.projects;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import com.flowset.planner.projects.dto.ProjectCounts;
import com.flowset.planner.projects.dto.ProjectProgressResponse;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.service.ProjectProgressCalculator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProjectProgressCalculatorTest {

    private final ProjectProgressCalculator calculator = new ProjectProgressCalculator();

    @Test
    void emptyDraftIsZeroPercent() {
        ProjectEntity project = base();
        ProjectProgressResponse progress = calculator.calculate(project, ProjectCounts.empty());
        assertThat(progress.percent()).isZero();
        assertThat(progress.completed()).isZero();
        assertThat(progress.total()).isEqualTo(10);
        assertThat(progress.tasks()).hasSize(10);
        assertThat(progress.tasks()).allMatch(task -> !task.done());
    }

    @Test
    void eachCompletedTaskAddsTenPercent() {
        ProjectEntity project = base();
        project.setEventDate(LocalDate.of(2026, 12, 12));
        project.setGuestCount(200);
        project.setCity("Jaipur");
        project.setDescription("Film shoot");
        project.setCoverImageUrl("https://cdn.example/cover.jpg");

        ProjectProgressResponse half = calculator.calculate(project, ProjectCounts.empty());
        assertThat(half.completed()).isEqualTo(5);
        assertThat(half.percent()).isEqualTo(50);

        ProjectProgressResponse full = calculator.calculate(
                project, new ProjectCounts(1, 1, 1, 1, 1));
        assertThat(full.completed()).isEqualTo(10);
        assertThat(full.percent()).isEqualTo(100);
        assertThat(full.tasks()).allMatch(ProjectProgressResponse.ProjectProgressTaskResponse::done);
    }

    @Test
    void venueNameAloneSatisfiesVenueTask() {
        ProjectEntity project = base();
        project.setVenueName("City Palace");
        ProjectProgressResponse progress = calculator.calculate(project, ProjectCounts.empty());
        assertThat(progress.tasks())
                .anyMatch(task -> "venue".equals(task.id()) && task.done());
    }

    private static ProjectEntity base() {
        ProjectEntity project = new ProjectEntity();
        project.setName("Sharma Wedding");
        project.setEventType(EventType.WEDDING);
        project.setStatus(ProjectStatus.DRAFT);
        return project;
    }
}

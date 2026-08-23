package com.flowset.planner.projects.mapper;

import com.flowset.common.ProductStatus;
import com.flowset.common.VendorStatus;
import com.flowset.planner.projects.dto.MoodBoardItemResponse;
import com.flowset.planner.projects.dto.MoodBoardResponse;
import com.flowset.planner.projects.dto.ProjectActivityResponse;
import com.flowset.planner.projects.dto.ProjectCounts;
import com.flowset.planner.projects.dto.ProjectFileResponse;
import com.flowset.planner.projects.dto.ProjectNoteResponse;
import com.flowset.planner.projects.dto.ProjectProductResponse;
import com.flowset.planner.projects.dto.ProjectProgressResponse;
import com.flowset.planner.projects.dto.ProjectResponse;
import com.flowset.planner.projects.dto.ProjectSummaryResponse;
import com.flowset.planner.projects.dto.ProjectVendorResponse;
import com.flowset.planner.projects.entity.ProjectActivityEntity;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectFileEntity;
import com.flowset.planner.projects.entity.ProjectMoodBoardEntity;
import com.flowset.planner.projects.entity.ProjectMoodBoardItemEntity;
import com.flowset.planner.projects.entity.ProjectNoteEntity;
import com.flowset.planner.projects.entity.ProjectProductEntity;
import com.flowset.planner.projects.entity.ProjectVendorEntity;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.vendor.entity.VendorEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(
            ProjectEntity project,
            ProjectCounts counts,
            ProjectProgressResponse progress,
            List<ProjectActivityResponse> recentActivity
    ) {
        return new ProjectResponse(
                project.getId(),
                project.getPlannerId(),
                project.getName(),
                project.getSlug(),
                project.getEventType(),
                project.getDescription(),
                project.getEventDate(),
                project.getStartTime(),
                project.getEndTime(),
                project.getVenueName(),
                project.getVenueAddress(),
                project.getCity(),
                project.getState(),
                project.getCountry(),
                project.getGuestCount(),
                project.getStatus(),
                project.getCoverImageUrl(),
                counts.productCount(),
                counts.vendorCount(),
                counts.moodBoardItemCount(),
                counts.noteCount(),
                counts.fileCount(),
                progress,
                recentActivity,
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public ProjectSummaryResponse toSummary(ProjectEntity project, ProjectCounts counts, int progressPercent) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getPlannerId(),
                project.getName(),
                project.getSlug(),
                project.getEventType(),
                project.getDescription(),
                project.getEventDate(),
                project.getVenueName(),
                project.getCity(),
                project.getGuestCount(),
                project.getStatus(),
                project.getCoverImageUrl(),
                counts.productCount(),
                counts.vendorCount(),
                progressPercent,
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    public ProjectProductResponse toProduct(ProjectProductEntity item) {
        ProductVariantEntity variant = item.getVariant();
        ProductEntity product = variant == null ? null : variant.getProduct();
        boolean catalogGone = variant == null
                || product == null
                || product.getDeletedAt() != null
                || product.getStatus() != ProductStatus.PUBLISHED;
        BigDecimal currentPrice = null;
        java.util.UUID productId = null;
        if (variant != null && product != null && product.getDeletedAt() == null) {
            productId = product.getId();
            currentPrice = variant.getPricePerDay() != null ? variant.getPricePerDay() : product.getPricePerDay();
        }
        VendorEntity vendor = item.getVendor();
        return new ProjectProductResponse(
                item.getId(),
                item.getProject().getId(),
                variant == null ? null : variant.getId(),
                productId,
                item.getProductSlug(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                vendor.getId(),
                vendor.getCompanyName(),
                vendor.getSlug(),
                vendor.isVerified(),
                item.getQuantity(),
                item.getNotes(),
                item.getUnitPricePerDay(),
                currentPrice,
                item.getCurrency(),
                item.getImageUrl(),
                !catalogGone,
                catalogGone,
                variant == null ? null : variant.getAvailableQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public ProjectVendorResponse toVendor(ProjectVendorEntity saved, long productsInProject, String city) {
        VendorEntity vendor = saved.getVendor();
        boolean unavailable = vendor.getDeletedAt() != null || vendor.getStatus() == VendorStatus.SUSPENDED;
        return new ProjectVendorResponse(
                saved.getId(),
                saved.getProject().getId(),
                vendor.getId(),
                vendor.getCompanyName(),
                vendor.getSlug(),
                vendor.getLogoUrl(),
                vendor.getRating(),
                vendor.getReviewCount(),
                vendor.isVerified(),
                city,
                saved.getNotes(),
                productsInProject,
                unavailable,
                saved.getCreatedAt());
    }

    public MoodBoardItemResponse toMoodItem(ProjectMoodBoardItemEntity item) {
        return new MoodBoardItemResponse(
                item.getId(),
                item.getMoodBoard().getId(),
                item.getImageUrl(),
                item.getSourceUrl(),
                item.getCaption(),
                item.getDisplayOrder(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public MoodBoardResponse toMoodBoard(ProjectMoodBoardEntity board, List<ProjectMoodBoardItemEntity> items) {
        return new MoodBoardResponse(
                board.getId(),
                board.getProject().getId(),
                items.stream().map(this::toMoodItem).toList(),
                board.getCreatedAt(),
                board.getUpdatedAt());
    }

    public ProjectNoteResponse toNote(ProjectNoteEntity note, UserEntity author) {
        String authorName = author == null
                ? "Planner"
                : (author.getFirstName() + " " + author.getLastName()).trim();
        return new ProjectNoteResponse(
                note.getId(),
                note.getProject().getId(),
                note.getTitle(),
                note.getContent(),
                note.isPinned(),
                note.getCreatedBy(),
                authorName,
                note.getCreatedAt(),
                note.getUpdatedAt());
    }

    public ProjectFileResponse toFile(ProjectFileEntity file, UserEntity uploader) {
        String name = uploader == null
                ? "Planner"
                : (uploader.getFirstName() + " " + uploader.getLastName()).trim();
        return new ProjectFileResponse(
                file.getId(),
                file.getProject().getId(),
                file.getFileUrl(),
                file.getFileName(),
                file.getFileType(),
                file.getFileSize(),
                file.getUploadedBy(),
                name,
                file.getCreatedAt());
    }

    public ProjectActivityResponse toActivity(ProjectActivityEntity activity) {
        return new ProjectActivityResponse(
                activity.getId(),
                activity.getProject().getId(),
                activity.getUserId(),
                activity.getActivityType(),
                activity.getDescription(),
                activity.getMetadata(),
                activity.getCreatedAt());
    }
}

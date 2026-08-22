package com.flowset.vendor.mapper;

import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.dto.PortfolioItemResponse;
import com.flowset.vendor.dto.ServiceAreaResponse;
import com.flowset.vendor.dto.SocialLinksResponse;
import com.flowset.vendor.dto.VendorResponse;
import com.flowset.vendor.entity.VendorDocumentEntity;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.entity.VendorPortfolioItemEntity;
import com.flowset.vendor.entity.VendorServiceAreaEntity;
import com.flowset.vendor.entity.VendorSocialLinksEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VendorMapper {

    public VendorResponse toResponse(
            VendorEntity vendor,
            List<VendorServiceAreaEntity> serviceAreas,
            List<VendorPortfolioItemEntity> portfolio,
            VendorSocialLinksEntity socialLinks,
            List<VendorDocumentEntity> documents
    ) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getOwnerUserId(),
                vendor.getCompanyName(),
                vendor.getSlug(),
                vendor.getDescription(),
                vendor.getShortDescription(),
                vendor.getGstNumber(),
                vendor.getBusinessType(),
                vendor.getEstablishedYear(),
                vendor.getWebsite(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getLogoUrl(),
                vendor.getBannerUrl(),
                vendor.getRating(),
                vendor.getReviewCount(),
                vendor.getCompletedEvents(),
                vendor.getResponseTimeMinutes(),
                vendor.isVerified(),
                vendor.getStatus(),
                vendor.getVerificationStatus(),
                vendor.isFeatured(),
                vendor.getVerificationReason(),
                vendor.getVerificationReviewedAt(),
                serviceAreas.stream().map(this::toServiceArea).toList(),
                portfolio.stream().map(this::toPortfolio).toList(),
                toSocial(socialLinks),
                documents.stream().map(this::toDocument).toList(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt());
    }

    public VendorResponse toSummary(VendorEntity vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getOwnerUserId(),
                vendor.getCompanyName(),
                vendor.getSlug(),
                vendor.getDescription(),
                vendor.getShortDescription(),
                vendor.getGstNumber(),
                vendor.getBusinessType(),
                vendor.getEstablishedYear(),
                vendor.getWebsite(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getLogoUrl(),
                vendor.getBannerUrl(),
                vendor.getRating(),
                vendor.getReviewCount(),
                vendor.getCompletedEvents(),
                vendor.getResponseTimeMinutes(),
                vendor.isVerified(),
                vendor.getStatus(),
                vendor.getVerificationStatus(),
                vendor.isFeatured(),
                null,
                vendor.getVerificationReviewedAt(),
                List.of(),
                List.of(),
                null,
                List.of(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt());
    }

    public ServiceAreaResponse toServiceArea(VendorServiceAreaEntity entity) {
        return new ServiceAreaResponse(entity.getId(), entity.getCountry(), entity.getState(), entity.getCity());
    }

    public PortfolioItemResponse toPortfolio(VendorPortfolioItemEntity entity) {
        return new PortfolioItemResponse(
                entity.getId(),
                entity.getImageUrl(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDisplayOrder());
    }

    public DocumentResponse toDocument(VendorDocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getFileUrl(),
                entity.getFileName(),
                entity.getDocumentType(),
                entity.getVerificationStatus(),
                entity.getReviewReason(),
                entity.getReviewedAt());
    }

    public SocialLinksResponse toSocial(VendorSocialLinksEntity entity) {
        if (entity == null) {
            return new SocialLinksResponse(null, null, null, null, null, null);
        }
        return new SocialLinksResponse(
                entity.getInstagram(),
                entity.getFacebook(),
                entity.getLinkedin(),
                entity.getYoutube(),
                entity.getPinterest(),
                entity.getWebsite());
    }
}

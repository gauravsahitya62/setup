package com.flowset.vendor.entity;

import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendors")
public class VendorEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, unique = true)
    private UUID ownerUserId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(length = 500)
    private String website;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "banner_url", length = 1000)
    private String bannerUrl;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "completed_events", nullable = false)
    private int completedEvents;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VendorVerificationStatus verificationStatus = VendorVerificationStatus.PENDING;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "verification_reason", length = 1000)
    private String verificationReason;

    @Column(name = "verification_reviewed_at")
    private Instant verificationReviewedAt;

    @Column(name = "verification_reviewed_by")
    private UUID verificationReviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "prior_verification_status", length = 30)
    private VendorVerificationStatus priorVerificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "prior_storefront_status", length = 30)
    private VendorStatus priorStorefrontStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VendorStatus status = VendorStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<VendorPortfolioItemEntity> portfolio = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VendorServiceAreaEntity> serviceAreas = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VendorDocumentEntity> documents = new ArrayList<>();

    @OneToOne(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VendorSocialLinksEntity socialLinks;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VendorBusinessHoursEntity> businessHours = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
        if (verificationStatus == null) {
            verificationStatus = VendorVerificationStatus.PENDING;
        }
        verified = verificationStatus == VendorVerificationStatus.VERIFIED;
        if (rating == null) {
            rating = BigDecimal.ZERO;
        }
        if (status == null) {
            status = VendorStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (verificationStatus != null) {
            verified = verificationStatus == VendorVerificationStatus.VERIFIED;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Integer getEstablishedYear() {
        return establishedYear;
    }

    public void setEstablishedYear(Integer establishedYear) {
        this.establishedYear = establishedYear;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getCompletedEvents() {
        return completedEvents;
    }

    public void setCompletedEvents(int completedEvents) {
        this.completedEvents = completedEvents;
    }

    public Integer getResponseTimeMinutes() {
        return responseTimeMinutes;
    }

    public void setResponseTimeMinutes(Integer responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public VendorVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VendorVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
        this.verified = verificationStatus == VendorVerificationStatus.VERIFIED;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public String getVerificationReason() {
        return verificationReason;
    }

    public void setVerificationReason(String verificationReason) {
        this.verificationReason = verificationReason;
    }

    public Instant getVerificationReviewedAt() {
        return verificationReviewedAt;
    }

    public void setVerificationReviewedAt(Instant verificationReviewedAt) {
        this.verificationReviewedAt = verificationReviewedAt;
    }

    public UUID getVerificationReviewedBy() {
        return verificationReviewedBy;
    }

    public void setVerificationReviewedBy(UUID verificationReviewedBy) {
        this.verificationReviewedBy = verificationReviewedBy;
    }

    public VendorVerificationStatus getPriorVerificationStatus() {
        return priorVerificationStatus;
    }

    public void setPriorVerificationStatus(VendorVerificationStatus priorVerificationStatus) {
        this.priorVerificationStatus = priorVerificationStatus;
    }

    public VendorStatus getPriorStorefrontStatus() {
        return priorStorefrontStatus;
    }

    public void setPriorStorefrontStatus(VendorStatus priorStorefrontStatus) {
        this.priorStorefrontStatus = priorStorefrontStatus;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public List<VendorPortfolioItemEntity> getPortfolio() {
        return portfolio;
    }

    public List<VendorServiceAreaEntity> getServiceAreas() {
        return serviceAreas;
    }

    public List<VendorDocumentEntity> getDocuments() {
        return documents;
    }

    public VendorSocialLinksEntity getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(VendorSocialLinksEntity socialLinks) {
        this.socialLinks = socialLinks;
    }

    public List<VendorBusinessHoursEntity> getBusinessHours() {
        return businessHours;
    }
}

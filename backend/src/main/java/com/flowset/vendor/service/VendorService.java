package com.flowset.vendor.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.DocumentVerificationStatus;
import com.flowset.common.RoleName;
import com.flowset.common.VendorDocumentType;
import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.events.VendorDocumentSubmittedEvent;
import com.flowset.events.VendorRegisteredEvent;
import com.flowset.events.VendorVerifiedEvent;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.SlugUtils;
import com.flowset.storage.StorageService;
import com.flowset.storage.StoredObject;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.repository.VendorProfileRepository;
import com.flowset.vendor.dto.CreateVendorRequest;
import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.dto.PortfolioItemResponse;
import com.flowset.vendor.dto.PortfolioUpdateRequest;
import com.flowset.vendor.dto.ServiceAreaRequest;
import com.flowset.vendor.dto.ServiceAreaResponse;
import com.flowset.vendor.dto.SocialLinksRequest;
import com.flowset.vendor.dto.SocialLinksResponse;
import com.flowset.vendor.dto.UpdateVendorRequest;
import com.flowset.vendor.dto.VendorResponse;
import com.flowset.vendor.entity.VendorDocumentEntity;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.entity.VendorPortfolioItemEntity;
import com.flowset.vendor.entity.VendorServiceAreaEntity;
import com.flowset.vendor.entity.VendorSocialLinksEntity;
import com.flowset.vendor.mapper.VendorMapper;
import com.flowset.vendor.repository.VendorDocumentRepository;
import com.flowset.vendor.repository.VendorPortfolioRepository;
import com.flowset.vendor.repository.VendorRepository;
import com.flowset.vendor.repository.VendorServiceAreaRepository;
import com.flowset.vendor.repository.VendorSocialLinksRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorServiceAreaRepository serviceAreaRepository;
    private final VendorPortfolioRepository portfolioRepository;
    private final VendorDocumentRepository documentRepository;
    private final VendorSocialLinksRepository socialLinksRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final VendorMapper vendorMapper;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public VendorService(
            VendorRepository vendorRepository,
            VendorServiceAreaRepository serviceAreaRepository,
            VendorPortfolioRepository portfolioRepository,
            VendorDocumentRepository documentRepository,
            VendorSocialLinksRepository socialLinksRepository,
            VendorProfileRepository vendorProfileRepository,
            VendorMapper vendorMapper,
            StorageService storageService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.vendorRepository = vendorRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.portfolioRepository = portfolioRepository;
        this.documentRepository = documentRepository;
        this.socialLinksRepository = socialLinksRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.vendorMapper = vendorMapper;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a DRAFT storefront for a newly registered vendor account.
     * Called from auth registration so every vendor starts with an empty profile.
     */
    @Transactional
    public VendorResponse createDraftForOwner(UUID ownerUserId, String businessName, String email) {
        var existing = vendorRepository.findActiveByOwnerUserId(ownerUserId);
        if (existing.isPresent()) {
            return toFullResponse(existing.get());
        }

        String companyName = StringUtils.hasText(businessName) ? businessName.trim() : "My Company";
        VendorEntity vendor = new VendorEntity();
        vendor.setOwnerUserId(ownerUserId);
        vendor.setCompanyName(companyName);
        vendor.setSlug(resolveSlug(null, companyName, null));
        vendor.setEmail(normalizeEmail(email));
        vendor.setStatus(VendorStatus.DRAFT);
        vendor.setVerificationStatus(VendorVerificationStatus.PENDING);
        vendor.setVerified(false);
        vendorRepository.save(vendor);
        auditLogService.log(
                "VENDOR_CREATED",
                ownerUserId.toString(),
                vendor.getId().toString(),
                vendor.getCompanyName());
        eventPublisher.publishEvent(new VendorRegisteredEvent(vendor.getId(), ownerUserId, vendor.getCompanyName()));
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse create(CreateVendorRequest request) {
        UserPrincipal principal = requirePrincipal();
        requireVendorOrAdmin(principal);

        if (principal.getRole() == RoleName.VENDOR
                && vendorRepository.findActiveByOwnerUserId(principal.getId()).isPresent()) {
            throw new ConflictException("Vendor storefront already exists for this account");
        }

        String companyName = request.companyName().trim();
        String slug = resolveSlug(request.slug(), companyName, null);

        VendorEntity vendor = new VendorEntity();
        vendor.setOwnerUserId(principal.getId());
        vendor.setCompanyName(companyName);
        vendor.setSlug(slug);
        vendor.setDescription(trimToNull(request.description()));
        vendor.setShortDescription(trimToNull(request.shortDescription()));
        vendor.setGstNumber(trimToNull(request.gstNumber()));
        vendor.setBusinessType(trimToNull(request.businessType()));
        vendor.setEstablishedYear(request.establishedYear());
        vendor.setWebsite(trimToNull(request.website()));
        vendor.setEmail(normalizeEmail(request.email()));
        vendor.setPhone(trimToNull(request.phone()));
        vendor.setStatus(request.status() == null ? VendorStatus.DRAFT : request.status());
        vendor.setVerificationStatus(VendorVerificationStatus.PENDING);
        vendor.setVerified(false);

        if (!StringUtils.hasText(vendor.getEmail())) {
            vendor.setEmail(principal.getUsername());
        }

        vendorProfileRepository.findByUserId(principal.getId()).ifPresent(profile -> {
            if (!StringUtils.hasText(profile.getBusinessName())) {
                profile.setBusinessName(companyName);
            }
        });

        VendorEntity saved = vendorRepository.save(vendor);
        auditLogService.log("VENDOR_CREATED", principal.getId().toString(), saved.getId().toString(), companyName);
        eventPublisher.publishEvent(new VendorRegisteredEvent(saved.getId(), saved.getOwnerUserId(), companyName));
        return toFullResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorResponse> search(
            String companyName,
            String city,
            String state,
            String businessType,
            Boolean verified,
            BigDecimal minRating,
            int page,
            int size
    ) {
        UserPrincipal principal = optionalPrincipal();
        boolean includeDrafts = principal != null && principal.getRole() == RoleName.ADMIN;

        Page<VendorEntity> result = vendorRepository.search(
                blankToNull(companyName),
                blankToNull(city),
                blankToNull(state),
                blankToNull(businessType),
                verified,
                minRating,
                includeDrafts,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("companyName")));

        return new PageResponse<>(
                result.getContent().stream().map(vendorMapper::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public VendorResponse getById(UUID id) {
        VendorEntity vendor = requireActive(id);
        assertCanView(vendor);
        return toFullResponse(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse getBySlug(String slug) {
        VendorEntity vendor = vendorRepository
                .findActiveBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Vendor not found"));
        assertCanView(vendor);
        return toFullResponse(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse getMine() {
        UserPrincipal principal = requirePrincipal();
        VendorEntity vendor = vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new NotFoundException("Vendor storefront not found"));
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse update(UUID id, UpdateVendorRequest request) {
        VendorEntity vendor = requireEditable(id);
        UserPrincipal principal = requirePrincipal();

        vendor.setCompanyName(request.companyName().trim());
        if (StringUtils.hasText(request.slug())) {
            vendor.setSlug(resolveSlug(request.slug(), request.companyName(), vendor.getId()));
        }
        vendor.setDescription(trimToNull(request.description()));
        vendor.setShortDescription(trimToNull(request.shortDescription()));
        vendor.setGstNumber(trimToNull(request.gstNumber()));
        vendor.setBusinessType(trimToNull(request.businessType()));
        vendor.setEstablishedYear(request.establishedYear());
        vendor.setWebsite(trimToNull(request.website()));
        vendor.setEmail(normalizeEmail(request.email()));
        vendor.setPhone(trimToNull(request.phone()));
        if (request.responseTimeMinutes() != null) {
            vendor.setResponseTimeMinutes(request.responseTimeMinutes());
        }
        if (request.completedEvents() != null) {
            vendor.setCompletedEvents(request.completedEvents());
        }
        if (request.status() != null) {
            if (principal.getRole() == RoleName.ADMIN) {
                vendor.setStatus(request.status());
            } else if (vendor.getStatus() == VendorStatus.SUSPENDED) {
                throw new ForbiddenException("A suspended storefront cannot change status");
            } else if (request.status() == VendorStatus.SUSPENDED) {
                throw new ForbiddenException("Vendors cannot suspend their own storefront");
            } else {
                vendor.setStatus(request.status());
            }
        }

        vendorRepository.save(vendor);
        auditLogService.log("PROFILE_UPDATED", principal.getId().toString(), id.toString(), vendor.getCompanyName());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse startReview(UUID id) {
        UserPrincipal principal = requireAdmin();
        VendorEntity vendor = requireActive(id);
        vendor.setVerificationStatus(VendorVerificationStatus.UNDER_REVIEW);
        vendor.setVerificationReviewedAt(Instant.now());
        vendor.setVerificationReviewedBy(principal.getId());
        syncProfileVerification(vendor);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_REVIEW_STARTED", principal.getId().toString(), id.toString(), vendor.getCompanyName());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse verify(UUID id) {
        UserPrincipal principal = requireAdmin();
        VendorEntity vendor = requireActive(id);
        boolean alreadyVerified = vendor.getVerificationStatus() == VendorVerificationStatus.VERIFIED;
        vendor.setVerificationStatus(VendorVerificationStatus.VERIFIED);
        vendor.setVerificationReason(null);
        vendor.setVerificationReviewedAt(Instant.now());
        vendor.setVerificationReviewedBy(principal.getId());
        syncProfileVerification(vendor);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_VERIFIED", principal.getId().toString(), id.toString(), vendor.getCompanyName());
        if (!alreadyVerified) {
            eventPublisher.publishEvent(new VendorVerifiedEvent(
                    vendor.getId(), vendor.getOwnerUserId(), vendor.getCompanyName()));
        }
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse reject(UUID id, String reason) {
        UserPrincipal principal = requireAdmin();
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("A reason is required to reject a vendor");
        }
        VendorEntity vendor = requireActive(id);
        vendor.setVerificationStatus(VendorVerificationStatus.REJECTED);
        vendor.setVerificationReason(reason.trim());
        vendor.setVerificationReviewedAt(Instant.now());
        vendor.setVerificationReviewedBy(principal.getId());
        syncProfileVerification(vendor);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_REJECTED", principal.getId().toString(), id.toString(), reason.trim());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse suspend(UUID id, String reason) {
        UserPrincipal principal = requireAdmin();
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("A reason is required to suspend a vendor");
        }
        VendorEntity vendor = requireActive(id);
        if (vendor.getVerificationStatus() != VendorVerificationStatus.SUSPENDED) {
            vendor.setPriorVerificationStatus(vendor.getVerificationStatus());
        }
        if (vendor.getStatus() != VendorStatus.SUSPENDED) {
            vendor.setPriorStorefrontStatus(vendor.getStatus());
        }
        vendor.setVerificationStatus(VendorVerificationStatus.SUSPENDED);
        vendor.setStatus(VendorStatus.SUSPENDED);
        vendor.setVerificationReason(reason.trim());
        vendor.setVerificationReviewedAt(Instant.now());
        vendor.setVerificationReviewedBy(principal.getId());
        syncProfileVerification(vendor);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_SUSPENDED", principal.getId().toString(), id.toString(), reason.trim());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse reactivate(UUID id) {
        UserPrincipal principal = requireAdmin();
        VendorEntity vendor = requireActive(id);
        VendorVerificationStatus restoredVerification = vendor.getPriorVerificationStatus() == null
                ? VendorVerificationStatus.PENDING
                : vendor.getPriorVerificationStatus();
        VendorStatus restoredStorefront = vendor.getPriorStorefrontStatus() == null
                ? VendorStatus.DRAFT
                : vendor.getPriorStorefrontStatus();
        vendor.setVerificationStatus(restoredVerification);
        vendor.setStatus(restoredStorefront == VendorStatus.SUSPENDED ? VendorStatus.DRAFT : restoredStorefront);
        vendor.setPriorVerificationStatus(null);
        vendor.setPriorStorefrontStatus(null);
        vendor.setVerificationReason(null);
        vendor.setVerificationReviewedAt(Instant.now());
        vendor.setVerificationReviewedBy(principal.getId());
        syncProfileVerification(vendor);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_REACTIVATED", principal.getId().toString(), id.toString(), vendor.getCompanyName());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse setFeatured(UUID id, boolean featured) {
        UserPrincipal principal = requireAdmin();
        VendorEntity vendor = requireActive(id);
        vendor.setFeatured(featured);
        vendorRepository.save(vendor);
        auditLogService.log(
                featured ? "VENDOR_FEATURED" : "VENDOR_UNFEATURED",
                principal.getId().toString(),
                id.toString(),
                vendor.getCompanyName());
        return toFullResponse(vendor);
    }

    @Transactional
    public DocumentResponse moderateDocument(UUID vendorId, UUID documentId, DocumentVerificationStatus status, String reason) {
        UserPrincipal principal = requireAdmin();
        requireActive(vendorId);
        if (status == DocumentVerificationStatus.REJECTED
                || status == DocumentVerificationStatus.RESUBMISSION_REQUESTED) {
            if (!StringUtils.hasText(reason)) {
                throw new BadRequestException("A reason is required");
            }
        }
        VendorDocumentEntity doc = documentRepository
                .findByIdAndVendorId(documentId, vendorId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        doc.setVerificationStatus(status);
        doc.setReviewReason(trimToNull(reason));
        doc.setReviewedAt(Instant.now());
        doc.setReviewedBy(principal.getId());
        documentRepository.save(doc);
        String action = switch (status) {
            case VERIFIED -> "DOCUMENT_APPROVED";
            case REJECTED -> "DOCUMENT_REJECTED";
            case RESUBMISSION_REQUESTED -> "DOCUMENT_RESUBMISSION_REQUESTED";
            default -> "DOCUMENT_REVIEWED";
        };
        auditLogService.log(action, principal.getId().toString(), documentId.toString(), reason);
        return vendorMapper.toDocument(doc);
    }

    @Transactional
    public void softDelete(UUID id) {
        VendorEntity vendor = requireEditable(id);
        UserPrincipal principal = requirePrincipal();
        vendor.setDeletedAt(Instant.now());
        vendor.setStatus(VendorStatus.SUSPENDED);
        vendorRepository.save(vendor);
        auditLogService.log("VENDOR_DELETED", principal.getId().toString(), id.toString(), vendor.getSlug());
    }

    @Transactional
    public VendorResponse uploadLogo(UUID id, MultipartFile file) {
        VendorEntity vendor = requireEditable(id);
        UserPrincipal principal = requirePrincipal();
        if (StringUtils.hasText(vendor.getLogoUrl())) {
            storageService.delete(extractRelative(vendor.getLogoUrl()));
        }
        StoredObject stored = storageService.store(file, "vendors/" + id + "/logo");
        vendor.setLogoUrl(stored.publicUrl());
        vendorRepository.save(vendor);
        auditLogService.log("LOGO_CHANGED", principal.getId().toString(), id.toString(), stored.relativePath());
        return toFullResponse(vendor);
    }

    @Transactional
    public VendorResponse uploadBanner(UUID id, MultipartFile file) {
        VendorEntity vendor = requireEditable(id);
        UserPrincipal principal = requirePrincipal();
        if (StringUtils.hasText(vendor.getBannerUrl())) {
            storageService.delete(extractRelative(vendor.getBannerUrl()));
        }
        StoredObject stored = storageService.store(file, "vendors/" + id + "/banner");
        vendor.setBannerUrl(stored.publicUrl());
        vendorRepository.save(vendor);
        auditLogService.log("BANNER_CHANGED", principal.getId().toString(), id.toString(), stored.relativePath());
        return toFullResponse(vendor);
    }

    @Transactional
    public PortfolioItemResponse addPortfolio(UUID id, MultipartFile file, String title, String description) {
        VendorEntity vendor = requireEditable(id);
        UserPrincipal principal = requirePrincipal();
        StoredObject stored = storageService.store(file, "vendors/" + id + "/portfolio");
        VendorPortfolioItemEntity item = new VendorPortfolioItemEntity();
        item.setVendor(vendor);
        item.setImageUrl(stored.publicUrl());
        item.setTitle(trimToNull(title));
        item.setDescription(trimToNull(description));
        item.setDisplayOrder((int) portfolioRepository.countByVendorId(id));
        VendorPortfolioItemEntity saved = portfolioRepository.save(item);
        auditLogService.log("PORTFOLIO_UPDATED", principal.getId().toString(), id.toString(), "added:" + saved.getId());
        return vendorMapper.toPortfolio(saved);
    }

    @Transactional
    public PortfolioItemResponse updatePortfolio(UUID vendorId, UUID itemId, PortfolioUpdateRequest request) {
        requireEditable(vendorId);
        UserPrincipal principal = requirePrincipal();
        VendorPortfolioItemEntity item = portfolioRepository
                .findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new NotFoundException("Portfolio item not found"));
        if (request.title() != null) {
            item.setTitle(trimToNull(request.title()));
        }
        if (request.description() != null) {
            item.setDescription(trimToNull(request.description()));
        }
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }
        VendorPortfolioItemEntity saved = portfolioRepository.save(item);
        auditLogService.log("PORTFOLIO_UPDATED", principal.getId().toString(), vendorId.toString(), "updated:" + itemId);
        return vendorMapper.toPortfolio(saved);
    }

    @Transactional
    public void deletePortfolio(UUID vendorId, UUID itemId) {
        requireEditable(vendorId);
        UserPrincipal principal = requirePrincipal();
        VendorPortfolioItemEntity item = portfolioRepository
                .findByIdAndVendorId(itemId, vendorId)
                .orElseThrow(() -> new NotFoundException("Portfolio item not found"));
        storageService.delete(extractRelative(item.getImageUrl()));
        portfolioRepository.delete(item);
        auditLogService.log("PORTFOLIO_UPDATED", principal.getId().toString(), vendorId.toString(), "deleted:" + itemId);
    }

    @Transactional(readOnly = true)
    public java.util.List<ServiceAreaResponse> listServiceAreas(UUID vendorId) {
        VendorEntity vendor = requireActive(vendorId);
        assertCanView(vendor);
        return serviceAreaRepository.findByVendorIdOrderByCityAsc(vendorId).stream()
                .map(vendorMapper::toServiceArea)
                .toList();
    }

    @Transactional
    public ServiceAreaResponse addServiceArea(UUID vendorId, ServiceAreaRequest request) {
        VendorEntity vendor = requireEditable(vendorId);
        String country = request.country().trim();
        String state = request.state().trim();
        String city = request.city().trim();
        if (serviceAreaRepository.existsByVendorIdAndCountryIgnoreCaseAndStateIgnoreCaseAndCityIgnoreCase(
                vendorId, country, state, city)) {
            throw new ConflictException("Service area already exists");
        }
        VendorServiceAreaEntity area = new VendorServiceAreaEntity();
        area.setVendor(vendor);
        area.setCountry(country);
        area.setState(state);
        area.setCity(city);
        return vendorMapper.toServiceArea(serviceAreaRepository.save(area));
    }

    @Transactional
    public ServiceAreaResponse updateServiceArea(UUID vendorId, UUID areaId, ServiceAreaRequest request) {
        requireEditable(vendorId);
        VendorServiceAreaEntity area = serviceAreaRepository
                .findByIdAndVendorId(areaId, vendorId)
                .orElseThrow(() -> new NotFoundException("Service area not found"));
        area.setCountry(request.country().trim());
        area.setState(request.state().trim());
        area.setCity(request.city().trim());
        return vendorMapper.toServiceArea(serviceAreaRepository.save(area));
    }

    @Transactional
    public void deleteServiceArea(UUID vendorId, UUID areaId) {
        requireEditable(vendorId);
        VendorServiceAreaEntity area = serviceAreaRepository
                .findByIdAndVendorId(areaId, vendorId)
                .orElseThrow(() -> new NotFoundException("Service area not found"));
        serviceAreaRepository.delete(area);
    }

    @Transactional(readOnly = true)
    public SocialLinksResponse getSocialLinks(UUID vendorId) {
        VendorEntity vendor = requireActive(vendorId);
        assertCanView(vendor);
        return socialLinksRepository
                .findByVendorId(vendorId)
                .map(vendorMapper::toSocial)
                .orElse(new SocialLinksResponse(null, null, null, null, null, null));
    }

    @Transactional
    public SocialLinksResponse upsertSocialLinks(UUID vendorId, SocialLinksRequest request) {
        VendorEntity vendor = requireEditable(vendorId);
        VendorSocialLinksEntity links = socialLinksRepository
                .findByVendorId(vendorId)
                .orElseGet(() -> {
                    VendorSocialLinksEntity created = new VendorSocialLinksEntity();
                    created.setVendor(vendor);
                    return created;
                });
        links.setInstagram(trimToNull(request.instagram()));
        links.setFacebook(trimToNull(request.facebook()));
        links.setLinkedin(trimToNull(request.linkedin()));
        links.setYoutube(trimToNull(request.youtube()));
        links.setPinterest(trimToNull(request.pinterest()));
        links.setWebsite(trimToNull(request.website()));
        return vendorMapper.toSocial(socialLinksRepository.save(links));
    }

    @Transactional(readOnly = true)
    public java.util.List<DocumentResponse> listDocuments(UUID vendorId) {
        requireDocumentAccess(vendorId);
        return documentRepository.findByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                .map(vendorMapper::toDocument)
                .toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(UUID vendorId, MultipartFile file, VendorDocumentType type) {
        VendorEntity vendor = requireEditable(vendorId);
        UserPrincipal principal = requirePrincipal();
        if (type == null) {
            throw new BadRequestException("Document type is required");
        }
        StoredObject stored = storageService.store(file, "vendors/" + vendorId + "/documents");
        VendorDocumentEntity doc = new VendorDocumentEntity();
        doc.setVendor(vendor);
        doc.setFileUrl(stored.publicUrl());
        doc.setFileName(stored.originalFilename());
        doc.setDocumentType(type);
        doc.setVerificationStatus(DocumentVerificationStatus.PENDING);
        VendorDocumentEntity saved = documentRepository.save(doc);
        auditLogService.log(
                "DOCUMENTS_UPLOADED",
                principal.getId().toString(),
                vendorId.toString(),
                type.name() + ":" + saved.getId());
        eventPublisher.publishEvent(new VendorDocumentSubmittedEvent(
                vendor.getId(), vendor.getOwnerUserId(), vendor.getCompanyName(), type));
        return vendorMapper.toDocument(saved);
    }

    @Transactional
    public void deleteDocument(UUID vendorId, UUID documentId) {
        requireEditable(vendorId);
        UserPrincipal principal = requirePrincipal();
        VendorDocumentEntity doc = documentRepository
                .findByIdAndVendorId(documentId, vendorId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        storageService.delete(extractRelative(doc.getFileUrl()));
        documentRepository.delete(doc);
        auditLogService.log("DOCUMENTS_UPLOADED", principal.getId().toString(), vendorId.toString(), "deleted:" + documentId);
    }

    private VendorResponse toFullResponse(VendorEntity vendor) {
        UUID id = vendor.getId();
        boolean includeDocuments = canSeePrivateDocuments(vendor);
        return vendorMapper.toResponse(
                vendor,
                serviceAreaRepository.findByVendorIdOrderByCityAsc(id),
                portfolioRepository.findByVendorIdOrderByDisplayOrderAsc(id),
                socialLinksRepository.findByVendorId(id).orElse(null),
                includeDocuments
                        ? documentRepository.findByVendorIdOrderByCreatedAtDesc(id)
                        : java.util.List.of());
    }

    private boolean canSeePrivateDocuments(VendorEntity vendor) {
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            return false;
        }
        if (principal.getRole() == RoleName.ADMIN) {
            return true;
        }
        return principal.getRole() == RoleName.VENDOR && vendor.getOwnerUserId().equals(principal.getId());
    }

    private void requireDocumentAccess(UUID vendorId) {
        VendorEntity vendor = requireActive(vendorId);
        if (!canSeePrivateDocuments(vendor)) {
            throw new NotFoundException("Document not found");
        }
    }

    private void syncProfileVerification(VendorEntity vendor) {
        vendorProfileRepository.findByUserId(vendor.getOwnerUserId()).ifPresent(profile -> {
            profile.setVerificationStatus(vendor.getVerificationStatus());
            vendorProfileRepository.save(profile);
        });
    }

    private UserPrincipal requireAdmin() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Only admins can perform this action");
        }
        return principal;
    }

    private VendorEntity requireActive(UUID id) {
        return vendorRepository
                .findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Vendor not found"));
    }

    private VendorEntity requireEditable(UUID id) {
        VendorEntity vendor = requireActive(id);
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return vendor;
        }
        if (principal.getRole() != RoleName.VENDOR || !vendor.getOwnerUserId().equals(principal.getId())) {
            throw new ForbiddenException("Only the vendor owner can modify this storefront");
        }
        return vendor;
    }

    private void assertCanView(VendorEntity vendor) {
        if (vendor.getStatus() == VendorStatus.PUBLISHED) {
            return;
        }
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            throw new NotFoundException("Vendor not found");
        }
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.VENDOR && vendor.getOwnerUserId().equals(principal.getId())) {
            return;
        }
        throw new NotFoundException("Vendor not found");
    }

    private String resolveSlug(String requested, String companyName, UUID currentId) {
        String base = StringUtils.hasText(requested) ? requested.trim().toLowerCase() : companyName;
        return SlugUtils.uniqueSlug(base, candidate -> {
            if (currentId == null) {
                return vendorRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(candidate);
            }
            return vendorRepository.existsBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(candidate, currentId);
        });
    }

    private UserPrincipal requirePrincipal() {
        UserPrincipal principal = optionalPrincipal();
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private UserPrincipal optionalPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }

    private void requireVendorOrAdmin(UserPrincipal principal) {
        if (principal.getRole() != RoleName.VENDOR && principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Only vendors can create a storefront");
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return trimToNull(value);
    }

    private static String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private String extractRelative(String publicUrl) {
        if (!StringUtils.hasText(publicUrl)) {
            return null;
        }
        String marker = "/api/v1/files/";
        int idx = publicUrl.indexOf(marker);
        if (idx >= 0) {
            return publicUrl.substring(idx + marker.length());
        }
        return publicUrl;
    }
}

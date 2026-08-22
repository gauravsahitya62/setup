package com.flowset.admin.service;

import com.flowset.admin.dto.AdminVendorDetailResponse;
import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.marketplace.product.dto.ProductSummaryResponse;
import com.flowset.marketplace.product.service.ProductService;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.planner.booking.service.BookingRequestService;
import com.flowset.reviews.dto.ReviewResponse;
import com.flowset.reviews.service.ReviewService;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.dto.VendorResponse;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.mapper.VendorMapper;
import com.flowset.vendor.repository.VendorRepository;
import com.flowset.vendor.service.VendorService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminVendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;
    private final VendorService vendorService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final BookingRequestService bookingRequestService;
    private final UserRepository userRepository;

    public AdminVendorService(
            VendorRepository vendorRepository,
            VendorMapper vendorMapper,
            VendorService vendorService,
            ProductService productService,
            ReviewService reviewService,
            BookingRequestService bookingRequestService,
            UserRepository userRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.vendorMapper = vendorMapper;
        this.vendorService = vendorService;
        this.productService = productService;
        this.reviewService = reviewService;
        this.bookingRequestService = bookingRequestService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorResponse> list(
            String q,
            VendorVerificationStatus verificationStatus,
            VendorStatus status,
            Boolean featured,
            int page,
            int size
    ) {
        Page<VendorEntity> result = vendorRepository.adminSearch(
                StringUtils.hasText(q) ? q.trim() : null,
                verificationStatus,
                status,
                featured,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(vendorMapper::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminVendorDetailResponse get(UUID id) {
        VendorResponse vendor = vendorService.getById(id);
        var owner = userRepository.findById(vendor.ownerUserId()).orElse(null);
        List<DocumentResponse> documents = vendorService.listDocuments(id);
        List<ProductSummaryResponse> products = productService.listByVendor(id, 0, 50).content();
        List<ReviewResponse> reviews = reviewService.adminList(null, id, null, 0, 20).content();
        List<BookingRequestSummaryResponse> bookings = bookingRequestService.list(null, null, null, id, 0, 20).content();
        return new AdminVendorDetailResponse(
                vendor,
                owner == null ? null : (owner.getFirstName() + " " + owner.getLastName()).trim(),
                owner == null ? null : owner.getEmail(),
                owner == null ? null : owner.getPhone(),
                documents,
                products,
                reviews,
                bookings);
    }
}

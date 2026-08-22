package com.flowset.admin.service;

import com.flowset.admin.dto.AdminReportsResponse;
import com.flowset.admin.dto.ProductPerformanceRow;
import com.flowset.admin.dto.VendorPerformanceRow;
import com.flowset.admin.support.AdminDateRange;
import com.flowset.common.BookingRequestStatus;
import com.flowset.common.OrderStatus;
import com.flowset.common.ReviewReportStatus;
import com.flowset.marketplace.favorite.repository.FavoriteProductRepository;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.marketplace.product.repository.ProductVariantRepository;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.entity.OrderItemEntity;
import com.flowset.orders.repository.OrderItemRepository;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.repository.BookingRequestItemRepository;
import com.flowset.planner.booking.repository.BookingRequestRepository;
import com.flowset.planner.projects.repository.ProjectProductRepository;
import com.flowset.reviews.repository.ReviewReportRepository;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    public static final String FINANCIAL_LABEL = "Estimated Rental Value (gross booking value; FlowSet does not collect payments in MVP)";

    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRequestItemRepository bookingRequestItemRepository;
    private final OrderRepository orderRepository;
    private final ProjectProductRepository projectProductRepository;
    private final FavoriteProductRepository favoriteProductRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminReportService(
            VendorRepository vendorRepository,
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            BookingRequestRepository bookingRequestRepository,
            BookingRequestItemRepository bookingRequestItemRepository,
            OrderRepository orderRepository,
            ProjectProductRepository projectProductRepository,
            FavoriteProductRepository favoriteProductRepository,
            ReviewRepository reviewRepository,
            ReviewReportRepository reviewReportRepository,
            UserRepository userRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.bookingRequestItemRepository = bookingRequestItemRepository;
        this.orderRepository = orderRepository;
        this.projectProductRepository = projectProductRepository;
        this.favoriteProductRepository = favoriteProductRepository;
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public AdminReportsResponse reports(String range, Instant customFrom, Instant customTo) {
        Instant from = AdminDateRange.from(range, customFrom, customTo);
        List<VendorEntity> vendors = vendorRepository.findAll(PageRequest.of(0, 100, Sort.by("companyName"))).getContent();
        List<ProductEntity> products = productRepository.findAll(PageRequest.of(0, 100, Sort.by("name"))).getContent();

        List<VendorPerformanceRow> vendorRows = new ArrayList<>();
        for (VendorEntity vendor : vendors) {
            if (vendor.isDeleted()) {
                continue;
            }
            vendorRows.add(vendorRow(vendor));
        }
        List<ProductPerformanceRow> productRows = new ArrayList<>();
        for (ProductEntity product : products) {
            if (product.isDeleted()) {
                continue;
            }
            productRows.add(productRow(product));
        }

        return new AdminReportsResponse(
                vendorRows,
                productRows,
                bookingRequestRepository.count(),
                orderRepository.countByStatus(OrderStatus.CONFIRMED),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                orderRepository.countByStatus(OrderStatus.CANCELLED),
                nullToZero(orderRepository.sumEstimatedRentalValue()),
                reviewRepository.count(),
                reviewReportRepository.countByStatus(ReviewReportStatus.OPEN),
                userRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(from),
                FINANCIAL_LABEL);
    }

    private VendorPerformanceRow vendorRow(VendorEntity vendor) {
        UUID id = vendor.getId();
        long requests = bookingRequestRepository.countByVendorIdAndStatusIn(
                id, EnumSet.allOf(BookingRequestStatus.class));
        long accepted = bookingRequestRepository.countByVendorIdAndStatusIn(
                id, EnumSet.of(BookingRequestStatus.ACCEPTED, BookingRequestStatus.CONFIRMED, BookingRequestStatus.COMPLETED));
        long rejected = bookingRequestRepository.countByVendorIdAndStatus(id, BookingRequestStatus.REJECTED);
        long cancelled = bookingRequestRepository.countByVendorIdAndStatus(id, BookingRequestStatus.CANCELLED);
        long completed = orderRepository.countByVendorIdAndStatus(id, OrderStatus.COMPLETED);
        List<BookingRequestEntity> bookings = bookingRequestRepository.findByVendorIdOrderByCreatedAtDesc(id);
        Double avgHours = averageResponseHours(bookings);
        return new VendorPerformanceRow(
                id,
                vendor.getCompanyName(),
                requests,
                rate(accepted, requests),
                rate(rejected, requests),
                completed,
                rate(cancelled, requests),
                vendor.getRating(),
                avgHours,
                nullToZero(orderRepository.sumEstimatedRentalValueByVendor(id)));
    }

    private ProductPerformanceRow productRow(ProductEntity product) {
        List<UUID> variantIds = variantRepository.findByProductIdOrderByCreatedAtAsc(product.getId()).stream()
                .map(ProductVariantEntity::getId)
                .toList();
        long bookingRequests = 0;
        if (!variantIds.isEmpty()) {
            bookingRequests = distinctRequestsForVariants(variantIds);
        }
        long confirmed = 0;
        long completed = 0;
        BigDecimal value = BigDecimal.ZERO;
        for (OrderEntity order : ordersForVariants(variantIds)) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            if (order.getStatus() == OrderStatus.COMPLETED) {
                completed++;
            }
            if (order.getStatus() != OrderStatus.PENDING) {
                confirmed++;
            }
            if (order.getTotalAmount() != null) {
                value = value.add(order.getTotalAmount());
            }
        }
        return new ProductPerformanceRow(
                product.getId(),
                product.getName(),
                product.getViewCount(),
                projectProductRepository.countByVariant_Product_Id(product.getId()),
                bookingRequests,
                confirmed,
                completed,
                value,
                favoriteProductRepository.countByProductId(product.getId()));
    }

    private long distinctRequestsForVariants(List<UUID> variantIds) {
        return bookingRequestItemRepository.findAll().stream()
                .filter(i -> i.getProductVariantId() != null && variantIds.contains(i.getProductVariantId()))
                .map(i -> i.getBookingRequest().getId())
                .distinct()
                .count();
    }

    private List<OrderEntity> ordersForVariants(List<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return List.of();
        }
        return orderItemRepository.findByProductVariantIdIn(variantIds).stream()
                .map(OrderItemEntity::getOrder)
                .distinct()
                .toList();
    }

    private static Double averageResponseHours(List<BookingRequestEntity> bookings) {
        List<Duration> durations = bookings.stream()
                .filter(b -> b.getRespondedAt() != null && b.getCreatedAt() != null)
                .map(b -> Duration.between(b.getCreatedAt(), b.getRespondedAt()))
                .toList();
        if (durations.isEmpty()) {
            return null;
        }
        double hours = durations.stream().mapToLong(Duration::toMinutes).average().orElse(0) / 60.0;
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double rate(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

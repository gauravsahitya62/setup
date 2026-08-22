package com.flowset.admin.service;

import com.flowset.admin.dto.AdminDashboardResponse;
import com.flowset.admin.dto.AuditLogResponse;
import com.flowset.admin.dto.TrendPoint;
import com.flowset.admin.support.AdminDateRange;
import com.flowset.audit.entity.AuditLogEntity;
import com.flowset.audit.repository.AuditLogRepository;
import com.flowset.common.OrderStatus;
import com.flowset.common.ProductStatus;
import com.flowset.common.ProjectStatus;
import com.flowset.common.ReviewReportStatus;
import com.flowset.common.RoleName;
import com.flowset.common.SupportTicketStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.repository.ProductRepository;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.repository.BookingRequestRepository;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.reviews.repository.ReviewReportRepository;
import com.flowset.support.repository.SupportTicketRepository;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;
    private final BookingRequestRepository bookingRequestRepository;
    private final OrderRepository orderRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            VendorRepository vendorRepository,
            ProductRepository productRepository,
            ProjectRepository projectRepository,
            BookingRequestRepository bookingRequestRepository,
            OrderRepository orderRepository,
            ReviewReportRepository reviewReportRepository,
            SupportTicketRepository supportTicketRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.productRepository = productRepository;
        this.projectRepository = projectRepository;
        this.bookingRequestRepository = bookingRequestRepository;
        this.orderRepository = orderRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(String range, Instant customFrom, Instant customTo) {
        Instant from = AdminDateRange.from(range, customFrom, customTo);
        Instant to = AdminDateRange.to(range, customTo);

        List<AuditLogEntity> recent = auditLogRepository
                .findAll(PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        return new AdminDashboardResponse(
                userRepository.countByRoleAndDeletedAtIsNull(RoleName.PLANNER),
                vendorRepository.countByDeletedAtIsNull(),
                vendorRepository.countByVerificationStatusAndDeletedAtIsNull(VendorVerificationStatus.VERIFIED),
                vendorRepository.countByVerificationStatusAndDeletedAtIsNull(VendorVerificationStatus.PENDING)
                        + vendorRepository.countByVerificationStatusAndDeletedAtIsNull(
                                VendorVerificationStatus.UNDER_REVIEW),
                productRepository.countByDeletedAtIsNull(),
                productRepository.countByStatusAndDeletedAtIsNull(ProductStatus.PUBLISHED),
                productRepository.countByStatusAndDeletedAtIsNull(ProductStatus.PENDING_REVIEW)
                        + productRepository.countByStatusAndDeletedAtIsNull(ProductStatus.REJECTED),
                projectRepository.countByStatusInAndDeletedAtIsNull(
                        EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.PLANNING)),
                bookingRequestRepository.count(),
                orderRepository.countByStatus(OrderStatus.CONFIRMED),
                orderRepository.countByStatus(OrderStatus.COMPLETED),
                reviewReportRepository.countByStatus(ReviewReportStatus.OPEN),
                supportTicketRepository.countByStatusIn(
                        EnumSet.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS,
                                SupportTicketStatus.WAITING_FOR_USER)),
                recent.stream().map(this::toAudit).toList(),
                bucketUsers(from, to),
                bucketVendors(from, to),
                bucketProducts(from, to),
                bucketBookings(from, to),
                bucketOrders(from, to, OrderStatus.CONFIRMED),
                bucketOrders(from, to, OrderStatus.COMPLETED));
    }

    private List<TrendPoint> bucketUsers(Instant from, Instant to) {
        return bucket(userRepository.findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(from),
                UserEntity::getCreatedAt, from, to);
    }

    private List<TrendPoint> bucketVendors(Instant from, Instant to) {
        return bucket(vendorRepository.findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(from),
                VendorEntity::getCreatedAt, from, to);
    }

    private List<TrendPoint> bucketProducts(Instant from, Instant to) {
        return bucket(productRepository.findByCreatedAtGreaterThanEqualAndDeletedAtIsNullOrderByCreatedAtAsc(from),
                ProductEntity::getCreatedAt, from, to);
    }

    private List<TrendPoint> bucketBookings(Instant from, Instant to) {
        return bucket(bookingRequestRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(from),
                BookingRequestEntity::getCreatedAt, from, to);
    }

    private List<TrendPoint> bucketOrders(Instant from, Instant to, OrderStatus status) {
        List<OrderEntity> rows = orderRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(from).stream()
                .filter(o -> o.getStatus() == status)
                .filter(o -> !o.getCreatedAt().isAfter(to))
                .toList();
        return bucket(rows, OrderEntity::getCreatedAt, from, to);
    }

    private <T> List<TrendPoint> bucket(List<T> rows, Function<T, Instant> createdAt, Instant from, Instant to) {
        Map<LocalDate, Long> counts = new TreeMap<>();
        LocalDate start = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = to.atZone(ZoneOffset.UTC).toLocalDate();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            counts.put(d, 0L);
        }
        for (T row : rows) {
            Instant at = createdAt.apply(row);
            if (at == null || at.isBefore(from) || at.isAfter(to)) {
                continue;
            }
            LocalDate day = at.atZone(ZoneOffset.UTC).toLocalDate();
            counts.merge(day, 1L, Long::sum);
        }
        List<TrendPoint> points = new ArrayList<>();
        counts.forEach((date, count) -> points.add(new TrendPoint(date, count)));
        return points;
    }

    private AuditLogResponse toAudit(AuditLogEntity entity) {
        String name = entity.getActorUserId() == null
                ? "System"
                : userRepository.findById(entity.getActorUserId())
                        .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                        .orElse("User");
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorUserId(),
                name,
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }
}

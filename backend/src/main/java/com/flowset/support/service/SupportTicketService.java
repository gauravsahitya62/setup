package com.flowset.support.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.AuditResourceType;
import com.flowset.common.RoleName;
import com.flowset.common.SupportTicketPriority;
import com.flowset.common.SupportTicketStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import com.flowset.support.dto.AdminSupportTicketUpdateRequest;
import com.flowset.support.dto.CreateSupportTicketRequest;
import com.flowset.support.dto.SupportCategoryResponse;
import com.flowset.support.dto.SupportMessageRequest;
import com.flowset.support.dto.SupportMessageResponse;
import com.flowset.support.dto.SupportTicketResponse;
import com.flowset.support.dto.SupportTicketSummaryResponse;
import com.flowset.support.entity.SupportCategoryEntity;
import com.flowset.support.entity.SupportMessageEntity;
import com.flowset.support.entity.SupportTicketEntity;
import com.flowset.support.repository.SupportCategoryRepository;
import com.flowset.support.repository.SupportMessageRepository;
import com.flowset.support.repository.SupportTicketRepository;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupportTicketService {

    private static final EnumSet<SupportTicketStatus> CLOSED_STATES =
            EnumSet.of(SupportTicketStatus.RESOLVED, SupportTicketStatus.CLOSED);

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public SupportTicketService(
            SupportTicketRepository ticketRepository,
            SupportMessageRepository messageRepository,
            SupportCategoryRepository categoryRepository,
            UserRepository userRepository,
            AuditLogService auditLogService
    ) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SupportCategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new SupportCategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getDisplayOrder()))
                .toList();
    }

    @Transactional
    public SupportTicketResponse create(CreateSupportTicketRequest request) {
        UserPrincipal principal = requirePrincipal();
        SupportCategoryEntity category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Support category not found"));
        SupportTicketEntity ticket = new SupportTicketEntity();
        ticket.setCreatedBy(principal.getId());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(category);
        ticket.setPriority(request.priority() == null ? SupportTicketPriority.MEDIUM : request.priority());
        ticket.setStatus(SupportTicketStatus.OPEN);
        SupportTicketEntity saved = ticketRepository.save(ticket);
        auditLogService.record(
                "SUPPORT_TICKET_CREATED",
                principal.getId(),
                AuditResourceType.SUPPORT_TICKET,
                saved.getId(),
                saved.getSubject());
        return toDetail(saved, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketSummaryResponse> listMine(int page, int size) {
        UserPrincipal principal = requirePrincipal();
        Specification<SupportTicketEntity> spec =
                (root, query, cb) -> cb.equal(root.get("createdBy"), principal.getId());
        return pageTickets(spec, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketSummaryResponse> adminList(
            SupportTicketStatus status,
            SupportTicketPriority priority,
            UUID assignedAdminId,
            int page,
            int size
    ) {
        requireAdmin();
        Specification<SupportTicketEntity> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (assignedAdminId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignedAdminId"), assignedAdminId));
        }
        return pageTickets(spec, page, size);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getById(UUID id) {
        SupportTicketEntity ticket = requireReadable(id);
        return toDetail(ticket, messageRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public SupportTicketResponse addMessage(UUID id, SupportMessageRequest request) {
        UserPrincipal principal = requirePrincipal();
        SupportTicketEntity ticket = requireReadable(id);
        if (CLOSED_STATES.contains(ticket.getStatus())) {
            throw new BadRequestException("Cannot add a message to a closed ticket");
        }
        SupportMessageEntity message = new SupportMessageEntity();
        message.setTicket(ticket);
        message.setAuthorId(principal.getId());
        message.setBody(request.body().trim());
        messageRepository.save(message);
        if (principal.getRole() == RoleName.ADMIN) {
            if (ticket.getStatus() == SupportTicketStatus.OPEN) {
                ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            } else if (ticket.getStatus() != SupportTicketStatus.RESOLVED) {
                ticket.setStatus(SupportTicketStatus.WAITING_FOR_USER);
            }
            if (ticket.getAssignedAdminId() == null) {
                ticket.setAssignedAdminId(principal.getId());
            }
        } else if (ticket.getStatus() == SupportTicketStatus.WAITING_FOR_USER) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(ticket);
        auditLogService.record(
                "SUPPORT_TICKET_MESSAGE",
                principal.getId(),
                AuditResourceType.SUPPORT_TICKET,
                id,
                null);
        return toDetail(ticket, messageRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public SupportTicketResponse close(UUID id) {
        UserPrincipal principal = requirePrincipal();
        SupportTicketEntity ticket = requireReadable(id);
        if (principal.getRole() != RoleName.ADMIN && !ticket.getCreatedBy().equals(principal.getId())) {
            throw new NotFoundException("Support ticket not found");
        }
        ticket.setStatus(SupportTicketStatus.CLOSED);
        if (ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(Instant.now());
        }
        ticketRepository.save(ticket);
        auditLogService.record(
                "SUPPORT_TICKET_CLOSED",
                principal.getId(),
                AuditResourceType.SUPPORT_TICKET,
                id,
                null);
        return toDetail(ticket, messageRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public SupportTicketResponse adminUpdate(UUID id, AdminSupportTicketUpdateRequest request) {
        UserPrincipal principal = requireAdmin();
        SupportTicketEntity ticket = ticketRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
        if (request.status() != null) {
            ticket.setStatus(request.status());
            if (request.status() == SupportTicketStatus.RESOLVED || request.status() == SupportTicketStatus.CLOSED) {
                ticket.setResolvedAt(Instant.now());
            }
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignedAdminId() != null) {
            UserEntity admin = userRepository
                    .findActiveById(request.assignedAdminId())
                    .orElseThrow(() -> new NotFoundException("Admin user not found"));
            if (admin.getRole() != RoleName.ADMIN) {
                throw new BadRequestException("Assignee must be an admin");
            }
            ticket.setAssignedAdminId(admin.getId());
            if (ticket.getStatus() == SupportTicketStatus.OPEN) {
                ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            }
        }
        ticketRepository.save(ticket);
        auditLogService.record(
                "SUPPORT_TICKET_UPDATED",
                principal.getId(),
                AuditResourceType.SUPPORT_TICKET,
                id,
                "status=" + ticket.getStatus() + ",priority=" + ticket.getPriority());
        return toDetail(ticket, messageRepository.findByTicketIdOrderByCreatedAtAsc(id));
    }

    private PageResponse<SupportTicketSummaryResponse> pageTickets(
            Specification<SupportTicketEntity> spec, int page, int size) {
        Page<SupportTicketEntity> result = ticketRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private SupportTicketEntity requireReadable(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return ticketRepository.findById(id).orElseThrow(() -> new NotFoundException("Support ticket not found"));
        }
        return ticketRepository
                .findByIdAndCreatedBy(id, principal.getId())
                .orElseThrow(() -> new NotFoundException("Support ticket not found"));
    }

    private SupportTicketSummaryResponse toSummary(SupportTicketEntity ticket) {
        return new SupportTicketSummaryResponse(
                ticket.getId(),
                ticket.getCreatedBy(),
                displayName(ticket.getCreatedBy()),
                ticket.getSubject(),
                ticket.getCategory().getName(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignedAdminId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    private SupportTicketResponse toDetail(SupportTicketEntity ticket, List<SupportMessageEntity> messages) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getCreatedBy(),
                displayName(ticket.getCreatedBy()),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getCategory().getId(),
                ticket.getCategory().getName(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignedAdminId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                messages.stream().map(this::toMessage).toList());
    }

    private SupportMessageResponse toMessage(SupportMessageEntity message) {
        return new SupportMessageResponse(
                message.getId(),
                message.getAuthorId(),
                displayName(message.getAuthorId()),
                message.getBody(),
                message.getCreatedAt());
    }

    private String displayName(UUID userId) {
        return userRepository
                .findById(userId)
                .map(u -> (StringUtils.hasText(u.getFirstName()) ? u.getFirstName() : "")
                        + (StringUtils.hasText(u.getLastName()) ? " " + u.getLastName() : ""))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .orElse("User");
    }

    private UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private UserPrincipal requireAdmin() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.ADMIN) {
            throw new NotFoundException("Support ticket not found");
        }
        return principal;
    }
}

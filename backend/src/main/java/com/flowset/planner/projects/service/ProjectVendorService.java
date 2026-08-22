package com.flowset.planner.projects.service;

import com.flowset.common.ProjectActivityType;
import com.flowset.common.VendorStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.planner.projects.dto.AddProjectVendorRequest;
import com.flowset.planner.projects.dto.ProjectVendorResponse;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.entity.ProjectVendorEntity;
import com.flowset.planner.projects.mapper.ProjectMapper;
import com.flowset.planner.projects.repository.ProjectProductRepository;
import com.flowset.planner.projects.repository.ProjectVendorRepository;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.entity.VendorServiceAreaEntity;
import com.flowset.vendor.repository.VendorRepository;
import com.flowset.vendor.repository.VendorServiceAreaRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved vendors are a shortlist. Removing a vendor from a project must not
 * cancel any future booking (Prompt 13).
 */
@Service
public class ProjectVendorService {

    private final ProjectVendorRepository projectVendorRepository;
    private final ProjectProductRepository projectProductRepository;
    private final VendorRepository vendorRepository;
    private final VendorServiceAreaRepository serviceAreaRepository;
    private final ProjectAccessService accessService;
    private final ProjectActivityService activityService;
    private final ProjectMapper mapper;

    public ProjectVendorService(
            ProjectVendorRepository projectVendorRepository,
            ProjectProductRepository projectProductRepository,
            VendorRepository vendorRepository,
            VendorServiceAreaRepository serviceAreaRepository,
            ProjectAccessService accessService,
            ProjectActivityService activityService,
            ProjectMapper mapper
    ) {
        this.projectVendorRepository = projectVendorRepository;
        this.projectProductRepository = projectProductRepository;
        this.vendorRepository = vendorRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.accessService = accessService;
        this.activityService = activityService;
        this.mapper = mapper;
    }

    @Transactional
    public ProjectVendorResponse save(UUID projectId, AddProjectVendorRequest request) {
        ProjectEntity project = accessService.requireWritable(projectId);
        VendorEntity vendor = vendorRepository
                .findActiveById(request.vendorId())
                .orElseThrow(() -> new NotFoundException("Vendor not found"));
        if (vendor.getStatus() == VendorStatus.SUSPENDED) {
            throw new BadRequestException("Vendor unavailable");
        }
        if (projectVendorRepository.existsByProjectIdAndVendorId(projectId, vendor.getId())) {
            throw new ConflictException("Vendor already saved to this project");
        }
        ProjectVendorEntity row = new ProjectVendorEntity();
        row.setProject(project);
        row.setVendor(vendor);
        row.setNotes(request.notes());
        ProjectVendorEntity saved = projectVendorRepository.save(row);
        activityService.record(
                project,
                ProjectActivityType.VENDOR_SAVED,
                "Saved " + vendor.getCompanyName(),
                Map.of("vendorId", vendor.getId().toString()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectVendorResponse> list(UUID projectId) {
        accessService.requireReadable(projectId);
        return projectVendorRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void remove(UUID projectId, UUID vendorId) {
        ProjectEntity project = accessService.requireWritable(projectId);
        ProjectVendorEntity row = projectVendorRepository
                .findByProjectIdAndVendorId(projectId, vendorId)
                .orElseThrow(() -> new NotFoundException("Saved vendor not found"));
        String name = row.getVendor().getCompanyName();
        projectVendorRepository.delete(row);
        activityService.record(project, ProjectActivityType.VENDOR_REMOVED, "Removed " + name, Map.of());
    }

    private ProjectVendorResponse toResponse(ProjectVendorEntity saved) {
        long products = projectProductRepository.countByProjectIdAndVendorId(
                saved.getProject().getId(), saved.getVendor().getId());
        String city = serviceAreaRepository.findByVendorIdOrderByCityAsc(saved.getVendor().getId()).stream()
                .map(VendorServiceAreaEntity::getCity)
                .findFirst()
                .orElse(null);
        return mapper.toVendor(saved, products, city);
    }
}

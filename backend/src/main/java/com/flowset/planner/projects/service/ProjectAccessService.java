package com.flowset.planner.projects.service;

import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.planner.projects.exception.ProjectNotFoundException;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.security.UserPrincipal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public ProjectAccessService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    /**
     * Planner: own project. Admin: any active project. Anyone else: 404.
     * Never returns 403 for a project the caller does not own — that would leak existence.
     */
    public ProjectEntity requireReadable(UUID projectId) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return projectRepository.findActiveById(projectId).orElseThrow(ProjectNotFoundException::new);
        }
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ProjectNotFoundException();
        }
        return projectRepository
                .findActiveByIdAndPlannerId(projectId, principal.getId())
                .orElseThrow(ProjectNotFoundException::new);
    }

    /**
     * Only the owning planner may mutate. Admins may view, not edit.
     */
    public ProjectEntity requireWritable(UUID projectId) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only the project owner can modify this project");
        }
        return projectRepository
                .findActiveByIdAndPlannerId(projectId, principal.getId())
                .orElseThrow(ProjectNotFoundException::new);
    }

    public UUID requirePlannerId() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only planners can create projects");
        }
        return principal.getId();
    }
}

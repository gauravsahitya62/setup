package com.flowset.planner.projects.exception;

import com.flowset.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Always mapped to HTTP 404. Used for both missing and unauthorized projects
 * so existence of another planner's project is not leaked.
 */
public final class ProjectNotFoundException extends ApiException {

    public ProjectNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Not Found", "Project not found");
    }
}

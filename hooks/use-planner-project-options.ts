"use client";

import * as React from "react";

import { useAuth } from "@/providers";
import { ApiClientError, mapProjectSummary, projectApi } from "@/services";

export interface PlannerProjectOption {
  id: string;
  name: string;
}

export function usePlannerProjectOptions(enabled: boolean) {
  const { isAuthenticated, role, isLoading: authLoading } = useAuth();
  const canUse = isAuthenticated && role === "planner";
  const [projects, setProjects] = React.useState<PlannerProjectOption[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const reload = React.useCallback(async () => {
    if (authLoading) return;
    if (!canUse) {
      setProjects([]);
      setError(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const page = await projectApi.list({ size: 50, sort: "updatedAt", direction: "desc" });
      const mapped = (page.content ?? []).map(mapProjectSummary);
      setProjects(mapped.map((project) => ({ id: project.id, name: project.name })));
    } catch (err) {
      setProjects([]);
      setError(err instanceof ApiClientError ? err.message : "Could not load projects");
    } finally {
      setLoading(false);
    }
  }, [authLoading, canUse]);

  React.useEffect(() => {
    if (!enabled) return;
    void reload();
  }, [enabled, reload]);

  const addProject = React.useCallback((project: PlannerProjectOption) => {
    setProjects((current) => [project, ...current.filter((item) => item.id !== project.id)]);
  }, []);

  return {
    projects,
    loading: authLoading || (canUse && loading),
    error,
    canUse,
    authLoading,
    isAuthenticated,
    reload,
    addProject,
  };
}

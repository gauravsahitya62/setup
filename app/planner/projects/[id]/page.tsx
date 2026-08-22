import { ProjectWorkspaceView } from "@/components/planner/project-workspace";
import { APP_NAME } from "@/constants";

interface ProjectWorkspacePageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: ProjectWorkspacePageProps) {
  const { id } = await params;
  return { title: `Project · ${APP_NAME}`, description: `Workspace ${id}` };
}

export default async function ProjectWorkspacePage({ params }: ProjectWorkspacePageProps) {
  const { id } = await params;
  return <ProjectWorkspaceView projectId={id} />;
}

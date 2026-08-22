"use client";

import * as React from "react";
import {
  Download,
  FileImage,
  FileSpreadsheet,
  FileText,
  File as FileIcon,
  Trash2,
  Upload,
} from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/shared/empty-state";
import { fileKind, formatFileSize } from "@/lib/projects/labels";
import { ApiClientError, projectApi, type ProjectFileDto } from "@/services";

interface ProjectFilesSectionProps {
  projectId: string;
}

const icons = {
  pdf: FileText,
  image: FileImage,
  spreadsheet: FileSpreadsheet,
  document: FileText,
  other: FileIcon,
} as const;

export function ProjectFilesSection({ projectId }: ProjectFilesSectionProps) {
  const [files, setFiles] = React.useState<ProjectFileDto[]>([]);
  const inputRef = React.useRef<HTMLInputElement>(null);

  const reload = React.useCallback(async () => {
    try {
      setFiles(await projectApi.files.list(projectId));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load files");
    }
  }, [projectId]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const upload = async (file?: File) => {
    if (!file) return;
    try {
      const created = await projectApi.files.upload(projectId, file);
      setFiles((prev) => [created, ...prev]);
      toast.success(`Uploaded ${created.fileName}`);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-end justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Files</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Venue layouts, briefs, and quotations. Files are stored outside PostgreSQL.
          </p>
        </div>
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          onChange={(event) => void upload(event.target.files?.[0])}
        />
        <Button
          variant="outline"
          size="sm"
          className="rounded-xl"
          onClick={() => inputRef.current?.click()}
        >
          <Upload className="size-3.5" />
          Upload
        </Button>
      </div>

      {files.length === 0 ? (
        <EmptyState
          title="No files yet"
          description="Upload a venue layout, brief, or quotation."
        />
      ) : (
        <Card className="border-border/70 shadow-soft">
          <CardContent className="divide-border/60 divide-y p-0">
            {files.map((file) => {
              const Icon = icons[fileKind(file.fileType)];
              return (
                <div key={file.id} className="flex items-center gap-3 px-4 py-3.5 sm:px-5">
                  <div className="bg-muted/80 text-muted-foreground flex size-10 shrink-0 items-center justify-center rounded-xl">
                    <Icon className="size-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{file.fileName}</p>
                    <p className="text-muted-foreground mt-0.5 text-xs">
                      {file.fileType} · {formatFileSize(file.fileSize)} · {file.uploadedByName} ·{" "}
                      {new Date(file.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <Button variant="ghost" size="sm" className="rounded-lg" asChild>
                    <a href={file.fileUrl} target="_blank" rel="noreferrer">
                      <Download className="size-3.5" />
                      Download
                    </a>
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive rounded-lg"
                    onClick={async () => {
                      await projectApi.files.remove(projectId, file.id);
                      setFiles((prev) => prev.filter((row) => row.id !== file.id));
                    }}
                  >
                    <Trash2 className="size-3.5" />
                    Delete
                  </Button>
                </div>
              );
            })}
          </CardContent>
        </Card>
      )}
    </div>
  );
}

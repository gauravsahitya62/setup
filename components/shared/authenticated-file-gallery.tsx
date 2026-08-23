"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ApiClientError, apiClient } from "@/services";

export type AuthenticatedGalleryFile = {
  id: string;
  fileUrl: string;
  fileName: string;
  fileType?: string | null;
};

function isImageFile(file: AuthenticatedGalleryFile, blobType?: string) {
  const type = (file.fileType || blobType || "").toLowerCase();
  if (type.startsWith("image/")) return true;
  return /\.(png|jpe?g|gif|webp|bmp)$/i.test(file.fileName);
}

function isPdfFile(file: AuthenticatedGalleryFile, blobType?: string) {
  const type = (file.fileType || blobType || "").toLowerCase();
  return type.includes("pdf") || file.fileName.toLowerCase().endsWith(".pdf");
}

export function AuthenticatedFileGallery({
  files,
  meta,
}: {
  files: AuthenticatedGalleryFile[];
  meta?: (file: AuthenticatedGalleryFile) => React.ReactNode;
}) {
  const [open, setOpen] = React.useState(false);
  const [index, setIndex] = React.useState(0);
  const [objectUrl, setObjectUrl] = React.useState<string | null>(null);
  const [loadingId, setLoadingId] = React.useState<string | null>(null);
  const [previewKind, setPreviewKind] = React.useState<"image" | "pdf">("image");

  const revoke = React.useCallback((url: string | null) => {
    if (url) URL.revokeObjectURL(url);
  }, []);

  React.useEffect(() => {
    return () => revoke(objectUrl);
  }, [objectUrl, revoke]);

  const loadFile = React.useCallback(
    async (nextIndex: number) => {
      const file = files[nextIndex];
      if (!file) return;
      setLoadingId(file.id);
      try {
        const blob = await apiClient.getBlob(file.fileUrl);
        const url = URL.createObjectURL(blob);
        setObjectUrl((current) => {
          revoke(current);
          return url;
        });
        setIndex(nextIndex);
        if (isImageFile(file, blob.type)) {
          setPreviewKind("image");
          setOpen(true);
        } else if (isPdfFile(file, blob.type)) {
          setPreviewKind("pdf");
          setOpen(true);
        } else {
          const link = document.createElement("a");
          link.href = url;
          link.download = file.fileName;
          link.rel = "noreferrer";
          document.body.appendChild(link);
          link.click();
          link.remove();
        }
      } catch (error) {
        toast.error(error instanceof ApiClientError ? error.message : "Could not open file");
      } finally {
        setLoadingId(null);
      }
    },
    [files, revoke]
  );

  const active = files[index];

  return (
    <>
      <ul className="space-y-1">
        {files.map((file, fileIndex) => (
          <li key={file.id}>
            <button
              type="button"
              className="text-left underline disabled:opacity-60"
              disabled={loadingId === file.id}
              onClick={() => void loadFile(fileIndex)}
            >
              {loadingId === file.id ? "Opening…" : file.fileName}
            </button>
            {meta ? meta(file) : null}
          </li>
        ))}
      </ul>

      <Dialog
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) {
            setObjectUrl((current) => {
              revoke(current);
              return null;
            });
          }
        }}
      >
        <DialogContent className="max-h-[90vh] overflow-hidden sm:max-w-4xl">
          <DialogHeader>
            <DialogTitle>{active?.fileName ?? "File preview"}</DialogTitle>
            <DialogDescription>Proof of delivery preview</DialogDescription>
          </DialogHeader>
          {objectUrl ? (
            previewKind === "image" ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={objectUrl}
                alt={active?.fileName ?? "Proof of delivery"}
                className="mx-auto max-h-[70vh] w-auto max-w-full rounded-lg object-contain"
              />
            ) : (
              <iframe
                title={active?.fileName ?? "PDF preview"}
                src={objectUrl}
                className="bg-muted h-[70vh] w-full rounded-lg"
              />
            )
          ) : null}
          {files.length > 1 ? (
            <div className="flex items-center justify-between gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={index <= 0 || loadingId !== null}
                onClick={() => void loadFile(index - 1)}
              >
                <ChevronLeft className="size-4" />
                Previous
              </Button>
              <p className="text-muted-foreground text-xs">
                {index + 1} of {files.length}
              </p>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={index >= files.length - 1 || loadingId !== null}
                onClick={() => void loadFile(index + 1)}
              >
                Next
                <ChevronRight className="size-4" />
              </Button>
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  );
}

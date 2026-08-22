"use client";

import * as React from "react";
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { SortableContext, arrayMove, rectSortingStrategy, useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Trash2, Upload } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { EmptyState } from "@/components/shared/empty-state";
import { ApiClientError, projectApi, type MoodBoardItemDto } from "@/services";

interface ProjectMoodBoardSectionProps {
  projectId: string;
}

function SortableTile({
  item,
  onCaption,
  onDelete,
  onOpen,
}: {
  item: MoodBoardItemDto;
  onCaption: (id: string, caption: string) => void;
  onDelete: (id: string) => void;
  onOpen: (item: MoodBoardItemDto) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: item.id });
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className="border-border/70 bg-card shadow-soft mb-4 break-inside-avoid overflow-hidden rounded-2xl border"
    >
      <button
        type="button"
        className="block w-full"
        onClick={() => onOpen(item)}
        {...attributes}
        {...listeners}
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={item.imageUrl}
          alt={item.caption ?? "Mood board"}
          className="aspect-[4/5] w-full object-cover"
        />
      </button>
      <div className="flex items-center gap-2 p-3">
        <Input
          defaultValue={item.caption ?? ""}
          placeholder="Caption"
          className="h-8 rounded-lg"
          onBlur={(event) => onCaption(item.id, event.target.value)}
        />
        <Button
          variant="ghost"
          size="icon-sm"
          className="text-destructive rounded-lg"
          onClick={() => onDelete(item.id)}
        >
          <Trash2 className="size-3.5" />
        </Button>
      </div>
    </div>
  );
}

export function ProjectMoodBoardSection({ projectId }: ProjectMoodBoardSectionProps) {
  const [items, setItems] = React.useState<MoodBoardItemDto[]>([]);
  const [active, setActive] = React.useState<MoodBoardItemDto | null>(null);
  const [url, setUrl] = React.useState("");
  const fileRef = React.useRef<HTMLInputElement>(null);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));

  const reload = React.useCallback(async () => {
    try {
      const board = await projectApi.moodBoard.get(projectId);
      setItems(board.items);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load mood board");
    }
  }, [projectId]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const onDragEnd = async (event: DragEndEvent) => {
    const { active: dragging, over } = event;
    if (!over || dragging.id === over.id) return;
    const oldIndex = items.findIndex((item) => item.id === dragging.id);
    const newIndex = items.findIndex((item) => item.id === over.id);
    const next = arrayMove(items, oldIndex, newIndex);
    setItems(next);
    try {
      await projectApi.moodBoard.reorder(
        projectId,
        next.map((item) => item.id)
      );
    } catch {
      await reload();
    }
  };

  const upload = async (file?: File) => {
    if (!file) return;
    try {
      const created = await projectApi.moodBoard.upload(projectId, file);
      setItems((prev) => [...prev, created]);
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Upload failed");
    }
  };

  const addUrl = async () => {
    if (!url.trim()) return;
    try {
      const created = await projectApi.moodBoard.addUrl(projectId, { imageUrl: url.trim() });
      setItems((prev) => [...prev, created]);
      setUrl("");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not add image URL");
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold tracking-tight">Mood Board</h2>
          <p className="text-muted-foreground mt-1 text-sm">
            Inspiration images — drag to reorder.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Input
            value={url}
            onChange={(event) => setUrl(event.target.value)}
            placeholder="Paste image URL"
            className="h-9 w-52 rounded-xl"
          />
          <Button variant="outline" size="sm" className="rounded-xl" onClick={() => void addUrl()}>
            Add URL
          </Button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(event) => void upload(event.target.files?.[0])}
          />
          <Button size="sm" className="rounded-xl" onClick={() => fileRef.current?.click()}>
            <Upload className="size-3.5" />
            Upload
          </Button>
        </div>
      </div>

      {items.length === 0 ? (
        <EmptyState title="No inspiration yet" description="Upload photos or paste an image URL." />
      ) : (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={(event) => void onDragEnd(event)}
        >
          <SortableContext items={items.map((item) => item.id)} strategy={rectSortingStrategy}>
            <div className="columns-1 gap-4 sm:columns-2 lg:columns-3">
              {items.map((item) => (
                <SortableTile
                  key={item.id}
                  item={item}
                  onOpen={setActive}
                  onCaption={async (id, caption) => {
                    await projectApi.moodBoard.update(projectId, id, { caption });
                  }}
                  onDelete={async (id) => {
                    await projectApi.moodBoard.remove(projectId, id);
                    setItems((prev) => prev.filter((row) => row.id !== id));
                  }}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}

      {active ? (
        <div
          className="bg-background/80 fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
          role="dialog"
          onClick={() => setActive(null)}
        >
          <div
            className="bg-card max-h-[90vh] max-w-3xl overflow-hidden rounded-2xl border"
            onClick={(event) => event.stopPropagation()}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={active.imageUrl}
              alt={active.caption ?? ""}
              className="max-h-[75vh] w-full object-contain"
            />
            <div className="flex justify-end p-3">
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl"
                onClick={() => setActive(null)}
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

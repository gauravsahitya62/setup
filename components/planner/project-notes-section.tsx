"use client";

import * as React from "react";
import { Pencil, Pin, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { ApiClientError, projectApi, type ProjectNoteDto } from "@/services";
import { cn } from "@/lib/utils";

interface ProjectNotesSectionProps {
  projectId: string;
}

export function ProjectNotesSection({ projectId }: ProjectNotesSectionProps) {
  const [notes, setNotes] = React.useState<ProjectNoteDto[]>([]);
  const [title, setTitle] = React.useState("");
  const [draft, setDraft] = React.useState("");
  const [saving, setSaving] = React.useState(false);
  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [editTitle, setEditTitle] = React.useState("");
  const [editContent, setEditContent] = React.useState("");

  const reload = React.useCallback(async () => {
    try {
      setNotes(await projectApi.notes.list(projectId));
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not load notes");
    }
  }, [projectId]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  const add = async () => {
    if (!draft.trim() || !title.trim()) return;
    setSaving(true);
    try {
      const created = await projectApi.notes.create(projectId, {
        title: title.trim(),
        content: draft.trim(),
      });
      setNotes((prev) => [created, ...prev]);
      setTitle("");
      setDraft("");
    } catch (error) {
      toast.error(error instanceof ApiClientError ? error.message : "Could not add note");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-tight">Notes</h2>
        <p className="text-muted-foreground mt-1 text-sm">
          Capture decisions, constraints, and open questions.
        </p>
      </div>

      <Card className="border-border/70 shadow-soft">
        <CardHeader className="pb-3">
          <CardTitle className="text-base">New note</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Title"
            className="border-input h-9 w-full rounded-lg border bg-transparent px-3 text-sm outline-none"
          />
          <Textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Write a note…"
            className="min-h-24 rounded-xl"
          />
          <div className="flex justify-end">
            <Button
              size="sm"
              className="rounded-xl"
              onClick={() => void add()}
              disabled={saving || !draft.trim()}
            >
              <Plus className="size-3.5" />
              Add note
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        {notes.map((note) => (
          <Card
            key={note.id}
            className={cn(
              "border-border/70 shadow-soft",
              note.pinned && "border-emerald-500/20 bg-emerald-500/[0.03]"
            )}
          >
            <CardHeader className="flex-row items-start justify-between gap-3 space-y-0">
              {editingId === note.id ? (
                <input
                  value={editTitle}
                  onChange={(event) => setEditTitle(event.target.value)}
                  className="border-input h-9 flex-1 rounded-lg border bg-transparent px-3 text-sm outline-none"
                />
              ) : (
                <CardTitle className="text-base">{note.title}</CardTitle>
              )}
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="rounded-lg"
                  onClick={() => {
                    setEditingId(note.id);
                    setEditTitle(note.title);
                    setEditContent(note.content);
                  }}
                >
                  <Pencil className="size-3.5" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="rounded-lg"
                  onClick={async () => {
                    try {
                      const updated = await projectApi.notes.update(projectId, note.id, {
                        pinned: !note.pinned,
                      });
                      setNotes((prev) =>
                        prev
                          .map((row) => (row.id === note.id ? updated : row))
                          .sort((a, b) => Number(b.pinned) - Number(a.pinned))
                      );
                    } catch (error) {
                      toast.error(
                        error instanceof ApiClientError ? error.message : "Could not pin note"
                      );
                    }
                  }}
                >
                  <Pin className={cn("size-3.5", note.pinned && "text-emerald-700")} />
                </Button>
                <Button
                  variant="ghost"
                  size="icon-sm"
                  className="text-destructive rounded-lg"
                  onClick={async () => {
                    try {
                      await projectApi.notes.remove(projectId, note.id);
                      setNotes((prev) => prev.filter((row) => row.id !== note.id));
                    } catch (error) {
                      toast.error(
                        error instanceof ApiClientError ? error.message : "Could not delete note"
                      );
                    }
                  }}
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {editingId === note.id ? (
                <div className="space-y-3">
                  <Textarea
                    value={editContent}
                    onChange={(event) => setEditContent(event.target.value)}
                    className="min-h-24 rounded-xl"
                  />
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="rounded-xl"
                      onClick={() => setEditingId(null)}
                    >
                      Cancel
                    </Button>
                    <Button
                      size="sm"
                      className="rounded-xl"
                      onClick={async () => {
                        try {
                          const updated = await projectApi.notes.update(projectId, note.id, {
                            title: editTitle.trim(),
                            content: editContent.trim(),
                          });
                          setNotes((prev) =>
                            prev.map((row) => (row.id === note.id ? updated : row))
                          );
                          setEditingId(null);
                        } catch (error) {
                          toast.error(
                            error instanceof ApiClientError
                              ? error.message
                              : "Could not update note"
                          );
                        }
                      }}
                    >
                      Save
                    </Button>
                  </div>
                </div>
              ) : (
                <>
                  <p className="text-muted-foreground text-sm leading-relaxed whitespace-pre-wrap">
                    {note.content}
                  </p>
                  <p className="text-muted-foreground mt-4 text-xs">
                    {note.authorName} · {new Date(note.updatedAt).toLocaleString()}
                  </p>
                </>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

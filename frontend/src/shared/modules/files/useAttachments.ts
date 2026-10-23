import { useEffect, useRef, useState } from 'react';
import type { Attachment, AttachmentItem } from './types';
import { usePostHog } from '@posthog/react';
import { AttachmentUploadError, uploadAttachment } from './api';
import { getFileContentType, validateAttachments } from './validation';

interface Options {
  initialAttachments?: Attachment[];
  kind: 'image' | 'file';
  contentType: 'notice' | 'assignment' | 'submission';
  mode: 'create' | 'edit';
}

export function useAttachments({ initialAttachments = [], kind, contentType, mode }: Options) {
  const posthog = usePostHog();
  const context = { content_type: contentType, mode, attachment_kind: kind };
  const [items, setItems] = useState<AttachmentItem[]>(() =>
    [...initialAttachments]
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map((file) => ({
        id: `existing-${file.fileId}`,
        name: file.originalFileName,
        fileId: file.fileId,
        status: 'completed',
      })),
  );
  const [error, setError] = useState<string>();
  // 연속 선택과 비동기 응답에도 가장 최근 목록을 기준으로 개수와 삭제를 처리한다.
  const currentItems = useRef(items);
  const nextId = useRef(0);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  function updateItems(next: AttachmentItem[]) {
    currentItems.current = next;
    setItems(next);
  }

  function updateItem(id: string, next: AttachmentItem) {
    if (!mounted.current) return;
    // 삭제한 행은 다시 추가하지 않는다.
    updateItems(currentItems.current.map((item) => (item.id === id ? next : item)));
  }

  function addFiles(files: File[]) {
    if (!files.length) return;
    const invalid = validateAttachments(files, currentItems.current.length, kind);
    setError(invalid?.message);
    if (invalid) {
      posthog?.capture('attachment_validation_failed', {
        ...context,
        reason: invalid.reason,
        selected_count: files.length,
        existing_count: currentItems.current.length,
      });
      return;
    }

    const added = files.map((file): AttachmentItem => ({
      id: `new-${nextId.current++}`,
      name: file.name,
      status: 'uploading',
    }));
    updateItems([...currentItems.current, ...added]);
    files.forEach((file, index) => {
      const item = added[index];
      const startedAt = performance.now();
      const properties = {
        ...context,
        upload_attempt_id: crypto.randomUUID(),
        file_size_bytes: file.size,
        mime_type: getFileContentType(file),
      };
      posthog?.capture('attachment_upload_started', properties);
      void uploadAttachment(file).then(
        (fileId) => {
          posthog?.capture('attachment_upload_completed', {
            ...properties,
            duration_ms: Math.round(performance.now() - startedAt),
          });
          updateItem(item.id, { id: item.id, name: item.name, status: 'completed', fileId });
        },
        (cause: unknown) => {
          posthog?.capture('attachment_upload_failed', {
            ...properties,
            duration_ms: Math.round(performance.now() - startedAt),
            failure_stage: cause instanceof AttachmentUploadError ? cause.stage : 'unknown',
          });
          updateItem(item.id, {
            id: item.id,
            name: item.name,
            status: 'error',
            message: cause instanceof Error ? cause.message : '파일 업로드에 실패했어요.',
          });
        },
      );
    });
  }

  function removeFile(id: string) {
    updateItems(currentItems.current.filter((item) => item.id !== id));
    setError(undefined);
  }

  return {
    items,
    error,
    addFiles,
    removeFile,
    isReady: items.every((item) => item.status === 'completed'),
    attachmentFileIds: items.flatMap((item) => (item.status === 'completed' ? [item.fileId] : [])),
  };
}

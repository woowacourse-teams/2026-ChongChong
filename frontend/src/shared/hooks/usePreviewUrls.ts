import { useEffect, useRef, useState } from 'react';
import type { AttachmentItem } from '../modules/files/types';

export function usePreviewUrls(items: AttachmentItem[]) {
  const urlsRef = useRef(new Map<string, { file: File; url: string }>());
  const [urls, setUrls] = useState<Record<string, string>>({});

  useEffect(() => {
    const previews = items.filter((item) => item.previewFile);
    const activeIds = new Set(previews.map((item) => item.id));
    let changed = false;

    for (const [id, entry] of urlsRef.current) {
      const item = previews.find((preview) => preview.id === id);
      if (activeIds.has(id) && item?.previewFile === entry.file) continue;
      URL.revokeObjectURL(entry.url);
      urlsRef.current.delete(id);
      changed = true;
    }

    for (const item of previews) {
      if (!item.previewFile || urlsRef.current.has(item.id)) continue;
      urlsRef.current.set(item.id, {
        file: item.previewFile,
        url: URL.createObjectURL(item.previewFile),
      });
      changed = true;
    }

    if (changed) {
      setUrls(Object.fromEntries([...urlsRef.current].map(([id, entry]) => [id, entry.url])));
    }
  }, [items]);

  useEffect(
    () => () => {
      for (const entry of urlsRef.current.values()) URL.revokeObjectURL(entry.url);
      urlsRef.current.clear();
    },
    [],
  );

  return urls;
}

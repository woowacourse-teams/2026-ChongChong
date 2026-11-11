import { useEffect, useRef, useState } from 'react';
import { uploadAttachment } from '../../../shared/modules/files/api';
import { validateAttachments } from '../../../shared/modules/files/validation';

// TODO: useAttachment와 동일한 로직을 파악하고 최대한 책임, 역할을 분리
interface Options {
  initialImageUrl?: string | null;
  fieldErrors?: { imageFileId?: string };
}

export default function useStudyProfileImage({ initialImageUrl, fieldErrors }: Options) {
  const [selection, setSelection] = useState<{ url?: string | null; fileId?: number | null }>({
    url: initialImageUrl,
  });
  const [isUploading, setIsUploading] = useState(false);
  const isMounted = useRef(false);
  const [error, setError] = useState<string>();
  const [dismissedErrors, setDismissedErrors] = useState<Options['fieldErrors']>();
  const serverError = fieldErrors === dismissedErrors ? undefined : fieldErrors?.imageFileId;

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);
  useEffect(() => {
    const url = selection.fileId === undefined ? undefined : selection.url;
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [selection]);

  async function select(file: File) {
    if (isUploading) return;
    setDismissedErrors(fieldErrors);
    setError(undefined);
    const validation = validateAttachments([file], 0, 'image', { includeFileName: false });
    if (validation) {
      setError(validation.message);
      return;
    }
    setIsUploading(true);
    let previewUrl: string | undefined;
    try {
      const fileId = await uploadAttachment(file);
      if (!isMounted.current) return;
      previewUrl = URL.createObjectURL(file);
      const preview = new Image();
      preview.src = previewUrl;
      try {
        await preview.decode();
      } catch (cause) {
        throw new Error('이미지를 불러오지 못했어요. 다른 이미지를 선택해 주세요.', { cause });
      }
      if (!isMounted.current) return;
      setSelection({ url: previewUrl, fileId });
      previewUrl = undefined;
    } catch (error) {
      if (!isMounted.current) return;
      setError(
        error instanceof Error
          ? error.message
          : '이미지를 업로드하지 못했어요. 다시 선택해 주세요.',
      );
    } finally {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      if (isMounted.current) setIsUploading(false);
    }
  }

  function reset() {
    if (isUploading) return;
    setDismissedErrors(fieldErrors);
    setError(undefined);
    setSelection({ fileId: initialImageUrl ? null : undefined });
  }

  return {
    imageUrl: selection.url,
    imageFileId: selection.fileId,
    isUploading,
    error: error ?? serverError,
    select,
    reset,
  };
}

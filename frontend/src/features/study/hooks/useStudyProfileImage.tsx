import { useEffect, useRef, useState } from 'react';
import { uploadAttachment } from '../../../shared/modules/files/api';
import { validateAttachments } from '../../../shared/modules/files/validation';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

// TODO: useAttachment와 동일한 로직을 파악하고 최대한 책임, 역할을 분리
export default function useStudyProfileImage(initialImageUrl?: string | null) {
  const [selection, setSelection] = useState<{ url?: string | null; fileId?: number }>({
    url: initialImageUrl,
  });
  const [isUploading, setIsUploading] = useState(false);
  const isMounted = useRef(false);
  const toast = useToast();

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
    const validation = validateAttachments([file], 0, 'image');
    if (validation) {
      toast.open(<StatusToast status="error" message={validation.message} />);
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
        // Keep the current photo until the replacement is ready to paint.
        await preview.decode();
      } catch (cause) {
        throw new Error('이미지를 불러오지 못했어요. 다른 이미지를 선택해 주세요.', { cause });
      }
      if (!isMounted.current) return;
      setSelection({ url: previewUrl, fileId });
      // The selection effect now owns this URL and revokes it after replacement/unmount.
      previewUrl = undefined;
    } catch (error) {
      if (!isMounted.current) return;
      toast.open(
        <StatusToast
          status="error"
          message={
            error instanceof Error
              ? error.message
              : '이미지를 업로드하지 못했어요. 다시 선택해 주세요.'
          }
        />,
      );
    } finally {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      if (isMounted.current) setIsUploading(false);
    }
  }

  function reset() {
    if (isUploading) return;
    // The current PATCH API cannot remove an already saved study image.
    if (initialImageUrl) {
      toast.open(
        <StatusToast
          status="error"
          message="저장된 이미지를 기본 프로필로 되돌리는 기능은 아직 지원되지 않아요."
        />,
      );
      return;
    }
    setSelection({});
  }

  return { imageUrl: selection.url, imageFileId: selection.fileId, isUploading, select, reset };
}

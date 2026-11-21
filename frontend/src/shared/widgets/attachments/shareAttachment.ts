import type { Attachment } from '../../modules/files/types';

export function isIOS() {
  return (
    /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
  );
}

export function isPWA() {
  return (
    (navigator as Navigator & { standalone?: boolean }).standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches
  );
}

export async function shareAttachment(attachment: Attachment) {
  if (!attachment.downloadUrl) return;
  const response = await fetch(attachment.downloadUrl);
  if (!response.ok) throw new Error(`파일 요청 실패: ${response.status}`);
  const blob = await response.blob();
  const file = new File([blob], attachment.originalFileName, {
    type: attachment.contentType,
  });
  await navigator.share({ files: [file] });
}

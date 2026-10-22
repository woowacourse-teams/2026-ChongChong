const imageTypes: Record<string, string> = {
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  png: 'image/png',
  webp: 'image/webp',
};
export const IMAGE_ACCEPT = '.jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp';

function imageContentType(file: File) {
  const extension = file.name.split('.').at(-1)?.toLowerCase() ?? '';
  return Object.hasOwn(imageTypes, extension) ? imageTypes[extension] : undefined;
}

export function getFileContentType(file: File): string {
  return file.type || imageContentType(file) || 'application/octet-stream';
}

export function validateAttachments(
  files: File[],
  currentCount: number,
  kind: 'image' | 'file',
): string | undefined {
  if (currentCount + files.length > 10) {
    return kind === 'image'
      ? '이미지는 최대 10장까지 첨부할 수 있어요.'
      : '파일은 최대 10개까지 첨부할 수 있어요.';
  }
  for (const file of files) {
    if (file.size > 5_242_880) return `${file.name}: 파일당 최대 5MB까지 첨부할 수 있어요.`;
    if (kind === 'image') {
      if (file.size === 0) return `${file.name}: 비어 있는 이미지는 첨부할 수 없어요.`;
      if (!imageContentType(file) || getFileContentType(file) !== imageContentType(file)) {
        return `${file.name}: JPEG, PNG, WebP 이미지만 첨부할 수 있어요.`;
      }
    }
  }
}

import { fileTable, MOCK_UPLOAD_ORIGIN } from './db';
import { invalidInputResponse } from '../../../../mocks/errors';

let browserFilePreviews = false;
const previewUrls = new Map<string, string>();

export function enableBrowserFilePreviews() {
  browserFilePreviews = true;
}

function fileUrl(file: { id: number; contentType: string; body?: ArrayBuffer }, download = false) {
  if (!download && !['image/jpeg', 'image/png', 'image/webp'].includes(file.contentType))
    return null;
  if (!browserFilePreviews)
    return `${MOCK_UPLOAD_ORIGIN}/${download ? 'files' : 'images'}/${file.id}?issuedAt=${Date.now()}`;
  if (!file.body) return null;

  // MSW 가상 주소는 새 탭 탐색을 처리할 수 없으므로 브라우저 메모리의 파일 URL을 사용한다.
  const key = `${download ? 'download' : 'image'}-${file.id}`;
  let url = previewUrls.get(key);
  if (!url) {
    url = URL.createObjectURL(
      new Blob([file.body], { type: download ? 'application/octet-stream' : file.contentType }),
    );
    previewUrls.set(key, url);
  }
  return url;
}

export function validateFileAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
  linkedFileIds: number[] = [],
  kind: 'image' | 'file' = 'image',
) {
  let reason: string | undefined;
  if (ids.length > 10)
    reason =
      kind === 'image'
        ? '이미지는 최대 10장까지 첨부할 수 있어요.'
        : '파일은 최대 10개까지 첨부할 수 있어요.';
  else if (new Set(ids).size !== ids.length || ids.some((id) => !Number.isInteger(id) || id <= 0))
    reason = '첨부 파일 목록을 확인해주세요.';
  else {
    for (const id of ids) {
      if (existingIds.includes(id)) continue;
      const file = fileTable.findFirst((q) => q.where({ id }));
      if (!file || file.ownerUserId !== userId || file.status !== 'COMPLETED') {
        reason = '업로드가 완료된 본인의 파일만 첨부할 수 있어요.';
        break;
      }
      if (linkedFileIds.includes(id)) {
        reason = '이미 다른 게시물에 첨부된 파일이에요.';
        break;
      }
      if (
        (kind === 'image' &&
          (!['image/jpeg', 'image/png', 'image/webp'].includes(file.contentType) ||
            file.size === 0)) ||
        file.size < 0 ||
        file.size > 5_242_880
      ) {
        reason =
          kind === 'image'
            ? '5MB 이하의 JPEG, PNG, WebP 이미지만 첨부할 수 있어요.'
            : '파일당 최대 5MB까지 첨부할 수 있어요.';
        break;
      }
    }
  }
  return reason
    ? invalidInputResponse([{ field: 'attachmentFileIds', code: 'INVALID', reason }])
    : null;
}

export function fileAttachments(ids: number[] = [], withDownloadUrl = false) {
  return ids.flatMap((fileId, sortOrder) => {
    const file = fileTable.findFirst((q) => q.where({ id: fileId }));
    return file
      ? [
          {
            fileId,
            originalFileName: file.originalFileName,
            contentType: file.contentType,
            size: file.size,
            sortOrder,
            imageUrl: fileUrl(file),
            ...(withDownloadUrl ? { downloadUrl: fileUrl(file, true) } : {}),
          },
        ]
      : [];
  });
}

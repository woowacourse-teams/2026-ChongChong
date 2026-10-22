import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../shared/modules/files/mocks/db';
import { invalidInputResponse } from '../../../mocks/errors';
import { noticeTable } from './db';

export function validateNoticeAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
) {
  let reason: string | undefined;
  if (ids.length > 10) reason = '이미지는 최대 10장까지 첨부할 수 있어요.';
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
      if (noticeTable.findMany().some((notice) => notice.attachmentFileIds?.includes(id))) {
        reason = '이미 다른 공지에 첨부된 파일이에요.';
        break;
      }
      if (
        !['image/jpeg', 'image/png', 'image/webp'].includes(file.contentType) ||
        file.size <= 0 ||
        file.size > 5_242_880
      ) {
        reason = '5MB 이하의 JPEG, PNG, WebP 이미지만 첨부할 수 있어요.';
        break;
      }
    }
  }
  return reason
    ? invalidInputResponse([{ field: 'attachmentFileIds', code: 'INVALID', reason }])
    : null;
}

export function noticeAttachments(ids: number[] = []) {
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
            imageUrl: ['image/jpeg', 'image/png', 'image/webp'].includes(file.contentType)
              ? `${MOCK_UPLOAD_ORIGIN}/images/${fileId}?issuedAt=${Date.now()}`
              : null,
          },
        ]
      : [];
  });
}

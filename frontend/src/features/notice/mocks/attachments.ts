import { validateImageAttachments } from '../../../shared/modules/files/mocks/attachments';
import { assignmentTable } from '../../assignment/mocks/db';
import { noticeTable } from './db';
export { fileAttachments as noticeAttachments } from '../../../shared/modules/files/mocks/attachments';

export function validateNoticeAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
) {
  const linkedIds = [...noticeTable.findMany(), ...assignmentTable.findMany()].flatMap(
    (record) => record.attachmentFileIds ?? [],
  );
  return validateImageAttachments(ids, userId, existingIds, linkedIds);
}

import { validateFileAttachments } from '../../../shared/modules/files/mocks/attachments';
import { assignmentTable, submissionTable } from '../../assignment/mocks/db';
import { noticeTable } from './db';
export { fileAttachments as noticeAttachments } from '../../../shared/modules/files/mocks/attachments';

export function validateNoticeAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
) {
  const linkedIds = [
    ...noticeTable.findMany(),
    ...assignmentTable.findMany(),
    ...submissionTable.findMany(),
  ].flatMap((record) => record.attachmentFileIds ?? []);
  return validateFileAttachments(ids, userId, existingIds, linkedIds);
}

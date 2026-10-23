import { validateFileAttachments } from '../../../shared/modules/files/mocks/attachments';
import { noticeTable } from '../../notice/mocks/db';
import { assignmentTable, submissionTable } from './db';

export function validateAssignmentAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
  kind: 'image' | 'file' = 'image',
) {
  const linkedIds = [
    ...noticeTable.findMany(),
    ...assignmentTable.findMany(),
    ...submissionTable.findMany(),
  ].flatMap((record) => record.attachmentFileIds ?? []);
  return validateFileAttachments(ids, userId, existingIds, linkedIds, kind);
}

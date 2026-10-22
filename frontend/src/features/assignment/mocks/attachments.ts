import { validateImageAttachments } from '../../../shared/modules/files/mocks/attachments';
import { noticeTable } from '../../notice/mocks/db';
import { assignmentTable } from './db';

export function validateAssignmentAttachments(
  ids: number[],
  userId: number,
  existingIds: number[] = [],
) {
  const linkedIds = [...noticeTable.findMany(), ...assignmentTable.findMany()].flatMap(
    (record) => record.attachmentFileIds ?? [],
  );
  return validateImageAttachments(ids, userId, existingIds, linkedIds);
}

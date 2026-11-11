import { Collection } from '@msw/data';
import z from 'zod';
import { commentSchema } from '../../../shared/modules/comments/responseSchemas';

export const commentTable = new Collection({
  schema: commentSchema.omit({ isMine: true, canEdit: true, canDelete: true }).extend({
    studyId: z.number(),
    noticeId: z.number().nullable().default(null),
    assignmentId: z.number().nullable().default(null),
    submissionId: z.number().nullable().default(null),
    authorUserId: z.number().nullable(),
  }),
});

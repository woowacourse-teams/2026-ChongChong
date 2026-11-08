import z from 'zod';

export const commentSchema = z.object({
  id: z.number().int().positive(),
  author: z.object({
    name: z.string(),
    profileImageUrl: z.string().nullable(),
    role: z.enum(['LEADER', 'MEMBER']).nullable(),
    isDeparted: z.boolean(),
  }),
  createdAt: z.string(),
  content: z.string(),
  isEdited: z.boolean(),
  isMine: z.boolean(),
  canEdit: z.boolean(),
  canDelete: z.boolean(),
});

export const commentListSchema = z.object({
  comments: z.array(commentSchema),
  totalCount: z.number().int().nonnegative(),
  nextCursor: z.number().int().positive().nullable(),
  hasNext: z.boolean(),
});

export type Comment = z.infer<typeof commentSchema>;
export type CommentListResponse = z.infer<typeof commentListSchema>;

export function isCommentListResponse(data: unknown): data is CommentListResponse {
  return commentListSchema.safeParse(data).success;
}

import { z } from 'zod';
import type { Attachment } from './types';

export const attachmentSchema = z.object({
  fileId: z.number().int().positive(),
  originalFileName: z.string(),
  contentType: z.string(),
  size: z.number().int().nonnegative(),
  sortOrder: z.number().int().nonnegative(),
  imageUrl: z.string().nullable().optional(),
}) satisfies z.ZodType<Attachment>;

export const uploadUrlSchema = z.object({
  fileId: z.number().int().positive(),
  uploadUrl: z.string().url(),
  expiresAt: z.string(),
});

import { Collection } from '@msw/data';
import { z } from 'zod';
import { mockStudies } from './fixtures';

const studySchema = z.object({
  id: z.number(),
  isPublic: z.boolean().optional(),
  name: z.string(),
  description: z.string().nullable(),
  inviteLink: z.string(),
  imageFileId: z.number().nullish(),
  imageUrl: z.string().nullable().optional(),
});

export const studyTable = new Collection({
  schema: studySchema,
});

export type MockStudy = z.infer<typeof studySchema>;

export function createSeedStudies() {
  for (const mockStudy of mockStudies) {
    studyTable.create(mockStudy);
  }
}

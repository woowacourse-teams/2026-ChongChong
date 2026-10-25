import { Collection } from '@msw/data';
import { z } from 'zod';

export const fileTable = new Collection({
  schema: z.object({
    id: z.number(),
    ownerUserId: z.number(),
    originalFileName: z.string(),
    contentType: z.string(),
    size: z.number(),
    uploadedSize: z.number().nullable(),
    body: z
      .custom<ArrayBuffer>(
        (value) => Object.prototype.toString.call(value) === '[object ArrayBuffer]',
      )
      .optional(),
    status: z.enum(['PENDING', 'COMPLETED']),
  }),
});

export const MOCK_UPLOAD_ORIGIN = 'https://uploads.chongchong.test';

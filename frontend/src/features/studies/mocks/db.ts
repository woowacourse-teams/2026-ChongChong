import { Collection } from '@msw/data';
import { z } from 'zod';

export const study = new Collection({
  schema: z.object({
    id: z.number(),
    name: z.string(),
    description: z.string(),
  }),
});

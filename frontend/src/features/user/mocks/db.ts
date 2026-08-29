import z from 'zod';
import { Collection } from '@msw/data';

const userSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string(),
});

export const userTable = new Collection({
  schema: userSchema,
});

type userSchemaType = z.infer<typeof userSchema>;

export const mockUsers = [
  {
    id: 1,
    name: '이든',
    profileImage: 'http://localhost:8000',
  },
  {
    id: 2,
    name: '안톨리니',
    profileImage: 'http://localhost:8000',
  },
  {
    id: 3,
    name: '피즈',
    profileImage: 'http://localhost:8000',
  },
  {
    id: 4,
    name: '디움',
    profileImage: 'http://localhost:8000',
  },
  {
    id: 5,
    name: '바니',
    profileImage: 'http://localhost:8000',
  },
] satisfies userSchemaType[];

export function createSeedUsers() {
  for (const mockUser of mockUsers) {
    userTable.create(mockUser);
  }
}

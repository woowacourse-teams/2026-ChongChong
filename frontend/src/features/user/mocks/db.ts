import z from 'zod';
import { Collection } from '@msw/data';

const userSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
});

export const userTable = new Collection({
  schema: userSchema,
});

export type userSchemaType = z.infer<typeof userSchema>;

const mockProfileImage =
  'https://i.namu.wiki/i/WHQhMwMSdsiSZxtKPb8Ncaq247sNJ6rJTp_rpy3Cc4X3Y4UR2pAAeKuQQnRAa8Spq-twdLRrf5MZscDfCf6ZWw.webp';

export const mockUsers = [
  {
    id: 1,
    name: '이든',
    profileImage: mockProfileImage,
  },
  {
    id: 2,
    name: '안톨리니',
    profileImage: mockProfileImage,
  },
  {
    id: 3,
    name: '피즈',
    profileImage: mockProfileImage,
  },
  {
    id: 4,
    name: '디움',
    profileImage: mockProfileImage,
  },
  {
    id: 5,
    name: '바니',
    profileImage: mockProfileImage,
  },
] satisfies userSchemaType[];

export function createSeedUsers() {
  for (const mockUser of mockUsers) {
    userTable.create(mockUser);
  }
}

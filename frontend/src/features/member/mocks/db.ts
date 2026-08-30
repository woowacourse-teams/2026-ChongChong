import { Collection } from '@msw/data';
import { z } from 'zod';

const memberSchema = z.object({
  id: z.number(),
  studyId: z.number(),
  userId: z.number(),
  name: z.string(),
  profileImage: z.string(),
  role: z.enum(['LEADER', 'MEMBER']),
});

export const memberTable = new Collection({
  schema: memberSchema,
});

type MemberSchemaType = z.infer<typeof memberSchema>;

export const mockMembers = [
  {
    id: 1,
    studyId: 2,
    userId: 1,
    name: '이든',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 2,
    studyId: 1,
    userId: 2,
    name: '안톨리니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 3,
    studyId: 1,
    userId: 3,
    name: '피즈',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 4,
    studyId: 1,
    userId: 4,
    name: '디움',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 5,
    studyId: 1,
    userId: 5,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 6,
    studyId: 2,
    userId: 5,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 7,
    studyId: 1,
    userId: 1,
    name: '이든',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 8,
    studyId: 3,
    userId: 4,
    name: '디움',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 9,
    studyId: 3,
    userId: 5,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
] satisfies MemberSchemaType[];

export function createSeedMembers() {
  for (const mockMember of mockMembers) {
    memberTable.create(mockMember);
  }
}

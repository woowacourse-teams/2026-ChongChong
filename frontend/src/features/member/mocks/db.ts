import { Collection } from '@msw/data';
import { z } from 'zod';

export const memberTable = new Collection({
  schema: z.object({
    id: z.number(),
    studyId: z.number(),
    name: z.string(),
    profileImage: z.string(),
    role: z.string(),
  }),
});

export const mockMembers = [
  {
    id: 1,
    studyId: 1,
    name: '이든',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 2,
    studyId: 1,
    name: '안톨리니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 3,
    studyId: 1,
    name: '피즈',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 4,
    studyId: 1,
    name: '디움',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 5,
    studyId: 1,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 5,
    studyId: 2,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 1,
    studyId: 2,
    name: '이든',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
  {
    id: 4,
    studyId: 3,
    name: '디움',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  },
  {
    id: 5,
    studyId: 3,
    name: '바니',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  },
];

export function createSeedMembers() {
  for (const mockMember of mockMembers) {
    memberTable.create(mockMember);
  }
}

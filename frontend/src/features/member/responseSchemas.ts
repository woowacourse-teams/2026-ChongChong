import z from 'zod';
import { Member, MemberResponse } from './types';

const memberSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string(),
  role: z.enum(['LEADER', 'MEMBER']),
}) satisfies z.ZodType<Member>;

const memberResponseSchema = z.object({
  members: z.array(memberSchema),
}) satisfies z.ZodType<MemberResponse>;

export function isMemberResponse(data: unknown): data is MemberResponse {
  return memberResponseSchema.safeParse(data).success;
}

import z from 'zod';
import {
  Study,
  Role,
  LeaderStudyDetail,
  MemberStudyDetail,
  StudyDetail,
  UserStudyInfo,
  InviteLink,
  StudyJoinInfo,
  PublicStudyListResponse,
} from './types';

const studyItemSchema = z.object({
  id: z.number(),
  role: z.enum(['LEADER', 'MEMBER']),
  name: z.string(),
  description: z.string().nullable(),
  imageUrl: z.string().nullable().optional(),
  memberCount: z.number(),
  noticeCount: z.number(),
  assignmentCount: z.number(),
}) satisfies z.ZodType<Study>;

const studySchema = z.object({
  studies: z.array(studyItemSchema),
}) satisfies z.ZodType<{ studies: Study[] }>;

const studyProgressItemSchema = z.object({
  id: z.number(),
  title: z.string(),
  completeCount: z.number(),
  memberCount: z.number(),
});

const leaderStudyDetailSchema = z.object({
  notices: z.object({
    count: z.number(),
    items: z.array(studyProgressItemSchema),
  }),
  assignments: z.object({
    count: z.number(),
    items: z.array(studyProgressItemSchema),
  }),
}) satisfies z.ZodType<LeaderStudyDetail>;

const memberStudyDetailSchema = z.object({
  totalCount: z.number(),
  notices: z.object({
    items: z.array(
      z.object({
        id: z.number(),
        title: z.string(),
      }),
    ),
  }),
  assignments: z.object({
    items: z.array(
      z.object({
        id: z.number(),
        title: z.string(),
      }),
    ),
  }),
}) satisfies z.ZodType<MemberStudyDetail>;

const studyInfoSchema = z.object({
  studyName: z.string(),
  description: z.string().nullable(),
  imageUrl: z.string().nullable().optional(),
  role: z.enum(['LEADER', 'MEMBER']),
  userName: z.string(),
}) satisfies z.ZodType<UserStudyInfo>;

const inviteLinkSchema = z.object({
  inviteLink: z.url(),
}) satisfies z.ZodType<InviteLink>;

const createStudySchema = z.object({
  studyId: z.number(),
});

const studyJoinInfoSchema = z.object({
  studyId: z.number().int().positive(),
  studyName: z.string(),
  description: z.string().nullable(),
  imageUrl: z.string().nullable(),
  memberCount: z.number().int().nonnegative(),
}) satisfies z.ZodType<StudyJoinInfo>;

const publicStudyListSchema = z.object({
  studies: z.array(studyJoinInfoSchema),
  hasNext: z.boolean(),
  nextCursor: z.number().int().positive().nullable(),
}) satisfies z.ZodType<PublicStudyListResponse>;

export function isPublicStudyListResponse(data: unknown): data is PublicStudyListResponse {
  return publicStudyListSchema.safeParse(data).success;
}

export function isStudyJoinInfoResponse(data: unknown): data is StudyJoinInfo {
  return studyJoinInfoSchema.safeParse(data).success;
}

export function isStudyResponse(data: unknown): data is { studies: Study[] } {
  return studySchema.safeParse(data).success;
}

export function isStudyDetailResponse<R extends Role>(
  data: unknown,
  role: R,
): data is StudyDetail<R> {
  const schema = role === 'LEADER' ? leaderStudyDetailSchema : memberStudyDetailSchema;

  return schema.safeParse(data).success;
}

export function isStudyInfoResponse(data: unknown): data is UserStudyInfo {
  return studyInfoSchema.safeParse(data).success;
}

export function isInviteLinkResponse(data: unknown): data is InviteLink {
  return inviteLinkSchema.safeParse(data).success;
}

export function isCreateStudyResponse(data: unknown): data is { studyId: number } {
  return createStudySchema.safeParse(data).success;
}

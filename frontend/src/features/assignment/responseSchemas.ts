import { z } from 'zod';
import type {
  AssignmentDetail,
  AssignmentListResponse,
  CreateAssignmentResponse,
  SubmissionListResponse,
  Submission,
  Assignment,
  SubmissionDetail,
  Member,
  AssignmentSubmitStatus,
  CreateSubmissionResponse,
  UserAssignmentSubmitDetail,
  LeaderAssignmentSummary,
  MemberAssignmentSummary,
  IncompleteMember,
} from './types';

const submissionStatusSchema = z.enum(['NOT_ASSIGNED', 'NOT_SUBMITTED', 'SUBMITTED']);

const assignmentSummaryBaseSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  submissionMethod: z.string(),
  closeAt: z.string(),
  submissionStatus: submissionStatusSchema,
});

const leaderAssignmentSummarySchema = assignmentSummaryBaseSchema.extend({
  memberCount: z.number(),
  completeCount: z.number(),
  remindAt: z.string().optional(),
  isComplete: z.boolean(),
}) satisfies z.ZodType<LeaderAssignmentSummary>;

const memberAssignmentSummarySchema =
  assignmentSummaryBaseSchema satisfies z.ZodType<MemberAssignmentSummary>;

const assignmentSchema = z.union([
  leaderAssignmentSummarySchema,
  memberAssignmentSummarySchema,
]) satisfies z.ZodType<Assignment>;

const assignmentListResponseSchema = z.object({
  nextCursor: z.number().nullable(),
  hasNext: z.boolean(),
  assignments: z.array(assignmentSchema),
}) satisfies z.ZodType<AssignmentListResponse>;

const createAssignmentSchema = z.object({
  assignmentId: z.number(),
}) satisfies z.ZodType<CreateAssignmentResponse>;

const createSubmissionSchema = z.object({
  submissionId: z.number(),
}) satisfies z.ZodType<CreateSubmissionResponse>;

const submissionSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
  createdAt: z.string(),
}) satisfies z.ZodType<Submission>;

const submissionListResponseSchema = z.object({
  submissions: z.array(submissionSchema),
}) satisfies z.ZodType<SubmissionListResponse>;

const assignmentDetailSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  submissionMethod: z.string(),
  closeAt: z.string(),
  submissionTarget: z.enum(['MEMBERS_ONLY', 'MEMBERS_AND_LEADER']),
}) satisfies z.ZodType<AssignmentDetail>;

const submissionDetailSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
  createdAt: z.string(),
  content: z.string(),
  link: z.string().nullable(),
}) satisfies z.ZodType<SubmissionDetail>;

const memberSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
}) satisfies z.ZodType<Member>;

const incompleteMemberSchema = memberSchema.extend({
  lastRemindAt: z.string().nullable(),
}) satisfies z.ZodType<IncompleteMember>;

const assignmentSubmitStatusSchema = z.object({
  id: z.number(),
  memberCount: z.number(),
  completeCount: z.number(),
  incompleteCount: z.number(),
  remindAt: z.string().nullable(),
  completeMembers: z.array(memberSchema),
  incompleteMembers: z.array(incompleteMemberSchema),
}) satisfies z.ZodType<AssignmentSubmitStatus>;

const userAssignmentSubmitDetailSchema = z.discriminatedUnion('submissionStatus', [
  z.object({
    submissionStatus: z.literal('SUBMITTED'),
    submissionId: z.number(),
    createdAt: z.string(),
    content: z.string().optional(),
    link: z.string().optional(),
  }),
  z.object({
    submissionStatus: z.literal('NOT_SUBMITTED'),
    submissionId: z.number(),
  }),
  z.object({
    submissionStatus: z.literal('NOT_ASSIGNED'),
  }),
]) satisfies z.ZodType<UserAssignmentSubmitDetail>;

export function isAssignmentListResponse(data: unknown): data is AssignmentListResponse {
  return assignmentListResponseSchema.safeParse(data).success;
}

export function isCreateAssignmentResponse(data: unknown): data is CreateAssignmentResponse {
  return createAssignmentSchema.safeParse(data).success;
}

export function isCreateSubmissionResponse(data: unknown): data is CreateSubmissionResponse {
  return createSubmissionSchema.safeParse(data).success;
}

export function isSubmissionListResponse(data: unknown): data is SubmissionListResponse {
  return submissionListResponseSchema.safeParse(data).success;
}

export function isAssignmentDetailResponse(data: unknown): data is AssignmentDetail {
  return assignmentDetailSchema.safeParse(data).success;
}

export function isSubmissionDetailResponse(data: unknown): data is SubmissionDetail {
  return submissionDetailSchema.safeParse(data).success;
}

export function isAssignmentSubmitStatusResponse(data: unknown): data is AssignmentSubmitStatus {
  return assignmentSubmitStatusSchema.safeParse(data).success;
}

export function isUserAssignmentSubmitDetailResponse(
  data: unknown,
): data is UserAssignmentSubmitDetail {
  return userAssignmentSubmitDetailSchema.safeParse(data).success;
}

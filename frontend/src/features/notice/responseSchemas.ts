import z from 'zod';
import {
  CreateNoticeResponse,
  Notice,
  NoticeListResponse,
  Member,
  UnreadNoticeMember,
  NoticeReadStatus,
  NoticeDetail,
  MemberReadStatus,
  UpdateNoticeReadResponse,
  LeaderNoticeSummary,
  MemberNoticeSummary,
} from './types';

const noticeSummaryBaseSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  createdAt: z.string(),
});

const leaderNoticeSummarySchema = noticeSummaryBaseSchema.extend({
  recipientCount: z.number(),
  readRecipientCount: z.number(),
  remindAt: z.string().optional(),
  isComplete: z.boolean(),
}) satisfies z.ZodType<LeaderNoticeSummary>;

const memberNoticeSummarySchema = noticeSummaryBaseSchema.extend({
  readStatus: z.enum(['NOT_ASSIGNED', 'UNREAD', 'READ']),
}) satisfies z.ZodType<MemberNoticeSummary>;

const noticeSchema = z.union([
  leaderNoticeSummarySchema,
  memberNoticeSummarySchema,
]) satisfies z.ZodType<Notice>;

const noticeListSchema = z.object({
  nextCursor: z.number().nullable(),
  hasNext: z.boolean(),
  notices: z.array(noticeSchema),
}) satisfies z.ZodType<NoticeListResponse>;

const createNoticeSchema = z.object({
  noticeId: z.number(),
}) satisfies z.ZodType<CreateNoticeResponse>;

const noticeDetailSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  createdAt: z.string(),
}) satisfies z.ZodType<NoticeDetail>;

const memberSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
}) satisfies z.ZodType<Member>;

const unreadNoticeMemberSchema = memberSchema.extend({
  lastRemindAt: z.string().nullable(),
}) satisfies z.ZodType<UnreadNoticeMember>;

const noticeReadStatusSchema = z.object({
  id: z.number(),
  memberCount: z.number(),
  readCount: z.number(),
  unreadCount: z.number(),
  remindAt: z.string().nullable(),
  readMembers: z.array(memberSchema),
  unreadMembers: z.array(unreadNoticeMemberSchema),
}) satisfies z.ZodType<NoticeReadStatus>;

const myReadStatusSchema = z.discriminatedUnion('readStatus', [
  z.object({
    readStatus: z.literal('READ'),
    readAt: z.string(),
  }),
  z.object({
    readStatus: z.literal('UNREAD'),
  }),
  z.object({
    readStatus: z.literal('NOT_ASSIGNED'),
  }),
]) satisfies z.ZodType<MemberReadStatus>;

const updateReadStatusSchema = z.object({
  readAt: z.string(),
}) satisfies z.ZodType<UpdateNoticeReadResponse>;

export function isNoticeListResponse(data: unknown): data is NoticeListResponse {
  return noticeListSchema.safeParse(data).success;
}

export function isCreateNoticeResponse(data: unknown): data is CreateNoticeResponse {
  return createNoticeSchema.safeParse(data).success;
}

export function isNoticeDetailResponse(data: unknown): data is NoticeDetail {
  return noticeDetailSchema.safeParse(data).success;
}

export function isUpdateReadStatusResponse(data: unknown): data is UpdateNoticeReadResponse {
  return updateReadStatusSchema.safeParse(data).success;
}

export function isNoticeReadStatusResponse(data: unknown): data is NoticeReadStatus {
  return noticeReadStatusSchema.safeParse(data).success;
}

export function isMyReadStatusResponse(data: unknown): data is MemberReadStatus {
  return myReadStatusSchema.safeParse(data).success;
}

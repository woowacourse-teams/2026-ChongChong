import z from 'zod';
import {
  CreateNoticeResponse,
  Notice,
  NoticeListResponse,
  Member,
  NoticeReadStatus,
  NoticeDetail,
  MemberReadStatus,
  UpdateNoticeReadResponse,
} from './types';

const noticeSchema = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
  createdAt: z.string(),
  recipientCount: z.number().optional(),
  readRecipientCount: z.number().optional(),
  remindAt: z.string().nullish(),
  isComplete: z.boolean(),
}) satisfies z.ZodType<Notice>;

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
  writer: z.string(),
  profileImageUrl: z.string().nullable(),
  content: z.string(),
  createdAt: z.string(),
}) satisfies z.ZodType<NoticeDetail>;

const memberSchema = z.object({
  id: z.number(),
  name: z.string(),
  profileImage: z.string().nullable(),
  lastRemindAt: z.string().nullish(),
}) satisfies z.ZodType<Member>;

const noticeReadStatusSchema = z.object({
  id: z.number(),
  memberCount: z.number(),
  readCount: z.number(),
  unreadCount: z.number(),
  remindAt: z.string().nullish(),
  readMembers: z.array(memberSchema),
  unreadMembers: z.array(memberSchema),
}) satisfies z.ZodType<NoticeReadStatus>;

const myReadStatusSchema = z.object({
  isRead: z.boolean(),
  readAt: z.string().nullable(),
}) satisfies z.ZodType<MemberReadStatus>;

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

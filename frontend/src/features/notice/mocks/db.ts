import { Collection } from '@msw/data';
import { z } from 'zod';

const noticeSchema = z.object({
  id: z.number(),
  studyId: z.number(),
  title: z.string(),
  content: z.string(),
  createdAt: z.string(),
});

export const noticeTable = new Collection({ schema: noticeSchema });

export type NoticeSchemaType = z.infer<typeof noticeSchema>;

const noticeRecipientSchema = z.object({
  id: z.number(),
  noticeId: z.number(),
  memberId: z.number(),
  readAt: z.string().nullable(),
  lastRemindAt: z.string().nullable(),
});

export const noticeRecipientTable = new Collection({ schema: noticeRecipientSchema });

export type NoticeRecipientSchemaType = z.infer<typeof noticeRecipientSchema>;

export const mockNotices = [
  {
    id: 12,
    studyId: 2,
    title: '농구 스터디 첫 모임 안내',
    content: '이번 주 토요일 오후 2시에 체육관 입구에서 만나요.',
    createdAt: '2026-08-01T10:00:00',
  },
  {
    id: 13,
    studyId: 2,
    title: '준비물 안내',
    content: '운동화와 개인 물병을 꼭 챙겨주세요.',
    createdAt: '2026-08-03T12:30:00',
  },
  {
    id: 3,
    studyId: 1,
    title: '1주 차 스터디 자료',
    content: '스프링 컨테이너 공식 문서를 읽고 참석해주세요.',
    createdAt: '2026-08-04T09:00:00',
  },
  {
    id: 4,
    studyId: 1,
    title: '온라인 진행 안내',
    content: '이번 주 모임은 온라인으로 진행합니다.',
    createdAt: '2026-08-06T18:00:00',
  },
  {
    id: 5,
    studyId: 1,
    title: '질문 정리 방법',
    content: '질문은 모임 전날까지 노션 페이지에 남겨주세요.',
    createdAt: '2026-08-08T11:20:00',
  },
  {
    id: 6,
    studyId: 1,
    title: '2주 차 일정 변경',
    content: '모임 시간이 오후 7시에서 오후 8시로 변경됐습니다.',
    createdAt: '2026-08-10T14:15:00',
  },
  {
    id: 7,
    studyId: 1,
    title: '코드 리뷰 규칙',
    content: '리뷰 요청 전 테스트와 포맷 검사를 실행해주세요.',
    createdAt: '2026-08-12T16:40:00',
  },
  {
    id: 8,
    studyId: 1,
    title: '발표 순서 안내',
    content: '이번 주 발표 순서는 안톨리니, 피즈, 디움 순서입니다.',
    createdAt: '2026-08-14T08:30:00',
  },
  {
    id: 9,
    studyId: 1,
    title: '회고 작성 안내',
    content: '모임 종료 후 이번 주 회고를 작성해주세요.',
    createdAt: '2026-08-16T20:00:00',
  },
  {
    id: 10,
    studyId: 7,
    title: '꼭 확인해야 하는 공지',
    content: '이 공지는 읽음 처리 동작을 확인하기 위한 공지입니다.',
    createdAt: '2026-08-18T10:00:00',
  },
  {
    id: 1,
    studyId: 1,
    title: '가입 전 작성된 공지',
    content: '신규 멤버에게는 보이지만 확인 대상에는 포함되지 않습니다.',
    createdAt: '2026-07-01T10:00:00',
  },
] satisfies NoticeSchemaType[];

export const mockNoticeRecipients = [
  { id: 1, noticeId: 12, memberId: 6, readAt: null, lastRemindAt: null },
  { id: 2, noticeId: 13, memberId: 6, readAt: '2026-08-03T13:00:00', lastRemindAt: null },
  { id: 3, noticeId: 3, memberId: 2, readAt: '2026-08-04T10:00:00', lastRemindAt: null },
  { id: 4, noticeId: 3, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 5, noticeId: 3, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 6, noticeId: 3, memberId: 7, readAt: '2026-08-04T11:00:00', lastRemindAt: null },
  { id: 7, noticeId: 4, memberId: 2, readAt: null, lastRemindAt: null },
  { id: 8, noticeId: 4, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 9, noticeId: 4, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 10, noticeId: 4, memberId: 7, readAt: null, lastRemindAt: null },
  { id: 11, noticeId: 5, memberId: 2, readAt: '2026-08-08T12:00:00', lastRemindAt: null },
  { id: 12, noticeId: 5, memberId: 3, readAt: '2026-08-08T12:10:00', lastRemindAt: null },
  { id: 13, noticeId: 5, memberId: 4, readAt: '2026-08-08T12:20:00', lastRemindAt: null },
  { id: 14, noticeId: 5, memberId: 7, readAt: '2026-08-08T12:30:00', lastRemindAt: null },
  { id: 15, noticeId: 6, memberId: 2, readAt: null, lastRemindAt: null },
  { id: 16, noticeId: 6, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 17, noticeId: 6, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 18, noticeId: 6, memberId: 7, readAt: null, lastRemindAt: null },
  { id: 19, noticeId: 7, memberId: 2, readAt: null, lastRemindAt: null },
  { id: 20, noticeId: 7, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 21, noticeId: 7, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 22, noticeId: 7, memberId: 7, readAt: '2026-08-12T18:00:00', lastRemindAt: null },
  { id: 23, noticeId: 8, memberId: 2, readAt: null, lastRemindAt: null },
  { id: 24, noticeId: 8, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 25, noticeId: 8, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 26, noticeId: 8, memberId: 7, readAt: null, lastRemindAt: null },
  { id: 27, noticeId: 9, memberId: 2, readAt: null, lastRemindAt: null },
  { id: 28, noticeId: 9, memberId: 3, readAt: null, lastRemindAt: null },
  { id: 29, noticeId: 9, memberId: 4, readAt: null, lastRemindAt: null },
  { id: 30, noticeId: 9, memberId: 7, readAt: null, lastRemindAt: null },
  { id: 31, noticeId: 10, memberId: 12, readAt: null, lastRemindAt: null },
] satisfies NoticeRecipientSchemaType[];

export function createSeedNotices() {
  for (const notice of mockNotices) {
    noticeTable.create(notice);
  }

  for (const recipient of mockNoticeRecipients) {
    noticeRecipientTable.create(recipient);
  }
}

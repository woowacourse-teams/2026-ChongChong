import { Collection } from '@msw/data';
import z from 'zod';

const notificationSchema = z.object({
  id: z.number(),
  userId: z.number(),
  title: z.string(),
  body: z.string(),
  type: z.enum(['REMIND', 'NEW']),
  resourceType: z.enum(['NOTICE', 'ASSIGNMENT', 'ASSIGNMENT_SUBMISSION']),
  resourceId: z.number(),
  deepLink: z.string(),
  isRead: z.boolean(),
  createdAt: z.string(),
});

export const notificationTable = new Collection({
  schema: notificationSchema,
});

export type NotificationSchemaType = z.infer<typeof notificationSchema>;

export const mockNotifications = [
  {
    id: 1,
    userId: 1,
    title: '[객체지향 스터디] 새 공지',
    body: '4주차부터 선택 미션이 추가됩니다',
    type: 'NEW',
    resourceType: 'NOTICE',
    resourceId: 10,
    deepLink: '/studies/2/notices/10',
    isRead: false,
    createdAt: '2026-09-21T12:00:00',
  },
  {
    id: 2,
    userId: 1,
    title: '[객체지향 스터디] 새 공지 리마인드',
    body: '4주차부터 선택 미션이 추가됩니다',
    type: 'REMIND',
    resourceType: 'NOTICE',
    resourceId: 10,
    deepLink: '/studies/2/notices/10',
    isRead: true,
    createdAt: '2026-09-21T12:00:00',
  },
  {
    id: 3,
    userId: 1,
    title: '[객체지향 스터디] 새 과제',
    body: '객체지향의 관점으로 리팩토링을 하고 결과를 제출해 주세요',
    type: 'NEW',
    resourceType: 'ASSIGNMENT',
    resourceId: 11,
    deepLink: '/studies/2/assignments/11',
    isRead: false,
    createdAt: '2026-09-21T12:00:00',
  },
  {
    id: 4,
    userId: 1,
    title: '[객체지향 스터디] 새 과제 리마인드',
    body: '객체지향의 관점으로 리팩토링을 하고 결과를 제출해 주세요',
    type: 'NEW',
    resourceType: 'ASSIGNMENT',
    resourceId: 11,
    deepLink: '/studies/2/assignments/11',
    isRead: true,
    createdAt: '2026-09-21T12:00:00',
  },
] satisfies NotificationSchemaType[];

export function createSeedNotification() {
  for (const mockNotification of mockNotifications) {
    notificationTable.create(mockNotification);
  }
}

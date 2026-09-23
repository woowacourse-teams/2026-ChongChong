import z from 'zod';
import { Notification } from './types';

const notificationSchema = z.object({
  id: z.number(),
  title: z.string(),
  body: z.string(),
  type: z.enum(['REMIND', 'CREATED', 'SUBMITTED']),
  resourceType: z.enum(['NOTICE', 'ASSIGNMENT', 'ASSIGNMENT_SUBMISSION']),
  resourceId: z.number(),
  deepLink: z.string(),
  isRead: z.boolean(),
  createdAt: z.string(),
});

const notificationListSchema = z.array(notificationSchema);

export function isNotificationResponse(data: unknown): data is Notification[] {
  return notificationListSchema.safeParse(data).success;
}

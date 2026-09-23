type NotificationType = 'REMIND' | 'CREATED' | 'SUBMITTED';

type NotificationResourceType = 'NOTICE' | 'ASSIGNMENT' | 'ASSIGNMENT_SUBMISSION';

export interface Notification {
  id: number;
  title: string;
  body: string;
  type: NotificationType;
  resourceType: NotificationResourceType;
  resourceId: number;
  deepLink: string;
  isRead: boolean;
  createdAt: string;
}

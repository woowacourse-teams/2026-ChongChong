import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';
import { useEntryScenario } from '../entry/EntryProvider';
import { useNotices } from '../notices/NoticeProvider';
import type { ActivityNotification, StudyActivity } from './fixtures';
import { activityFixtures, notificationFixtures } from './fixtures';

type ActivityState = {
  readonly activities: readonly StudyActivity[];
  readonly notifications: readonly ActivityNotification[];
  readonly unreadCount: number;
  readonly markActivityRead: (studyId: string, activityId: string) => void;
  readonly markNotificationRead: (notificationId: string) => void;
  readonly resetActivity: () => void;
};

const ActivityContext = createContext<ActivityState | null>(null);

export function ActivityProvider({ children }: PropsWithChildren) {
  const { studies, selectedStudy } = useEntryScenario();
  const { notices, getNotices, markRead, resetNotices } = useNotices();
  const [readNotifications, setReadNotifications] = useState<readonly string[]>(
    [],
  );
  const activities: readonly StudyActivity[] = selectedStudy
    ? [
        ...notices.map(
          (notice): StudyActivity => ({
            id: notice.id,
            kind: 'notice',
            title: notice.title,
            body: notice.body,
            readCount: notice.recipients.filter((item) => item.readAt).length,
            totalCount: notice.recipients.length,
            read: !notice.recipients.some(
              (item) => item.id === 'self' && !item.readAt,
            ),
          }),
        ),
        ...activityFixtures.filter(
          (item) => item.kind === 'assignment' && selectedStudy.assignments > 0,
        ),
      ]
    : [];
  const notifications = notificationFixtures
    .filter(
      (notification) =>
        studies.some((study) => study.id === notification.studyId) &&
        getNotices(notification.studyId).some(
          (notice) => notice.id === notification.activityId,
        ),
    )
    .map((notification) => ({
      ...notification,
      read: notification.read || readNotifications.includes(notification.id),
    }));
  const markActivityRead = (studyId: string, activityId: string) => {
    if (studyId === selectedStudy?.id) markRead(activityId);
  };
  const markNotificationRead = (notificationId: string) => {
    setReadNotifications((current) =>
      current.includes(notificationId) ? current : [...current, notificationId],
    );
  };
  const resetActivity = () => {
    resetNotices();
    setReadNotifications([]);
  };
  return (
    <ActivityContext
      value={{
        activities,
        notifications,
        unreadCount: notifications.filter((notification) => !notification.read)
          .length,
        markActivityRead,
        markNotificationRead,
        resetActivity,
      }}
    >
      {children}
    </ActivityContext>
  );
}

export function useActivity() {
  const value = useContext(ActivityContext);
  if (!value) throw new MissingActivityProviderError();
  return value;
}

class MissingActivityProviderError extends Error {
  constructor() {
    super('ActivityProvider 안에서 사용해야 합니다.');
    this.name = 'MissingActivityProviderError';
  }
}

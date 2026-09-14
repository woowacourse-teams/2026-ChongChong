import type { PropsWithChildren } from 'react';
import { createContext, useContext, useState } from 'react';
import { useEntryScenario } from '../entry/EntryProvider';
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
  const [readActivities, setReadActivities] = useState<readonly string[]>([]);
  const [readNotifications, setReadNotifications] = useState<readonly string[]>(
    [],
  );
  const activities = selectedStudy
    ? activityFixtures
        .filter((activity) =>
          activity.kind === 'notice'
            ? selectedStudy.notices > 0
            : selectedStudy.assignments > 0,
        )
        .map((activity) => {
          const opened = readActivities.includes(
            `${selectedStudy.id}/${activity.id}`,
          );
          return {
            ...activity,
            read: activity.read || opened,
            readCount:
              activity.kind === 'notice' && !activity.read && opened
                ? activity.readCount + 1
                : activity.readCount,
          };
        })
    : [];
  const notifications = notificationFixtures
    .filter((notification) =>
      studies.some((study) => study.id === notification.studyId),
    )
    .map((notification) => ({
      ...notification,
      read: notification.read || readNotifications.includes(notification.id),
    }));
  const markActivityRead = (studyId: string, activityId: string) => {
    const key = `${studyId}/${activityId}`;
    setReadActivities((current) =>
      current.includes(key) ? current : [...current, key],
    );
  };
  const markNotificationRead = (notificationId: string) => {
    setReadNotifications((current) =>
      current.includes(notificationId) ? current : [...current, notificationId],
    );
  };
  const resetActivity = () => {
    setReadActivities([]);
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

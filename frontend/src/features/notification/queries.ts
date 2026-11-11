import { queryOptions } from '@tanstack/react-query';
import { fetchNotifications, getWebPushSubscriptionStatus } from './api';
import { hasLocalPushSubscription } from './localPush';

const notificationQueries = {
  pushStatus: (installationId: string) =>
    queryOptions({
      queryKey: ['web-push-subscription', installationId],
      queryFn: async () => {
        const [status, hasLocalSubscription] = await Promise.all([
          getWebPushSubscriptionStatus(installationId),
          hasLocalPushSubscription(),
        ]);

        return { ...status, hasLocalSubscription };
      },
      staleTime: 0,
      gcTime: 0,
      retry: false,
      refetchOnMount: 'always',
    }),
  all: () => ['notifications'],
  notification: () => [...notificationQueries.all()],
  lists: () => [...notificationQueries.notification(), 'notifications'],
  list: () =>
    queryOptions({
      queryKey: notificationQueries.lists(),
      queryFn: () => fetchNotifications(),
    }),
};

export default notificationQueries;

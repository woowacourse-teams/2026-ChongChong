import { queryOptions } from '@tanstack/react-query';
import { fetchNotifications, getWebPushSubscriptionStatus } from './api';

const notificationQueries = {
  pushStatus: (installationId: string) =>
    queryOptions({
      queryKey: ['web-push-subscription', installationId],
      queryFn: () => getWebPushSubscriptionStatus(installationId),
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

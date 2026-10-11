import { queryOptions } from '@tanstack/react-query';
import { fetchNotifications } from './api';

const notificationQueries = {
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

import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/notifications',
    lazy: async () => ({
      Component: (await import('./pages/NotificationListPage')).default,
    }),
  },
];

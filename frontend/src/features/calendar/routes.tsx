import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/studies/:studyId/calendar',
    lazy: async () => ({
      Component: (await import('./pages/StudyCalendarPage')).default,
    }),
  },
];

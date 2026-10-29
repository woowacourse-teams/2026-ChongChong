import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/studies/:studyId/members',
    lazy: async () => ({
      Component: (await import('./pages/MemberListPage')).default,
    }),
  },
];

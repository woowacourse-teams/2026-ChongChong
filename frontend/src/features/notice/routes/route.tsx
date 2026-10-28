import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/studies/:studyId/notices',
    lazy: async () => ({
      Component: (await import('../pages/NoticeListPage')).default,
    }),
  },
  {
    path: '/studies/:studyId/notices/create',
    lazy: async () => ({
      Component: (await import('../pages/CreateNoticePage')).default,
    }),
  },
  {
    path: '/studies/:studyId/notices/:noticeId',
    lazy: async () => ({
      Component: (await import('../pages/NoticeDetailPage')).default,
    }),
  },
  {
    path: '/studies/:studyId/notices/:noticeId/edit',
    lazy: async () => ({
      Component: (await import('../pages/EditNoticePage')).default,
    }),
  },
];

import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    lazy: async () => ({
      Component: (await import('./pages/MyStudiesPage')).default,
    }),
  },
  {
    path: '/studies/new',
    lazy: async () => ({
      Component: (await import('./pages/CreateStudyPage')).default,
    }),
  },
  {
    path: '/studies/:studyId',
    lazy: async () => ({
      Component: (await import('./pages/StudyDetailPage')).default,
    }),
  },
  {
    path: '/studies/join',
    lazy: async () => ({
      Component: (await import('./pages/StudyJoinPage')).default,
    }),
  },
  {
    path: '/studies/:studyId/management',
    lazy: async () => ({
      Component: (await import('./pages/StudyManagementPage')).default,
    }),
  },
  {
    path: '/studies/:studyId/edit',
    lazy: async () => ({
      Component: (await import('./pages/StudyEditPage')).default,
    }),
  },
];

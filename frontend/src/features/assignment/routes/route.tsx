import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: 'studies/:studyId/assignments',
    lazy: async () => ({
      Component: (await import('../pages/AssignmentListPage')).default,
    }),
  },
  {
    path: 'studies/:studyId/assignments/:assignmentId',
    lazy: async () => ({
      Component: (await import('../pages/AssignmentDetailPage')).default,
    }),
  },
  {
    path: 'studies/:studyId/assignments/:assignmentId/submissions/:submissionId',
    lazy: async () => ({
      Component: (await import('../pages/AssignmentSubmissionDetailPage')).default,
    }),
  },
  {
    path: 'studies/:studyId/assignments/create',
    lazy: async () => ({
      Component: (await import('../pages/CreateAssignmentPage')).default,
    }),
  },
  {
    path: 'studies/:studyId/assignments/:assignmentId/edit',
    lazy: async () => ({
      Component: (await import('../pages/EditAssignmentPage')).default,
    }),
  },
];

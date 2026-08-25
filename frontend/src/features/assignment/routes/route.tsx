import type { RouteObject } from 'react-router';
import AssignmentListPage from '../pages/AssignmentListPage';
import AssignmentDetailPage from '../pages/AssignmentDetailPage';
import AssignmentSubmissionDetailPage from '../pages/AssignmentSubmissionDetailPage';

export const routes: RouteObject[] = [
  {
    path: 'studies/:studyId/assignments',
    element: <AssignmentListPage />,
  },
  {
    path: 'studies/:studyId/assignments/:assignmentId',
    element: <AssignmentDetailPage />,
  },
  {
    path: 'studies/:studyId/assignments/:assignmentId/submissions/:submissionId',
    element: <AssignmentSubmissionDetailPage />,
  },
];

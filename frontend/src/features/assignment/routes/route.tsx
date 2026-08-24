import type { RouteObject } from 'react-router';
import AssignmentListPage from '../pages/AssignmentListPage';

export const routes: RouteObject[] = [
  {
    path: 'studies/:studyId/assignments',
    element: <AssignmentListPage />,
  },
];

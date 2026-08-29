import { RouteObject } from 'react-router';
import MemberListPage from './pages/MemberListPage';

export const routes: RouteObject[] = [
  {
    path: '/studies/:studyId/members',
    element: <MemberListPage />,
  },
];

import { RouteObject } from 'react-router';
import StudyListPage from './StudyListPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <StudyListPage />,
  },
];

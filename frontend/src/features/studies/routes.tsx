import { RouteObject } from 'react-router';
import MyStudiesPage from './pages/MyStudiesPage';
import NewStudyPage from './pages/NewStudyPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <MyStudiesPage />,
  },
  {
    path: '/studies/new',
    element: <NewStudyPage />,
  },
];

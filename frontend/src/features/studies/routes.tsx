import { RouteObject } from 'react-router';
import MyStudiesPage from './pages/MyStudiesPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <MyStudiesPage />,
  },
];

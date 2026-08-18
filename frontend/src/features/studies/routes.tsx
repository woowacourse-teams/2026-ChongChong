import { RouteObject } from 'react-router';
import MyStudiesPage from './MyStudiesPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <MyStudiesPage />,
  },
];

import { RouteObject } from 'react-router';
import NotificationListPage from './pages/NotificationListPage';

export const routes: RouteObject[] = [
  {
    path: '/notifications',
    element: <NotificationListPage />,
  },
];

import { RouteObject } from 'react-router';
import MyPage from './pages/MyPage';

export const routes: RouteObject[] = [
  {
    path: '/studies/mypage',
    element: <MyPage />,
  },
];

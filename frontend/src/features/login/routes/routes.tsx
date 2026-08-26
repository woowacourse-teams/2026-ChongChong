import type { RouteObject } from 'react-router';
import KakaoCallbackPage from '../pages/KakaoCallbackPage';
import LoginPage from '../pages/LoginPage';

export const routes: RouteObject[] = [
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/auth/kakao/callback',
    element: <KakaoCallbackPage />,
  },
];

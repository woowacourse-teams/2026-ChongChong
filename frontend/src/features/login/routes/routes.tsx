import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/login',
    lazy: async () => ({
      Component: (await import('../pages/LoginPage')).default,
    }),
  },
  {
    path: '/auth/kakao/callback',
    lazy: async () => ({
      Component: (await import('../pages/KakaoCallbackPage')).default,
    }),
  },
];

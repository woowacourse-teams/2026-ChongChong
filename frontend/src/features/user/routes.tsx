import type { RouteObject } from 'react-router';

export const routes: RouteObject[] = [
  {
    path: '/studies/mypage',
    lazy: async () => ({
      Component: (await import('./pages/MyPage')).default,
    }),
  },
];

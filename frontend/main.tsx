import ReactDOM from 'react-dom/client';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { StrictMode } from 'react';
import { Global } from '@emotion/react';
import { globalStyles } from './src/styles/global';
import App from './src/App';
import { routes as noticeRoutes } from './src/features/notice/routes/route';
import { routes as studiesRoutes } from './src/features/study/routes';
import { routes as AssignmentRoutes } from './src/features/assignment/routes/route';
import { routes as memberRoutes } from './src/features/member/routes';
import { routes as loginRoutes } from './src/features/login/routes/routes';
import { refreshAccessToken } from './src/features/login/api';
import { PostHogProvider } from '@posthog/react';

const appRoutes = [
  {
    path: '/',
    element: <App />,
  },
  ...studiesRoutes,
  ...noticeRoutes,
  ...AssignmentRoutes,
  ...memberRoutes,
  ...loginRoutes,
];

const root = document.getElementById('root')!;

async function enableMocking() {
  // 개발 환경에서는 MSW를 실행합니다.
  if (process.env.NODE_ENV !== 'development') return;

  const { worker } = await import('./src/mocks/msw-browser');

  return worker.start();
}

const queryClient = new QueryClient();

const publicPaths = new Set(['/login', '/auth/kakao/callback']);

async function restoreSession() {
  if (publicPaths.has(window.location.pathname)) return;

  try {
    await refreshAccessToken();
  } catch {
    window.history.replaceState({}, document.title, '/login');
  }
}

async function bootstrap() {
  await enableMocking();
  await restoreSession();

  const router = createBrowserRouter(appRoutes);

  ReactDOM.createRoot(root).render(
    <StrictMode>
      <PostHogProvider
        apiKey={process.env.POSTHOG_PROJECT_TOKEN!}
        options={{
          api_host: process.env.POSTHOG_HOST,
          defaults: '2026-05-30',
        }}
      >
        <QueryClientProvider client={queryClient}>
          <Global styles={globalStyles} />
          <RouterProvider router={router} />
        </QueryClientProvider>
      </PostHogProvider>
    </StrictMode>,
  );
}

bootstrap();

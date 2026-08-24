import ReactDOM from 'react-dom/client';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { Global } from '@emotion/react';
import { globalStyles } from './src/styles/global';
import App from './src/App';
import { routes as noticeRoutes } from './src/features/notice/routes/route';
import { routes as studiesRoutes } from './src/features/studies/routes';
import { routes as AssignmentRoutes } from './src/features/assignment/routes/route';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  ...studiesRoutes,
  ...noticeRoutes,
  ...AssignmentRoutes,
]);

const root = document.getElementById('root')!;

async function enableMocking() {
  // 개발 환경에서는 MSW를 실행합니다.
  if (process.env.NODE_ENV !== 'development') return;

  const { worker } = await import('./src/mocks/msw-browser');

  return worker.start();
}

const queryClient = new QueryClient();

enableMocking().then(() => {
  ReactDOM.createRoot(root).render(
    <>
      <QueryClientProvider client={queryClient}>
        <Global styles={globalStyles} />
        <RouterProvider router={router} />
      </QueryClientProvider>
    </>,
  );
});

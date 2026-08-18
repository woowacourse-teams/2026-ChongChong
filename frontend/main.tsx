import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { Global } from '@emotion/react';
import { globalStyles } from './src/styles/global';
import App from './src/App';
import NoticeListPage from './src/features/notice/NoticeListPage';
import NoticeDetailPage from './src/features/notice/NoticeDetailPage';
import CreateNoticePage from './src/features/notice/CreateNoticePage';
import EditNoticePage from './src/features/notice/EditNoticePage';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  {
    path: '/studies/:studyId/notices',
    element: <NoticeListPage />,
  },
  {
    path: '/studies/:studyId/notices/create',
    element: <CreateNoticePage />,
  },
  {
    path: '/studies/:studyId/notices/:noticeId',
    element: <NoticeDetailPage />,
  },
  {
    path: '/studies/:studyId/notices/:noticeId/modify',
    element: <EditNoticePage />,
  },
]);

const root = document.getElementById('root')!;

async function enableMocking() {
  // 개발 환경에서는 MSW를 실행합니다.
  if (process.env.NODE_ENV !== 'development') return;

  const { worker } = await import('./src/mocks/msw-browser');

  return worker.start();
}

enableMocking().then(() => {
  ReactDOM.createRoot(root).render(
    <>
      <Global styles={globalStyles} />
      <RouterProvider router={router} />
    </>,
  );
});

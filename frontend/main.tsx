import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { Global } from '@emotion/react';
import { globalStyles } from './src/styles/global';
import App from './src/App';

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
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
      <RouterProvider router={router} />,
    </>,
  );
});

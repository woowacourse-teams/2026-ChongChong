import type { RouteObject } from 'react-router';
import CreateNoticePage from '../pages/CreateNoticePage';
import EditNoticePage from '../pages/EditNoticePage';
import NoticeDetailPage from '../pages/NoticeDetailPage';
import NoticeListPage from '../pages/NoticeListPage';

export const routes: RouteObject[] = [
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
    path: '/studies/:studyId/notices/:noticeId/edit',
    element: <EditNoticePage />,
  },
];

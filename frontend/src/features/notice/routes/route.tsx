import type { RouteObject } from 'react-router';
import CreateNoticePage from '../CreateNoticePage';
import EditNoticePage from '../EditNoticePage';
import NoticeDetailPage from '../NoticeDetailPage';
import NoticeListPage from '../NoticeListPage';

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
    path: '/studies/:studyId/notices/:noticeId/modify',
    element: <EditNoticePage />,
  },
];

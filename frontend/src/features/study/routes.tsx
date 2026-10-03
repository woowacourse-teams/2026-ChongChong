import { RouteObject } from 'react-router';
import MyStudiesPage from './pages/MyStudiesPage';
import CreateStudyPage from './pages/CreateStudyPage';
import StudyDetailPage from './pages/StudyDetailPage';
import StudyJoinPage from './pages/StudyJoinPage';
import StudyManagementPage from './pages/StudyManagementPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <MyStudiesPage />,
  },
  {
    path: '/studies/new',
    element: <CreateStudyPage />,
  },
  {
    path: '/studies/:studyId',
    element: <StudyDetailPage />,
  },
  {
    path: '/studies/join',
    element: <StudyJoinPage />,
  },
  {
    path: '/studies/:studyId/management',
    element: <StudyManagementPage />,
  },
];

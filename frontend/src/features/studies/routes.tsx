import { RouteObject } from 'react-router';
import MyStudiesPage from './pages/MyStudiesPage';
import CreateStudyPage from './pages/CreateStudyPage';
import StudyDetailPage from './pages/StudyDetailPage';

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
];

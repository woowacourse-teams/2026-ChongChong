import { RouteObject } from 'react-router';
import MyStudiesPage from './pages/MyStudiesPage';
import NewStudyPage from './pages/NewStudyPage';
import StudyDetailPage from './pages/StudyDetailPage';

export const routes: RouteObject[] = [
  {
    path: '/studies',
    element: <MyStudiesPage />,
  },
  {
    path: '/studies/new',
    element: <NewStudyPage />,
  },
  {
    path: '/studies/:studyId',
    element: <StudyDetailPage />,
  },
];

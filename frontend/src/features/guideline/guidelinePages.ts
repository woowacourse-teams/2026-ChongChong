import WelcomePage from './components/pages/WelcomePage';
import PwaPage from './components/pages/PwaPage';
import NotificationPage from './components/pages/NotificationPage';
import InvitationPage from './components/pages/InvitationPage';
import CompletePage from './components/pages/CompletePage';
import type { GuidelinePage } from './types';

export const guidelinePages = [
  { id: 'welcome', label: '환영해요', Component: WelcomePage },
  { id: 'pwa', label: '앱 설치', Component: PwaPage },
  { id: 'notification', label: '알림 설정', Component: NotificationPage },
  { id: 'invitation', label: '스터디 참여', Component: InvitationPage },
  { id: 'complete', label: '준비 완료', Component: CompletePage },
] satisfies GuidelinePage[];

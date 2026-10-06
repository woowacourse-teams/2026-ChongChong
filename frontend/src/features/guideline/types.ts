import type { ComponentType } from 'react';
import type { PwaPlatform } from './utils/pwaInstallGuide';

export type NotificationStatus =
  'checking' | 'enabled' | 'disabled' | 'denied' | 'unsupported' | 'error';

export interface GuidelinePwaState {
  platform: PwaPlatform;
  guideOpen: boolean;
  isStandalone: boolean;
  isInstalled: boolean;
  isInstalling: boolean;
  install: () => Promise<void>;
  message: string;
}

export interface GuidelineNotificationState {
  status: NotificationStatus;
  permissionGranted: boolean;
  message: string;
}

export interface GuidelinePageProps {
  onNext: () => void;
  onClose: () => void;
  onJoinStudy: () => void;
  onNotificationSettings: () => void;
  pwa: GuidelinePwaState;
  notification: GuidelineNotificationState;
}

export interface GuidelinePage {
  id: string;
  label: string;
  Component: ComponentType<GuidelinePageProps>;
}

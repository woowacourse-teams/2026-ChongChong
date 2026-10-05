import { useCallback, useEffect, useRef, useState } from 'react';
import { usePwaInstall } from '../../landing/usePwaInstall';
import { PUSH_ENABLED_KEY, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/localPush';
import type { GuidelineNotificationState, GuidelinePwaState, NotificationStatus } from '../types';
import {
  isStandalone,
  notificationMessages,
  readNotificationStatus,
} from '../utils/guidelineEnvironment';
import { getPwaPlatform } from '../utils/pwaInstallGuide';

let installedInSession = false;
window.addEventListener('beforeinstallprompt', () => {
  installedInSession = false;
});
window.addEventListener('appinstalled', () => {
  installedInSession = true;
});

function hasNotificationPermission() {
  return typeof Notification !== 'undefined' && Notification.permission === 'granted';
}

export function useGuidelineEnvironment(): {
  pwa: GuidelinePwaState;
  notification: GuidelineNotificationState;
} {
  const { install: requestInstall, isPrompting } = usePwaInstall();
  const [platform, setPlatform] = useState(getPwaPlatform);
  const [guideOpen, setGuideOpen] = useState(false);
  const [standalone, setStandalone] = useState(isStandalone);
  const [installed, setInstalled] = useState(installedInSession);
  const [installMessage, setInstallMessage] = useState('');
  const [notificationState, setNotificationState] = useState<GuidelineNotificationState>(() => ({
    status: 'checking',
    permissionGranted: hasNotificationPermission(),
    message: notificationMessages.checking,
  }));
  const mounted = useRef(false);
  const checkVersion = useRef(0);

  const updateNotification = useCallback((status: NotificationStatus, message?: string) => {
    setNotificationState({
      status,
      permissionGranted: hasNotificationPermission(),
      message: message ?? notificationMessages[status],
    });
  }, []);

  const refreshNotification = useCallback(async () => {
    const version = ++checkVersion.current;
    const status = await readNotificationStatus();
    if (mounted.current && version === checkVersion.current) updateNotification(status);
  }, [updateNotification]);

  useEffect(() => {
    mounted.current = true;
    const media =
      typeof window.matchMedia === 'function'
        ? window.matchMedia('(display-mode: standalone)')
        : null;
    const refreshNotificationState = () => {
      const permissionGranted = hasNotificationPermission();
      setNotificationState((previous) => ({ ...previous, permissionGranted }));
      void refreshNotification();
    };
    const refreshPlatform = () => setPlatform(getPwaPlatform());
    const refresh = () => {
      refreshPlatform();
      setStandalone(isStandalone());
      setInstalled(installedInSession);
      refreshNotificationState();
    };
    const onInstalled = () => {
      installedInSession = true;
      setInstalled(true);
      setInstallMessage('총총 앱을 추가했어요. 홈 화면의 총총 아이콘으로 열어 주세요.');
      refresh();
    };
    const onInstallAvailable = () => {
      setInstalled(false);
      setInstallMessage('');
    };
    const onVisibility = () => {
      if (document.visibilityState === 'visible') refresh();
    };
    const onStorage = (event: StorageEvent) => {
      if (
        event.key === null ||
        event.key === PUSH_ENABLED_KEY ||
        event.key === PUSH_SUBSCRIPTION_ID_KEY
      ) {
        refreshNotificationState();
      }
    };

    void refreshNotification();
    window.addEventListener('focus', refresh);
    window.addEventListener('resize', refreshPlatform);
    window.addEventListener('beforeinstallprompt', onInstallAvailable);
    window.addEventListener('appinstalled', onInstalled);
    window.addEventListener('storage', onStorage);
    document.addEventListener('visibilitychange', onVisibility);
    media?.addEventListener?.('change', refresh);
    return () => {
      mounted.current = false;
      checkVersion.current += 1;
      window.removeEventListener('focus', refresh);
      window.removeEventListener('resize', refreshPlatform);
      window.removeEventListener('beforeinstallprompt', onInstallAvailable);
      window.removeEventListener('appinstalled', onInstalled);
      window.removeEventListener('storage', onStorage);
      document.removeEventListener('visibilitychange', onVisibility);
      media?.removeEventListener?.('change', refresh);
    };
  }, [refreshNotification]);

  async function install() {
    if (isStandalone() || installedInSession) {
      setStandalone(isStandalone());
      setInstalled(installedInSession);
      return;
    }

    if (platform === 'ios-safari' || platform === 'ios-other') {
      setGuideOpen(true);
      return;
    }

    // The existing install hook needs matchMedia; unsupported browsers use the manual steps.
    const result = typeof window.matchMedia === 'function' ? await requestInstall() : 'unavailable';
    if (!mounted.current) return;
    if (result === 'installed') {
      installedInSession = true;
      setInstalled(true);
    } else if (result === 'accepted') {
      setInstallMessage('설치를 진행 중이에요. 완료되면 총총 앱 아이콘으로 열어 주세요.');
    } else if (result === 'dismissed') {
      setInstallMessage(
        '설치는 나중에도 할 수 있어요. 원할 때 앱으로 시작하기를 다시 눌러 주세요.',
      );
    } else if (result === 'unavailable') {
      setGuideOpen(true);
      setInstallMessage('');
    }
  }

  return {
    pwa: {
      platform,
      guideOpen,
      isStandalone: standalone,
      isInstalled: standalone || installed,
      isInstalling: isPrompting,
      install,
      message: installMessage,
    },
    notification: notificationState,
  };
}

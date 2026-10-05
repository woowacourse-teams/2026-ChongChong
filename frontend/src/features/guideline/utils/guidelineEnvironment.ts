import { PUSH_ENABLED_KEY, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/localPush';
import type { NotificationStatus } from '../types';

export function isStandalone() {
  return (
    (typeof window.matchMedia === 'function' &&
      window.matchMedia('(display-mode: standalone)').matches) ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

export function supportsPush() {
  return (
    'Notification' in window &&
    'PushManager' in window &&
    'serviceWorker' in navigator &&
    typeof navigator.serviceWorker.getRegistration === 'function'
  );
}

export const notificationMessages: Record<NotificationStatus, string> = {
  checking: '이 기기의 알림 설정을 확인하고 있어요.',
  enabled: '이 기기에서 스터디 알림을 받을 준비가 되었어요.',
  disabled: '알림을 켜면 새 공지와 과제 소식을 받을 수 있어요.',
  denied: '알림이 차단되어 있어요. 기기 또는 브라우저 설정에서 허용한 뒤 돌아와 주세요.',
  unsupported:
    '이 환경에서는 푸시 알림을 지원하지 않아요. iPhone은 홈 화면에 추가한 총총 앱에서 열어 주세요.',
  error: '알림 설정을 확인하지 못했어요. 잠시 후 다시 시도해 주세요.',
};

function readNotificationPrerequisites(): NotificationStatus | null {
  if (!supportsPush()) return 'unsupported';
  if (Notification.permission === 'denied') return 'denied';
  if (
    Notification.permission !== 'granted' ||
    localStorage.getItem(PUSH_ENABLED_KEY) !== 'true' ||
    !localStorage.getItem(PUSH_SUBSCRIPTION_ID_KEY)
  ) {
    return 'disabled';
  }
  return null;
}

export async function readNotificationStatus(): Promise<NotificationStatus> {
  try {
    const initialStatus = readNotificationPrerequisites();
    if (initialStatus) return initialStatus;

    const scope = new URL('/push/', window.location.origin).href;
    const registration = await navigator.serviceWorker.getRegistration(scope);
    if (registration?.scope !== scope || registration.active?.state !== 'activated') {
      return 'disabled';
    }

    const subscription = await registration.pushManager.getSubscription();
    // Permission or local settings may change while the browser checks the subscription.
    return (
      readNotificationPrerequisites() ??
      (subscription && registration.active?.state === 'activated' ? 'enabled' : 'disabled')
    );
  } catch {
    return 'error';
  }
}

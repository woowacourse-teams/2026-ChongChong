import useBooleanState from '../../../shared/hooks/useBooleanState';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { enablePush, disablePush, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/push';

const PUSH_ENABLED_KEY = 'chongchong:push-enabled';

// TODO: Storage 저장공간을 훅이 모르게 분리
export default function useNotificationEnabledState() {
  const toast = useToast();
  const [isEnabled, enabled, disabled] = useBooleanState(
    () =>
      'Notification' in window &&
      Notification.permission === 'granted' &&
      localStorage.getItem(PUSH_ENABLED_KEY) === 'true' &&
      localStorage.getItem(PUSH_SUBSCRIPTION_ID_KEY) !== null,
  );

  const [changing, startChanging, endChanging] = useBooleanState();

  async function disableNotifications() {
    await disablePush();
    localStorage.setItem(PUSH_ENABLED_KEY, 'false');
    disabled();
  }

  async function handleToggleNotificationEnabled() {
    if (changing) return;

    startChanging();

    try {
      const nextEnabledState = !isEnabled;

      if (nextEnabledState) {
        if (!(await enablePush())) return;
        localStorage.setItem(PUSH_ENABLED_KEY, 'true');
        enabled();
      } else {
        await disableNotifications();
      }

      localStorage.setItem(PUSH_ENABLED_KEY, String(nextEnabledState));
    } catch (error) {
      toast.open(
        <StatusToast
          message={
            error instanceof Error
              ? error.message
              : '알림 설정을 변경하지 못했어요. 다시 시도해 주세요.'
          }
          status="Error"
        />,
      );
    } finally {
      endChanging();
    }
  }

  return [isEnabled, changing, handleToggleNotificationEnabled, disableNotifications] as const;
}

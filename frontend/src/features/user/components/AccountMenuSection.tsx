import { type CSSProperties } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { logout } from '../../login/api';
import { clearLocalPushSubscription } from '../../notification/localPush';
import { useToast } from '../../../shared/providers/ToastProvider';
import { tokens, typography } from '../../../styles/global';
import List from '../../../shared/ui/List';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import useWithdrawAccount from '../hooks/useWithdrawAccount';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import Switch from '../../../shared/ui/Switch';
import { usePostHog } from '@posthog/react';
import useNotificationEnabledState from '../hooks/useNotificationEnabledState';

const menuItems = [
  {
    label: '개인정보처리방침',
    color: tokens.color.mainBlack,
    url: 'https://glittery-cobweb-ac6.notion.site/3b9842e631298049903cec2633a4fab7?pvs=74',
  },
  { label: '지원', color: tokens.color.mainBlack, url: 'mailto:chongwithoutc@gmail.com' },
] as const;

const sectionStyle = {
  marginTop: '60px',
} satisfies CSSProperties;

const menuButtonStyle = {
  ...typography.subtitle,
  padding: 0,
  border: 0,
  background: 'transparent',
  textAlign: 'left',
  cursor: 'pointer',
} satisfies CSSProperties;

export default function AccountMenuSection() {
  const navigate = useNavigate();
  const toast = useToast();
  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate: requestLogout, isPending: isLoggingOut } = useMutation({
    mutationFn: async () => {
      try {
        await disableNotifications();
      } catch (error) {
        console.error(error);
        await clearLocalPushSubscription().catch(console.error);
      }
      await logout();
    },
    onSuccess: () => posthog.reset(),
  });
  const { mutate: withdraw, isPending: isWithdrawing } = useWithdrawAccount();

  const {
    isEnabled: isNotificationEnabled,
    changing: notificationEnabledChanging,
    isLoading: isNotificationStatusLoading,
    isError: isNotificationStatusError,
    refetch: refetchNotificationStatus,
    handleToggleNotificationEnabled,
    disableNotifications,
  } = useNotificationEnabledState();

  const posthog = usePostHog();

  const isAccountActionPending = notificationEnabledChanging || isLoggingOut || isWithdrawing;

  function handleLogout() {
    if (isAccountActionPending) return;
    requestLogout(undefined, {
      onSuccess: () => {
        navigate('/login', { replace: true });
      },
      onError: (error) => {
        toast.open(<StatusToast message={error.message} status="error" />);
      },
    });
  }

  function handleWithdraw() {
    if (isAccountActionPending) return;
    withdraw(undefined, {
      onSuccess: () => {
        posthog.reset();
        navigate('/login', { replace: true });
      },
      onError: (error) => {
        closeDialog();
        toast.open(<StatusToast message={error.message} status="error" />);
      },
    });
  }

  return (
    <>
      <section css={sectionStyle} aria-label="계정 메뉴">
        <List>
          <List.Item>
            <div css={{ display: 'flex', justifyContent: 'space-between' }}>
              <label htmlFor="notification-enabled-state">푸시 알림</label>
              <Switch
                checked={isNotificationEnabled}
                disabled={
                  isAccountActionPending || isNotificationStatusLoading || isNotificationStatusError
                }
                onChange={handleToggleNotificationEnabled}
                id="notification-enabled-state"
              />
            </div>
            {isNotificationStatusError && (
              <div role="alert">
                알림 설정을 불러오지 못했어요.
                <button
                  type="button"
                  onClick={() => void refetchNotificationStatus()}
                  disabled={isNotificationStatusLoading}
                >
                  다시 시도
                </button>
              </div>
            )}
          </List.Item>
          <List.Item>
            <button
              type="button"
              css={{ ...menuButtonStyle, color: tokens.text.critical }}
              onClick={handleLogout}
              disabled={isAccountActionPending}
            >
              로그아웃
            </button>
          </List.Item>
          {menuItems.map(({ label, color, url }) => (
            <List.Item key={label} css={{ ...typography.subtitle, color }}>
              <a href={url}>{label}</a>
            </List.Item>
          ))}
          <List.Item>
            <button
              type="button"
              css={{ ...menuButtonStyle, color: tokens.text.muted }}
              onClick={openDialog}
            >
              회원 탈퇴
            </button>
          </List.Item>
        </List>
      </section>
      {isOpen && (
        <ConfirmDialog
          title="회원 탈퇴하시겠습니까?"
          description="탈퇴하면 계정 정보가 삭제되며 다시 복구할 수 없어요."
          onClose={closeDialog}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton onClick={handleWithdraw} disabled={isAccountActionPending}>
              {isWithdrawing ? '탈퇴 중...' : '탈퇴'}
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </>
  );
}

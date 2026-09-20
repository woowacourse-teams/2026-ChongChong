import type { CSSProperties } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { logout } from '../../login/api';
import { clearAccessToken } from '../../login/accessToken';
import { useToast } from '../../../shared/providers/ToastProvider';
import { tokens, typography } from '../../../styles/global';
import List from '../../../shared/ui/List';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import useWithdrawAccount from '../hooks/useWithdrawAccount';
import useBooleanState from '../../../shared/hooks/useBooleanState';

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

  const { mutate: requestLogout, isPending: isLoggingOut } = useMutation({ mutationFn: logout });
  const { mutate: withdraw, isPending: isWithdrawing } = useWithdrawAccount();

  function handleLogout() {
    requestLogout(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
      onError: (error) => {
        toast.open(<StatusToast message={error.message} status="Error" />);
      },
    });
  }

  function handleWithdraw() {
    withdraw(undefined, {
      onSuccess: () => {
        clearAccessToken();
        navigate('/login', { replace: true });
      },
      onError: (error) => {
        close();
        toast.open(<StatusToast message={error.message} status="Error" />);
      },
    });
  }

  return (
    <>
      <section css={sectionStyle} aria-label="계정 메뉴">
        <List>
          <List.Item>
            <button
              type="button"
              css={{ ...menuButtonStyle, color: tokens.text.critical }}
              onClick={handleLogout}
              disabled={isLoggingOut}
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
            <ConfirmDialog.ConfirmButton onClick={handleWithdraw} disabled={isWithdrawing}>
              {isWithdrawing ? '탈퇴 중...' : '탈퇴'}
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </>
  );
}

import { screen } from '@testing-library/react';
import { setup } from '../../../../test/render';
import type { GuidelinePageProps, NotificationStatus } from '../../types';
import NotificationPage from './NotificationPage';

function renderPage(status: NotificationStatus, permissionGranted = false) {
  const onNotificationSettings = jest.fn();
  const props: GuidelinePageProps = {
    pwa: {
      platform: 'desktop',
      guideOpen: false,
      isStandalone: false,
      isInstalled: false,
      isInstalling: false,
      install: async () => {},
      message: '',
    },
    notification: { status, permissionGranted, message: '' },
    onNext: () => {},
    onClose: () => {},
    onJoinStudy: () => {},
    onNotificationSettings,
  };
  return { ...setup(<NotificationPage {...props} />), props, onNotificationSettings };
}

test('구독 중이면 완료 버튼을 표시하고 설정 이동과 중복 활성화 안내를 제공하지 않는다', async () => {
  const { user, onNotificationSettings } = renderPage('enabled', true);
  const button = screen.getByRole('button', { name: '알림 구독 중' });

  expect(button).toBeDisabled();
  expect(button).toHaveTextContent('✓');
  expect(screen.queryByText('마이페이지 → 푸시 알림')).not.toBeInTheDocument();
  expect(screen.queryByText(/스위치를 켜고/)).not.toBeInTheDocument();
  expect(screen.getByText(/이미 푸시 알림을 구독하고 있어요/)).toBeInTheDocument();
  await user.click(button);
  expect(onNotificationSettings).not.toHaveBeenCalled();
});

test('최초 상태 확인 중에는 버튼을 잠시 비활성화한다', async () => {
  const { user, onNotificationSettings } = renderPage('checking');
  const button = screen.getByRole('button', { name: '알림 상태 확인 중…' });

  expect(button).toBeDisabled();
  expect(button).toHaveAttribute('aria-busy', 'true');
  await user.click(button);
  expect(onNotificationSettings).not.toHaveBeenCalled();
});

test.each(['disabled', 'denied', 'unsupported', 'error'] as const)(
  '%s 상태에서는 기존 마이페이지 이동을 유지한다',
  async (status) => {
    const { user, onNotificationSettings } = renderPage(status, status === 'disabled');
    const button = screen.getByRole('button', { name: '알림 켜기' });

    expect(button).toBeEnabled();
    expect(screen.getByText('마이페이지 → 푸시 알림')).toBeInTheDocument();
    await user.click(button);
    expect(onNotificationSettings).toHaveBeenCalledTimes(1);
  },
);

test('확인 결과와 복귀 후 구독 변경을 버튼과 본문에 반영한다', () => {
  const { props, rerender } = renderPage('checking');
  rerender(
    <NotificationPage
      {...props}
      notification={{ status: 'enabled', permissionGranted: true, message: '' }}
    />,
  );
  expect(screen.getByRole('button', { name: '알림 구독 중' })).toBeDisabled();
  expect(screen.queryByText(/스위치를 켜고/)).not.toBeInTheDocument();

  rerender(
    <NotificationPage
      {...props}
      notification={{ status: 'disabled', permissionGranted: true, message: '' }}
    />,
  );
  expect(screen.getByRole('button', { name: '알림 켜기' })).toBeEnabled();
  expect(screen.getByText(/스위치를 켜고/)).toBeInTheDocument();
});

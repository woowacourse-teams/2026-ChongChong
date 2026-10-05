import { render, screen } from '@testing-library/react';
import type { GuidelinePageProps } from '../../types';
import CompletePage from './CompletePage';

function renderPage({
  isStandalone = false,
  isInstalled = false,
  permissionGranted = false,
  notificationStatus = 'disabled',
  platform = 'desktop',
}: {
  isStandalone?: boolean;
  isInstalled?: boolean;
  permissionGranted?: boolean;
  notificationStatus?: GuidelinePageProps['notification']['status'];
  platform?: GuidelinePageProps['pwa']['platform'];
} = {}) {
  const props: GuidelinePageProps = {
    pwa: {
      platform,
      guideOpen: false,
      isStandalone,
      isInstalled,
      isInstalling: false,
      install: async () => {},
      message: '',
    },
    notification: { status: notificationStatus, permissionGranted, message: '' },
    onNext: () => {},
    onClose: () => {},
    onJoinStudy: () => {},
    onNotificationSettings: () => {},
  };
  return render(<CompletePage {...props} />);
}

test('설치했어도 브라우저에서 열었다면 앱 실행을 완료로 표시하지 않는다', () => {
  renderPage({ isInstalled: true });
  expect(screen.getByRole('img', { name: '앱으로 실행: 브라우저 이용 중' })).toBeInTheDocument();
  expect(screen.queryByRole('img', { name: '앱으로 실행: 사용 중' })).not.toBeInTheDocument();
});

test('현재 독립 실행 모드로 열려 있으면 앱 실행을 체크한다', () => {
  renderPage({ isStandalone: true });
  expect(screen.getByRole('img', { name: '앱으로 실행: 사용 중' })).toBeInTheDocument();
});

test('브라우저 권한만 허용하고 푸시 구독이 꺼져 있으면 완료로 표시하지 않는다', () => {
  renderPage({ isStandalone: true, permissionGranted: true });
  expect(screen.getByRole('img', { name: '푸시 알림: 구독 안 됨' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: '마지막으로 확인해요' })).toBeInTheDocument();
});

test('알림 권한을 허용하지 않은 상태도 접근성 이름으로 구분한다', () => {
  renderPage();
  expect(screen.getByRole('img', { name: '푸시 알림: 구독 안 됨' })).toBeInTheDocument();
});

test.each([
  { isStandalone: false, permissionGranted: false, notificationStatus: 'disabled' as const },
  { isStandalone: true, permissionGranted: false, notificationStatus: 'disabled' as const },
  { isStandalone: false, permissionGranted: true, notificationStatus: 'enabled' as const },
  { isStandalone: true, permissionGranted: true, notificationStatus: 'enabled' as const },
])('현재 실행 및 권한 상태에 맞는 설명을 보여 준다: %o', (state) => {
  renderPage(state);

  expect(
    screen.getByRole('img', {
      name: state.notificationStatus === 'enabled' ? '푸시 알림: 구독 중' : '푸시 알림: 구독 안 됨',
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByText(
      state.isStandalone
        ? '앱으로 잘 들어오셨어요!'
        : '앱으로 열면 브라우저 메뉴 없이 이용할 수 있어요.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      state.permissionGranted
        ? '푸시 알림을 받을 준비가 되었어요.'
        : '새 공지와 일정의 푸시 알림을 받을 수 없어요.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      state.isStandalone && state.permissionGranted
        ? '축하해요! 준비를 마쳤어요.'
        : '현재 설정을 확인해 보세요.',
    ),
  ).toBeInTheDocument();
});

test.each(['checking', 'error'] as const)(
  '구독 상태가 %s이면 미확인 상태로 표시한다',
  (notificationStatus) => {
    renderPage({ isStandalone: true, permissionGranted: true, notificationStatus });
    expect(
      screen.getByRole('img', {
        name: notificationStatus === 'checking' ? '푸시 알림: 확인 중' : '푸시 알림: 확인 필요',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '마지막으로 확인해요' })).toBeInTheDocument();
  },
);

test('권한이 회수되면 이전 구독 상태가 남아 있어도 완료 체크를 제거한다', () => {
  renderPage({ isStandalone: true, permissionGranted: false, notificationStatus: 'enabled' });
  expect(screen.getByRole('img', { name: '푸시 알림: 구독 안 됨' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: '마지막으로 확인해요' })).toBeInTheDocument();
});

test.each(['ios-safari', 'ios-other'] as const)(
  '아이폰 브라우저에서는 홈 화면 앱으로 열어야 알림을 받을 수 있다고 안내한다: %s',
  (platform) => {
    renderPage({ platform });

    expect(screen.getByText('홈 화면의 앱으로 열어야 알림을 받을 수 있어요.')).toBeInTheDocument();
  },
);

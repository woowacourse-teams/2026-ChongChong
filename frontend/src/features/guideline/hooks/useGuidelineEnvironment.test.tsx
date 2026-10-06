import { act, renderHook, waitFor } from '@testing-library/react';
import { PUSH_ENABLED_KEY, PUSH_SUBSCRIPTION_ID_KEY } from '../../notification/localPush';
import { useGuidelineEnvironment } from './useGuidelineEnvironment';

let permission: NotificationPermission;
let subscription: object | null;
let getSubscription: jest.Mock;
let getRegistration: jest.Mock;
let media: EventTarget & { matches: boolean };

const originalDescriptors = [
  [window, 'Notification'],
  [window, 'PushManager'],
  [window, 'matchMedia'],
  [navigator, 'serviceWorker'],
  [navigator, 'standalone'],
  [document, 'visibilityState'],
].map(([target, key]) => ({
  target: target as object,
  key: key as string,
  descriptor: Object.getOwnPropertyDescriptor(target, key as string),
}));

function markEnabled() {
  localStorage.setItem(PUSH_ENABLED_KEY, 'true');
  localStorage.setItem(PUSH_SUBSCRIPTION_ID_KEY, '123');
}

beforeEach(() => {
  localStorage.clear();
  permission = 'default';
  subscription = null;
  getSubscription = jest.fn(async () => subscription);
  getRegistration = jest.fn(async () => ({
    scope: new URL('/push/', window.location.origin).href,
    active: { state: 'activated' },
    pushManager: {
      getSubscription,
    },
  }));
  Object.defineProperty(window, 'Notification', {
    configurable: true,
    value: {
      get permission() {
        return permission;
      },
    },
  });
  Object.defineProperty(window, 'PushManager', { configurable: true, value: class {} });
  Object.defineProperty(navigator, 'serviceWorker', {
    configurable: true,
    value: { getRegistration },
  });
  media = Object.assign(new EventTarget(), { matches: false });
  Object.defineProperty(window, 'matchMedia', { configurable: true, value: () => media });
  Reflect.deleteProperty(navigator, 'standalone');
  Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'visible' });
});

afterEach(() => {
  for (const { target, key, descriptor } of originalDescriptors) {
    if (descriptor) Object.defineProperty(target, key, descriptor);
    else Reflect.deleteProperty(target, key);
  }
  localStorage.clear();
});

test.each(['Notification', 'PushManager', 'serviceWorker'])(
  '%s 없는 환경을 지원하지 않는 상태로 표시한다',
  async (feature) => {
    Reflect.deleteProperty(feature === 'serviceWorker' ? navigator : window, feature);
    const { result } = renderHook(useGuidelineEnvironment);
    await waitFor(() => expect(result.current.notification.status).toBe('unsupported'));
  },
);

test('권한 차단은 저장된 활성화 표시보다 우선한다', async () => {
  permission = 'denied';
  markEnabled();
  const { result } = renderHook(useGuidelineEnvironment);
  await waitFor(() => expect(result.current.notification.status).toBe('denied'));
});

test('알림 권한 허용 여부는 푸시 구독 없이도 확인한다', async () => {
  permission = 'granted';
  const { result } = renderHook(useGuidelineEnvironment);
  expect(result.current.notification.permissionGranted).toBe(true);
  await waitFor(() => expect(result.current.notification.status).toBe('disabled'));
  expect(result.current.notification.permissionGranted).toBe(true);
});

test('알림 API가 없으면 알림 권한을 허용한 것으로 표시하지 않는다', async () => {
  Reflect.deleteProperty(window, 'Notification');
  const { result } = renderHook(useGuidelineEnvironment);
  expect(result.current.notification.permissionGranted).toBe(false);
  await waitFor(() => expect(result.current.notification.status).toBe('unsupported'));
});

test.each(['focus', 'visibilitychange'])(
  '%s 후 구독 확인을 기다리는 동안에도 변경된 알림 권한을 반영한다',
  async (event) => {
    const { result } = renderHook(useGuidelineEnvironment);
    await waitFor(() => expect(result.current.notification.status).toBe('disabled'));
    permission = 'granted';
    markEnabled();
    getRegistration.mockReturnValue(new Promise(() => {}));
    act(() => (event === 'focus' ? window : document).dispatchEvent(new Event(event)));
    expect(result.current.notification.permissionGranted).toBe(true);

    permission = 'denied';
    act(() => (event === 'focus' ? window : document).dispatchEvent(new Event(event)));
    expect(result.current.notification.permissionGranted).toBe(false);
    await waitFor(() => expect(result.current.notification.status).toBe('denied'));
  },
);

test.each(['permission', 'marker', 'id', 'subscription', 'scope', 'activation'])(
  '실제 활성 구독의 %s 조건이 빠지면 완료로 표시하지 않는다',
  async (missing) => {
    permission = 'granted';
    markEnabled();
    subscription = {};
    if (missing === 'permission') permission = 'default';
    if (missing === 'marker') localStorage.removeItem(PUSH_ENABLED_KEY);
    if (missing === 'id') localStorage.removeItem(PUSH_SUBSCRIPTION_ID_KEY);
    if (missing === 'subscription') subscription = null;
    if (missing === 'scope' || missing === 'activation') {
      getRegistration.mockResolvedValue({
        scope: new URL(missing === 'scope' ? '/' : '/push/', window.location.origin).href,
        active: { state: missing === 'activation' ? 'installing' : 'activated' },
        pushManager: { getSubscription },
      });
    }
    const { result } = renderHook(useGuidelineEnvironment);
    await waitFor(() => expect(result.current.notification.status).toBe('disabled'));
  },
);

test.each(['storage', 'registration', 'subscription'])(
  '%s 확인 실패는 완료 대신 오류로 표시한다',
  async (failure) => {
    permission = 'granted';
    markEnabled();
    subscription = {};
    if (failure === 'storage')
      jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('storage blocked');
      });
    if (failure === 'registration')
      getRegistration.mockRejectedValue(new Error('worker unavailable'));
    if (failure === 'subscription')
      getSubscription.mockRejectedValue(new Error('subscription unavailable'));
    const { result } = renderHook(useGuidelineEnvironment);
    await waitFor(() => expect(result.current.notification.status).toBe('error'));
    expect(result.current.notification.message).not.toBe('');
  },
);

test.each(['focus', 'visibilitychange', 'storage'])(
  '%s 후 실제 기기 설정을 다시 확인한다',
  async (event) => {
    const { result } = renderHook(useGuidelineEnvironment);
    await waitFor(() => expect(result.current.notification.status).toBe('disabled'));
    permission = 'granted';
    markEnabled();
    subscription = {};
    act(() => {
      if (event === 'storage')
        window.dispatchEvent(new StorageEvent('storage', { key: PUSH_ENABLED_KEY }));
      else (event === 'focus' ? window : document).dispatchEvent(new Event(event));
    });
    await waitFor(() => expect(result.current.notification.status).toBe('enabled'));
  },
);

test('이전 구독 확인 응답이 최신 권한 차단을 덮어쓰지 않는다', async () => {
  permission = 'granted';
  markEnabled();
  let resolveSubscription!: (value: object) => void;
  getSubscription.mockImplementationOnce(
    () =>
      new Promise((resolve) => {
        resolveSubscription = resolve;
      }),
  );
  const { result } = renderHook(useGuidelineEnvironment);
  await waitFor(() => expect(resolveSubscription).toBeDefined());
  permission = 'denied';
  act(() => window.dispatchEvent(new Event('focus')));
  await waitFor(() => expect(result.current.notification.status).toBe('denied'));
  await act(async () => resolveSubscription({}));
  expect(result.current.notification.status).toBe('denied');
});

test('matchMedia가 없는 환경에서도 설치 안내를 사용할 수 있다', async () => {
  Reflect.deleteProperty(window, 'matchMedia');
  const { result } = renderHook(useGuidelineEnvironment);
  await act(async () => result.current.pwa.install());
  expect(result.current.pwa.isInstalled).toBe(false);
  expect(result.current.pwa.guideOpen).toBe(true);
  expect(result.current.pwa.message).toBe('');
});

test('독립 실행 모드 변경으로 앱 실행 상태를 갱신한다', async () => {
  const { result } = renderHook(useGuidelineEnvironment);
  await waitFor(() => expect(result.current.notification.status).toBe('disabled'));
  await act(async () => {
    media.matches = true;
    media.dispatchEvent(new Event('change'));
  });
  expect(result.current.pwa.isStandalone).toBe(true);
  expect(result.current.pwa.isInstalled).toBe(true);
});

test('설치 수락만으로 완료로 표시하지 않고 appinstalled 이후 재방문에도 완료를 유지한다', async () => {
  const event = new Event('beforeinstallprompt', { cancelable: true });
  Object.assign(event, {
    prompt: async () => {},
    userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
  });
  window.dispatchEvent(event);
  const { result, unmount } = renderHook(useGuidelineEnvironment);
  await act(async () => result.current.pwa.install());
  expect(result.current.pwa.isInstalled).toBe(false);
  await act(async () => {
    window.dispatchEvent(new Event('appinstalled'));
  });
  expect(result.current.pwa.isInstalled).toBe(true);
  unmount();
  const reopened = renderHook(useGuidelineEnvironment);
  await waitFor(() => expect(reopened.result.current.notification.status).toBe('disabled'));
  expect(reopened.result.current.pwa.isInstalled).toBe(true);
});

test('설치 완료 후 새 설치 제안이 오면 이전 확인을 지우고 다시 설치할 수 있다', async () => {
  const { result, unmount } = renderHook(useGuidelineEnvironment);
  await act(async () => {
    window.dispatchEvent(new Event('appinstalled'));
  });
  expect(result.current.pwa.isInstalled).toBe(true);

  const prompt = jest.fn(async () => {});
  const event = new Event('beforeinstallprompt', { cancelable: true });
  Object.assign(event, {
    prompt,
    userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
  });
  act(() => {
    window.dispatchEvent(event);
  });
  expect(result.current.pwa.isInstalled).toBe(false);
  expect(result.current.pwa.message).toBe('');
  await act(async () => result.current.pwa.install());
  expect(prompt).toHaveBeenCalledTimes(1);
  expect(result.current.pwa.isInstalled).toBe(false);

  unmount();
  const reopened = renderHook(useGuidelineEnvironment);
  await waitFor(() => expect(reopened.result.current.notification.status).toBe('disabled'));
  expect(reopened.result.current.pwa.isInstalled).toBe(false);
});

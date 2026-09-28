import { act, renderHook } from '@testing-library/react';
import { usePwaInstall } from './usePwaInstall';

function offerInstall(outcome: 'accepted' | 'dismissed' = 'accepted') {
  const event = new Event('beforeinstallprompt', { cancelable: true });
  const prompt = jest.fn().mockResolvedValue(undefined);
  Object.assign(event, { prompt, userChoice: Promise.resolve({ outcome, platform: 'web' }) });
  act(() => window.dispatchEvent(event));
  return { event, prompt };
}

beforeEach(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: jest.fn(() => ({ matches: false })),
  });
  Reflect.deleteProperty(navigator, 'standalone');
});

test('랜딩이 마운트되기 전에 받은 설치 이벤트도 버튼 동작에서 사용한다', async () => {
  const { event, prompt } = offerInstall();
  const { result } = renderHook(usePwaInstall);

  expect(event.defaultPrevented).toBe(true);
  await act(async () => {
    expect(await result.current.install()).toBe('accepted');
  });
  expect(prompt).toHaveBeenCalledTimes(1);
});

test('취소한 설치 이벤트는 재사용하지 않고 새 이벤트가 오면 다시 설치한다', async () => {
  const { result } = renderHook(usePwaInstall);
  const first = offerInstall('dismissed');
  await act(async () => {
    expect(await result.current.install()).toBe('dismissed');
    expect(await result.current.install()).toBe('unavailable');
  });
  expect(first.prompt).toHaveBeenCalledTimes(1);

  const next = offerInstall();
  await act(async () => {
    expect(await result.current.install()).toBe('accepted');
  });
  expect(next.prompt).toHaveBeenCalledTimes(1);
});

test('설치 창 응답을 기다리는 동안 연속 클릭으로 중복 요청하지 않는다', async () => {
  let choose!: (choice: { outcome: string }) => void;
  const event = new Event('beforeinstallprompt', { cancelable: true });
  const prompt = jest.fn().mockResolvedValue(undefined);
  Object.assign(event, {
    prompt,
    userChoice: new Promise((resolve) => {
      choose = resolve;
    }),
  });
  const { result } = renderHook(usePwaInstall);
  act(() => window.dispatchEvent(event));
  let installation!: ReturnType<typeof result.current.install>;
  act(() => {
    installation = result.current.install();
  });
  expect(result.current.isPrompting).toBe(true);
  await act(async () => {
    expect(await result.current.install()).toBe('busy');
    choose({ outcome: 'dismissed' });
    expect(await installation).toBe('dismissed');
  });
  expect(result.current.isPrompting).toBe(false);
  expect(prompt).toHaveBeenCalledTimes(1);
});

test('브라우저가 설치 요청을 거절하면 안내로 전환할 수 있고 다시 시도할 수 있다', async () => {
  const { result } = renderHook(usePwaInstall);
  const { prompt } = offerInstall();
  prompt.mockRejectedValueOnce(new Error('NotAllowedError'));
  await act(async () => {
    expect(await result.current.install()).toBe('unavailable');
  });
  expect(result.current.isPrompting).toBe(false);
  offerInstall();
  await act(async () => {
    expect(await result.current.install()).toBe('accepted');
  });
});

test.each(['display-mode', 'ios-standalone'])(
  '%s 앱 실행 중에는 설치를 다시 요청하지 않는다',
  async (mode) => {
    const { prompt } = offerInstall();
    if (mode === 'display-mode') {
      Object.defineProperty(window, 'matchMedia', { value: () => ({ matches: true }) });
    } else {
      Object.defineProperty(navigator, 'standalone', { configurable: true, value: true });
    }
    const { result } = renderHook(usePwaInstall);
    await act(async () => {
      expect(await result.current.install()).toBe('installed');
    });
    expect(prompt).not.toHaveBeenCalled();
  },
);

test('브라우저 메뉴에서 설치가 완료되어도 이후 클릭은 앱 시작으로 처리한다', async () => {
  const { result } = renderHook(usePwaInstall);
  const { prompt } = offerInstall();
  act(() => window.dispatchEvent(new Event('appinstalled')));
  await act(async () => {
    expect(await result.current.install()).toBe('installed');
  });
  expect(prompt).not.toHaveBeenCalled();
});

test('설치 완료 후 랜딩을 다시 방문해도 설치 안내를 반복하지 않는다', async () => {
  const firstVisit = renderHook(usePwaInstall);
  offerInstall();
  act(() => window.dispatchEvent(new Event('appinstalled')));
  firstVisit.unmount();

  const { result } = renderHook(usePwaInstall);
  await act(async () => {
    expect(await result.current.install()).toBe('installed');
  });
});

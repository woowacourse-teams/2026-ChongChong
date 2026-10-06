import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { useGuidelineEnvironment } from '../../hooks/useGuidelineEnvironment';
import PwaPage from './PwaPage';

const safari =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) Version/26.0 Mobile/15E148 Safari/604.1';
const originalDescriptors = ['userAgent', 'clipboard', 'standalone'].map((key) => ({
  key,
  descriptor: Object.getOwnPropertyDescriptor(navigator, key),
}));
const originalMatchMedia = Object.getOwnPropertyDescriptor(window, 'matchMedia');

function Page({ onNext = () => {} }: { onNext?: () => void } = {}) {
  const environment = useGuidelineEnvironment();
  return (
    <PwaPage
      {...environment}
      onNext={onNext}
      onClose={() => {}}
      onJoinStudy={() => {}}
      onNotificationSettings={() => {}}
    />
  );
}

function setUserAgent(value: string) {
  Object.defineProperty(navigator, 'userAgent', { configurable: true, value });
}

function offerInstall() {
  const prompt = jest.fn().mockResolvedValue(undefined);
  const event = new Event('beforeinstallprompt', { cancelable: true });
  Object.assign(event, {
    prompt,
    userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
  });
  window.dispatchEvent(event);
  return prompt;
}

beforeEach(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false }),
  });
  Reflect.deleteProperty(navigator, 'standalone');
  setUserAgent(safari);
  offerInstall();
});

afterEach(() => {
  for (const { key, descriptor } of originalDescriptors) {
    if (descriptor) Object.defineProperty(navigator, key, descriptor);
    else Reflect.deleteProperty(navigator, key);
  }
  if (originalMatchMedia) Object.defineProperty(window, 'matchMedia', originalMatchMedia);
  else Reflect.deleteProperty(window, 'matchMedia');
  window.history.replaceState(null, '', '/');
});

test('iPhone Safari에서는 설치 안내를 바로 보여 주고 다음 단계로 이동할 수 있다', async () => {
  const prompt = offerInstall();
  const onNext = jest.fn();
  render(<Page onNext={onNext} />);
  expect(screen.getByRole('region', { name: 'Safari 설치 안내' })).toBeInTheDocument();
  const guide = await screen.findByRole('region', { name: 'Safari 설치 안내' });
  expect(within(guide).getByText('공유 메뉴 열기')).toBeInTheDocument();
  expect(within(guide).getByText('홈 화면에 추가')).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(prompt).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: '다음으로' }));
  expect(onNext).toHaveBeenCalledTimes(1);
});

test('Android에서는 클릭 중 즉시 설치창을 요청하고 수락만으로 완료나 수동 안내를 표시하지 않는다', async () => {
  setUserAgent('Mozilla/5.0 (Linux; Android 16) Chrome/140.0.0.0 Mobile Safari/537.36');
  const prompt = offerInstall();
  render(<Page />);
  expect(screen.getByRole('region', { name: 'Android 설치 안내' })).toBeInTheDocument();
  expect(prompt).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' }));
  expect(prompt).toHaveBeenCalledTimes(1);
  await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('설치를 진행 중'));
  expect(screen.getByRole('region', { name: 'Android 설치 안내' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: '홈 화면 앱 준비 완료' })).not.toBeInTheDocument();
  act(() => window.dispatchEvent(new Event('appinstalled')));
  expect(screen.getByRole('button', { name: '홈 화면 앱 준비 완료' })).toBeDisabled();
});

test('설치창을 사용할 수 없는 브라우저는 버튼 클릭 후 수동 안내로 이동한다', async () => {
  setUserAgent('Mozilla/5.0 Desktop');
  Reflect.deleteProperty(window, 'matchMedia');
  render(<Page />);
  expect(screen.getByRole('region', { name: '브라우저 설치 안내' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' }));
  const guide = await screen.findByRole('region', { name: '브라우저 설치 안내' });
  expect(guide).toHaveFocus();
});

test.each(['focus', 'resize'])(
  '모바일 미리보기에서 PC 환경으로 바뀌면 %s 시 설치 안내도 갱신한다',
  async (eventName) => {
    setUserAgent('Mozilla/5.0 (Linux; Android 16) Chrome/140.0 Mobile Safari/537.36');
    Reflect.deleteProperty(window, 'matchMedia');
    render(<Page />);
    fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' }));
    expect(await screen.findByRole('region', { name: 'Android 설치 안내' })).toBeInTheDocument();

    setUserAgent(
      'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36',
    );
    await act(async () => {
      fireEvent(window, new Event(eventName));
    });

    expect(screen.getByRole('region', { name: '브라우저 설치 안내' })).toBeInTheDocument();
    expect(screen.queryByText(/Android에서는/)).not.toBeInTheDocument();
  },
);

test.each(['success', 'denied', 'unsupported'])(
  'iPhone 다른 브라우저에서는 비밀 쿼리가 없는 주소를 복사하거나 직접 선택할 수 있다 (%s)',
  async (clipboard) => {
    setUserAgent(safari.replace('Version/26.0', 'CriOS/140.0'));
    window.history.replaceState(null, '', '/studies/join?token=private#private');
    const writeText = jest.fn().mockImplementation(async () => {
      if (clipboard === 'denied') throw new Error('NotAllowedError');
    });
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: clipboard === 'unsupported' ? undefined : { writeText },
    });
    render(<Page />);
    expect(screen.getByRole('textbox', { name: 'Safari에서 열 주소' })).toBeInTheDocument();
    const url = await screen.findByRole('textbox', { name: 'Safari에서 열 주소' });
    expect(url).toHaveValue('http://localhost/studies');
    expect(url).toHaveAttribute('readonly');
    fireEvent.click(screen.getByRole('button', { name: '주소 복사' }));
    if (clipboard === 'success') {
      await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('복사했어요'));
      expect(writeText).toHaveBeenCalledWith('http://localhost/studies');
    } else {
      await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('직접 복사'));
      fireEvent.focus(url);
      expect((url as HTMLInputElement).selectionStart).toBe(0);
      expect((url as HTMLInputElement).selectionEnd).toBe('http://localhost/studies'.length);
    }
  },
);

import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import InstallAppButton from './InstallAppButton';

beforeEach(() => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false }),
  });
  jest.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (iPhone)');
});

function renderButton() {
  return render(
    <MemoryRouter>
      <Routes>
        <Route path="/" element={<InstallAppButton onActiveChange={() => {}} />} />
        <Route path="/studies" element={<h1>스터디 목록</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

test.each([
  ['Mozilla/5.0 (iPhone)', /Safari/, /공유/],
  ['Mozilla/5.0 (Linux; Android 15)', /Chrome/, /메뉴/],
  ['Mozilla/5.0 (Windows NT 10.0)', /Chrome|Edge/, /주소창/],
])(
  '설치 창을 사용할 수 없는 %s 환경에서는 맞는 설치 방법을 안내한다',
  async (agent, browser, action) => {
    jest.spyOn(navigator, 'userAgent', 'get').mockReturnValue(agent);
    renderButton();
    const button = screen.getByRole('button', { name: '앱으로 시작하기' });
    fireEvent.click(button);
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent(browser);
    expect(dialog).toHaveTextContent(action);
    fireEvent.click(within(dialog).getByRole('button', { name: '알겠어요' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(button).toHaveFocus();
  },
);

test('설치 창을 열 수 있으면 수동 안내 대신 브라우저 설치를 요청하고 취소 후 다시 사용할 수 있다', async () => {
  renderButton();
  const event = new Event('beforeinstallprompt', { cancelable: true });
  const prompt = jest.fn().mockResolvedValue(undefined);
  Object.assign(event, {
    prompt,
    userChoice: Promise.resolve({ outcome: 'dismissed', platform: 'web' }),
  });
  act(() => window.dispatchEvent(event));
  await act(async () => fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' })));
  expect(prompt).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: '앱으로 시작하기' })).toBeEnabled();

  fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' }));
  expect(await screen.findByRole('dialog')).toBeInTheDocument();
});

test('이미 앱으로 실행 중이면 설치 안내 없이 스터디로 이동한다', async () => {
  Object.defineProperty(window, 'matchMedia', { value: () => ({ matches: true }) });
  renderButton();
  fireEvent.click(screen.getByRole('button', { name: '앱으로 시작하기' }));
  expect(await screen.findByRole('heading', { name: '스터디 목록' })).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

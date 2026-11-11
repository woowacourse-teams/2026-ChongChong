import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router';
import App from './App';

const mockCapture = jest.fn();
jest.mock('@posthog/react', () => ({ usePostHog: () => ({ capture: mockCapture }) }));

beforeEach(() => {
  mockCapture.mockClear();
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: false, addEventListener: () => {}, removeEventListener: () => {} }),
  });
});

function renderLanding() {
  return render(
    <MemoryRouter>
      <Routes>
        <Route path="/" element={<App />} />
        <Route path="/studies" element={<h1>스터디 목록</h1>} />
      </Routes>
    </MemoryRouter>,
  );
}

test('첫 화면 제목과 웹 서비스로 들어가는 링크를 보여준다', () => {
  renderLanding();

  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('공지 확인부터');
  const webLinks = screen.getAllByRole('link', { name: '웹으로 시작하기' });
  expect(webLinks.length).toBeGreaterThanOrEqual(3);
  webLinks.forEach((link) => expect(link).toHaveAttribute('href', '/studies'));
});

test('웹으로 시작하기를 누르면 위치와 함께 PostHog에 기록하고 스터디 목록으로 이동한다', () => {
  renderLanding();

  const heroActions = screen.getByRole('heading', { level: 1 }).parentElement as HTMLElement;
  fireEvent.click(within(heroActions).getByRole('link', { name: '웹으로 시작하기' }));

  expect(mockCapture).toHaveBeenCalledWith('web_button_clicked', {
    location: 'landing_page',
    position: 'hero',
  });
  expect(screen.getByRole('heading', { name: '스터디 목록' })).toBeInTheDocument();
});

test('앱으로 시작하기 버튼은 첫 화면과 마지막 안내에 있고 안내 창 id가 겹치지 않는다', () => {
  const { container } = renderLanding();

  expect(screen.getAllByRole('button', { name: '앱으로 시작하기' })).toHaveLength(2);
  const ids = Array.from(container.querySelectorAll('[id]')).map((element) => element.id);
  expect(new Set(ids).size).toBe(ids.length);
});

test('실제 서비스에 없는 기능은 소개하지 않는다', () => {
  renderLanding();

  expect(screen.queryByText(/모두에게 알리기/)).not.toBeInTheDocument();
  expect(screen.queryByText(/공지 리마인드/)).not.toBeInTheDocument();
});

test('리마인드 단계를 누르면 그 단계 설명으로 바뀐다', () => {
  renderLanding();

  const flow = screen.getByRole('list', { name: '리마인드 3단계 흐름' });
  const deadline = within(flow).getByRole('button', { name: /마감/ });
  act(() => fireEvent.click(deadline));

  expect(deadline).toHaveAttribute('aria-current', 'step');
  expect(screen.getByText('마감 후에는 누가 냈는지 바로 확인해요').parentElement).toHaveClass(
    'show',
  );
});

test('움직임 줄이기 설정에서도 리마인드 단계를 누르면 그 단계로 바뀐다', () => {
  Object.defineProperty(window, 'matchMedia', {
    configurable: true,
    value: () => ({ matches: true, addEventListener: () => {}, removeEventListener: () => {} }),
  });
  renderLanding();

  const flow = screen.getByRole('list', { name: '리마인드 3단계 흐름' });
  ['과제 등록', '마감'].forEach((title) => {
    const button = within(flow).getByRole('button', { name: new RegExp(title) });
    act(() => fireEvent.click(button));
    expect(button).toHaveAttribute('aria-current', 'step');
  });
  expect(screen.getByText('마감 후에는 누가 냈는지 바로 확인해요').parentElement).toHaveClass(
    'show',
  );
});

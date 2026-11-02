import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, BrowserRouter, Route, Routes } from 'react-router';
import usePageNavigation from '../hooks/usePageNavigation';
import { URL_POLICY } from '../constants/urlPolicy';
import { PrevButton } from './PrevButton';

function DetailPage() {
  const { move } = usePageNavigation();
  return (
    <>
      <h1>공지 상세</h1>
      <PrevButton />
      <button onClick={() => move(URL_POLICY.afterNoticeDelete(1))}>삭제 완료</button>
    </>
  );
}

function CreatePage() {
  const { move } = usePageNavigation();
  return (
    <>
      <h1>공지 생성</h1>
      <button onClick={() => move(URL_POLICY.afterNoticeCreate(1, 10))}>생성 완료</button>
    </>
  );
}

function renderPage(path: string) {
  window.history.replaceState({ idx: 0, key: 'default', usr: null }, '', path);

  render(
    <BrowserRouter>
      <Routes>
        <Route
          path="/notifications"
          element={
            <>
              <h1>알림 목록</h1>
              <Link {...URL_POLICY.toNotice(1, 10)}>공지 알림</Link>
            </>
          }
        />
        <Route
          path="/studies/:studyId/notices"
          element={
            <>
              <h1>공지 목록</h1>
              <Link {...URL_POLICY.toCreateNotice(1)}>공지 만들기</Link>
              <PrevButton />
            </>
          }
        />
        <Route path="/studies/:studyId/notices/create" element={<CreatePage />} />
        <Route path="/studies/:studyId/notices/:noticeId" element={<DetailPage />} />
      </Routes>
    </BrowserRouter>,
  );
  return userEvent.setup();
}

function clickBack(user: ReturnType<typeof userEvent.setup>) {
  return user.click(screen.getByRole('button', { name: '뒤로 가기' }));
}

describe('브라우저 이력을 따르는 이전 버튼', () => {
  test('알림에서 진입하면 실제 이전 화면인 알림으로 돌아간다', async () => {
    const user = renderPage('/notifications');
    await user.click(screen.getByRole('link', { name: '공지 알림' }));
    await clickBack(user);
    expect(await screen.findByRole('heading', { name: '알림 목록' })).toBeVisible();
  });

  test('직접 진입하면 기본 목적지로 현재 항목을 교체한다', async () => {
    const user = renderPage('/studies/1/notices/10');
    await clickBack(user);
    expect(await screen.findByRole('heading', { name: '공지 목록' })).toBeVisible();
    expect(window.history.state.idx).toBe(0);
  });

  test('삭제 후 목록에서 뒤로가면 알림으로 돌아간다', async () => {
    const user = renderPage('/notifications');
    await user.click(screen.getByRole('link', { name: '공지 알림' }));
    await user.click(screen.getByRole('button', { name: '삭제 완료' }));
    await clickBack(user);
    expect(await screen.findByRole('heading', { name: '알림 목록' })).toBeVisible();
  });

  test('생성 완료 후 앱 뒤로가기는 생성 화면을 건너뛴다', async () => {
    const user = renderPage('/studies/1/notices');
    await user.click(screen.getByRole('link', { name: '공지 만들기' }));
    await user.click(screen.getByRole('button', { name: '생성 완료' }));
    await clickBack(user);
    expect(await screen.findByRole('heading', { name: '공지 목록' })).toBeVisible();
  });

  test('브라우저 뒤로가기도 생성 화면을 건너뛴다', async () => {
    const user = renderPage('/studies/1/notices');
    await user.click(screen.getByRole('link', { name: '공지 만들기' }));
    await user.click(screen.getByRole('button', { name: '생성 완료' }));
    await act(async () => window.history.back());
    expect(await screen.findByRole('heading', { name: '공지 목록' })).toBeVisible();
  });
});

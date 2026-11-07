import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, Link, Route, Routes } from 'react-router';
import { PrevButton } from './PrevButton';
import Tabs from './Tabs';

async function renderTabs(detailUrl = '/detail?tab=detail') {
  window.history.replaceState({ idx: 0, key: 'default', usr: null }, '', '/list');
  const user = userEvent.setup();

  render(
    <BrowserRouter>
      <Routes>
        <Route
          path="/list"
          element={
            <>
              <h1>목록</h1>
              <Link to={detailUrl}>상세 보기</Link>
            </>
          }
        />
        <Route
          path="/detail"
          element={
            <>
              <PrevButton />
              <Tabs<'detail' | 'status'>
                tabs={[
                  { key: 'detail', label: '내용', panel: <p>상세 내용</p> },
                  { key: 'status', label: '현황', panel: <p>제출 현황</p> },
                ]}
              />
            </>
          }
        />
      </Routes>
    </BrowserRouter>,
  );

  await user.click(screen.getByRole('link', { name: '상세 보기' }));
  return user;
}

describe('탭 전환 테스트', () => {
  test('탭 전환은 뒤로가기 기록을 추가하지 않는다', async () => {
    const user = await renderTabs();

    for (const name of ['현황', '내용', '현황']) {
      await user.click(screen.getByRole('tab', { name }));
      expect(screen.getByRole('tab', { name })).toHaveAttribute('aria-selected', 'true');
      expect(screen.getByRole('tabpanel', { name })).toHaveTextContent(
        name === '현황' ? '제출 현황' : '상세 내용',
      );
    }

    await user.click(screen.getByRole('button', { name: '뒤로 가기' }));

    expect(await screen.findByRole('heading', { name: '목록' })).toBeVisible();
    expect(window.location.pathname).toBe('/list');
  });
});

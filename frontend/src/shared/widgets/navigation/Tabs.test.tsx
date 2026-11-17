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

describe('Tabs 테스트', () => {
  test('탭 쿼리가 없으면 첫 번째 탭의 콘텐츠를 보여준다', async () => {
    await renderTabs('/detail');

    expect(screen.getByRole('tab', { name: '내용' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: '내용' })).toHaveTextContent('상세 내용');
  });
  test('탭 전환시 전환한 컨텐츠가 보인다', async () => {
    const user = await renderTabs();

    expect(screen.getByRole('tabpanel', { name: '내용' })).toHaveTextContent('상세 내용');

    await user.click(screen.getByRole('tab', { name: '현황' }));

    expect(screen.getByRole('tabpanel', { name: '현황' })).toHaveTextContent('제출 현황');
    expect(screen.queryByText('상세 내용')).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '현황' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '내용' })).toHaveAttribute('aria-selected', 'false');
  });

  test('탭 전환시 URL에 쿼리 파라미터가 변경된다', async () => {
    const user = await renderTabs('/detail?tab=detail&filter=all#submission');

    await user.click(screen.getByRole('tab', { name: '현황' }));

    const params = new URLSearchParams(window.location.search);
    expect(params.get('tab')).toBe('status');
    expect(params.get('filter')).toBe('all');
  });

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

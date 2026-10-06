import { fireEvent, screen } from '@testing-library/react';
import { Link, Route } from 'react-router';
import { StrictMode } from 'react';
import { createWrapper, setup, login } from '../../../test/render';
import MyStudiesPage from '../../study/pages/MyStudiesPage';
import { userTable } from '../../user/mocks/db';
import { clearAccessToken as logout } from '../../login/accessToken';

describe('스터디 목록 페이지의 가이드라인 연동', () => {
  beforeEach(async () => {
    localStorage.clear();
    await userTable.create({
      id: 1,
      name: '벤지',
      profileImage: 'http://localhost:8000',
    });
    login('벤지');
  });

  afterEach(() => {
    localStorage.clear();
    logout();
  });

  test('숨긴 가이드를 도움말로 열고 알림 설정에서 돌아와도 숨김과 읽던 페이지를 유지한다', async () => {
    localStorage.setItem('chongchong:guideline', JSON.stringify({ page: 4, dismissed: false }));
    const { user } = setup(<MyStudiesPage />, {
      wrapper: createWrapper({
        initialEntries: ['/studies'],
        routes: (element) => (
          <>
            <Route path="/studies" element={element} />
            <Route path="/studies/mypage" element={<Link to="/studies">목록으로</Link>} />
          </>
        ),
      }),
    });

    await user.click(screen.getByRole('button', { name: '다시 보지 않기' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(await screen.findByRole('button', { name: '총총 어떻게 사용해요?' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('chongchong:guideline')!)).toEqual({
      page: 1,
      dismissed: true,
    });

    await user.click(screen.getByRole('button', { name: '다음 가이드' }));
    await user.click(screen.getByRole('button', { name: '다음 가이드' }));
    await user.click(screen.getByRole('button', { name: '알림 켜기' }));
    await user.click(screen.getByRole('link', { name: '목록으로' }));

    const helpButton = await screen.findByRole('button', { name: '총총 어떻게 사용해요?' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(helpButton);
    expect(screen.getByRole('button', { name: '알림 켜기' })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem('chongchong:guideline')!)).toEqual({
      page: 3,
      dismissed: true,
    });
  });

  test.each(['닫기', 'Escape'])(
    '도움말을 키보드로 열고 %s로 닫으면 초점을 돌려준다',
    async (method) => {
      localStorage.setItem('chongchong:guideline', JSON.stringify({ page: 1, dismissed: true }));
      const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });
      const helpButton = await screen.findByRole('button', { name: '총총 어떻게 사용해요?' });
      helpButton.focus();
      await user.keyboard('{Enter}');
      expect(screen.getByRole('dialog')).toBeInTheDocument();

      if (method === '닫기') {
        await user.click(screen.getByRole('button', { name: '가이드 닫기' }));
      } else {
        // jsdom에는 Escape의 native dialog 취소 동작이 없어 cancel 이벤트를 전달한다.
        fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }));
      }

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(helpButton).toHaveFocus();
      expect(JSON.parse(localStorage.getItem('chongchong:guideline')!).dismissed).toBe(true);
    },
  );

  test('클릭이 버튼에 초점을 주지 않는 브라우저에서도 닫으면 도움말로 초점을 돌려준다', async () => {
    localStorage.setItem('chongchong:guideline', JSON.stringify({ page: 1, dismissed: true }));
    const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });
    const helpButton = await screen.findByRole('button', { name: '총총 어떻게 사용해요?' });

    // Safari의 포인터 활성화처럼 버튼으로 초점이 이동하지 않는 클릭이다.
    fireEvent.click(helpButton);
    await user.click(screen.getByRole('button', { name: '가이드 닫기' }));

    expect(helpButton).toHaveFocus();
  });

  test('StrictMode에서도 모달 밖의 초점 이동이 차단된 동안 원래 도움말 버튼을 기억한다', async () => {
    localStorage.setItem('chongchong:guideline', JSON.stringify({ page: 1, dismissed: true }));
    const { user } = setup(
      <StrictMode>
        <MyStudiesPage />
      </StrictMode>,
      { wrapper: createWrapper() },
    );
    const helpButton = await screen.findByRole('button', { name: '총총 어떻게 사용해요?' });
    const focusHelp = helpButton.focus.bind(helpButton);
    jest.spyOn(helpButton, 'focus').mockImplementation(() => {
      // jsdom의 dialog에는 실제 브라우저의 배경 inert 동작이 없다.
      if (!document.querySelector('dialog[open]')) focusHelp();
    });

    fireEvent.click(helpButton);
    await user.click(screen.getByRole('button', { name: '가이드 닫기' }));

    expect(helpButton).toHaveFocus();
  });
});

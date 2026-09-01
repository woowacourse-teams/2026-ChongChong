import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Routes, Route, useLocation } from 'react-router';
import { createWrapper } from '../../test/render';
import { PrevButton } from './PrevButton';

function SomePage({ name }: { name: string }) {
  const { pathname } = useLocation();
  return (
    <div>
      <PrevButton />
      <h1>{name}</h1>
      <p data-testid="pathname">{pathname}</p>
    </div>
  );
}

function SomeRoutes() {
  return (
    <Routes>
      <Route path="/studies" element={<SomePage name="내 스터디 목록" />} />
      <Route path="/studies/:studyId" element={<SomePage name="스터디 상세" />} />
      <Route path="/studies/:studyId/members" element={<SomePage name="멤버 목록" />} />
      <Route path="/studies/:studyId/notices" element={<SomePage name="공지 목록" />} />
      <Route path="/studies/:studyId/notices/create" element={<SomePage name="공지 생성" />} />
      <Route path="/studies/:studyId/notices/:noticeId" element={<SomePage name="공지 상세" />} />
      <Route
        path="/studies/:studyId/notices/:noticeId/edit"
        element={<SomePage name="공지 수정" />}
      />
      <Route path="/studies/:studyId/assignments" element={<SomePage name="과제 목록" />} />
      <Route
        path="/studies/:studyId/assignments/:assignmentId"
        element={<SomePage name="과제 상세" />}
      />
      <Route
        path="/studies/:studyId/assignments/:assignmentId/submissions/:submissionId"
        element={<SomePage name="과제 제출 상세" />}
      />
    </Routes>
  );
}

function renderPrevButton(initialPath: string) {
  render(SomeRoutes(), { wrapper: createWrapper({ initialEntries: [initialPath] }) });

  return userEvent.setup();
}

function clickPrevButton(user: ReturnType<typeof userEvent.setup>) {
  return user.click(screen.getByRole('button', { name: '뒤로 가기' }));
}

describe('이전 버튼 이동 테스트', () => {
  test('공지 목록에서 클릭하면 특정 스터디 디테일로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/notices');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '스터디 상세' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1');
  });

  test('특정 공지에서 클릭하면 공지 목록 페이지로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/notices/10');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '공지 목록' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1/notices');
  });

  test('특정 공지 수정에서 클릭하면 해당 공지 상세로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/notices/10/edit');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '공지 상세' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1/notices/10');
  });

  test('공지 생성에서 클릭하면 공지 목록으로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/notices/create');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '공지 목록' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1/notices');
  });

  test('멤버 목록에서 클릭하면 스터디 상세로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/members');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '스터디 상세' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1');
  });

  test.failing('과제 제출 상세에서 클릭하면 과제 상세로 이동한다', async () => {
    // studies/1/assignments/2/submissions로 이동합니다.
    // 현재 submissions 페이지는 존재하지 않기 때문에 에러가 발생합니다.
    const user = renderPrevButton('/studies/1/assignments/2/submissions/3');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '과제 상세' })).toBeInTheDocument();
    // submission 페이지가 제공되냐 여부에따라 다르게 처리해야할거 같습니다.
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1/assignments/2');
  });

  test('스터디 상세에서 클릭하면 스터디 목록으로 이동한다', async () => {
    const user = renderPrevButton('/studies/1');

    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '내 스터디 목록' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies');
  });

  test('여러 번 클릭하면 상위 경로로 연속적으로 이동한다', async () => {
    const user = renderPrevButton('/studies/1/notices/10/edit');

    await clickPrevButton(user);
    await clickPrevButton(user);
    await clickPrevButton(user);

    expect(screen.getByRole('heading', { name: '스터디 상세' })).toBeInTheDocument();
    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1');
  });

  test('최상위 경로에서 클릭했을때 현재 페이지가 렌더링된다', async () => {
    const user = renderPrevButton('/studies');

    await clickPrevButton(user);

    expect(screen.getByTestId('pathname')).toHaveTextContent('/studies');
  });

  test.failing(
    '후행 슬래시가 있는 공지 목록에서 클릭하면 동일하게 특정 스터디 디테일로 이동한다',
    async () => {
      const user = renderPrevButton('/studies/1/notices/');

      await clickPrevButton(user);

      expect(screen.getByRole('heading', { name: '스터디 상세' })).toBeInTheDocument();
      expect(screen.getByTestId('pathname')).toHaveTextContent('/studies/1');
    },
  );
});

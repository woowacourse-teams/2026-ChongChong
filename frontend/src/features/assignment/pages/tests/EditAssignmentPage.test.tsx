import { Suspense } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import EditAssignmentPage from '../EditAssignmentPage';
import { createWrapper } from '../../../../test/render';
import { assignmentTable } from '../../mocks/db';

describe('과제수정폼 테스트', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
    assignmentTable.create({
      id: 999,
      studyId: 2,
      title: '드리블 연습',
      content: '드리블 루틴을 매일 연습해주세요.',
      submissionMethod: '링크로 제출하세요',
      closeAt: '2999-12-31T23:59:59',
      completeUserIds: [],
    });
  });

  function renderEditPage() {
    render(
      <Suspense fallback={<div>로딩중</div>}>
        <Routes>
          <Route
            path="studies/:studyId/assignments/:assignmentId/edit"
            element={<EditAssignmentPage />}
          />
        </Routes>
      </Suspense>,
      {
        wrapper: createWrapper({
          initialEntries: [`/studies/2/assignments/999/edit`],
        }),
      },
    );
  }

  async function findTitleInput() {
    return screen.findByRole('textbox', { name: '제목' });
  }

  test('필드를 비우고 수정하면 에러메시지가 표시 된다', async () => {
    renderEditPage();

    await user.clear(await findTitleInput());
    await user.click(screen.getByRole('button', { name: '과제 수정하기' }));

    expect(await screen.findByText('과제 제목은 필수입니다.')).toBeInTheDocument();
  });

  test('성공한 필드의 에러메시지는 지워진다', async () => {
    renderEditPage();

    await user.clear(await findTitleInput());
    const submitButton = screen.getByRole('button', { name: '과제 수정하기' });
    await user.click(submitButton);
    // 에러메시지 렌더 확인
    expect(await screen.findByText('과제 제목은 필수입니다.')).toBeInTheDocument();

    await user.type(await findTitleInput(), '치킨 먹고싶다');
    await user.clear(screen.getByRole('textbox', { name: '제출 방법' }));
    await user.click(submitButton);

    expect(await screen.findByText('제출 방법은 필수입니다.')).toBeInTheDocument();
    // 이전 에러메시지 제거 확인
    expect(screen.queryByText('과제 제목은 필수입니다.')).not.toBeInTheDocument();
  });
});

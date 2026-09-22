import { fireEvent, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import EditAssignmentPage from '../EditAssignmentPage';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { assignmentTable } from '../../mocks/db';
import { API_URL } from '../../../../../config';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';

const ASSIGNMENT_DETAIL_URL = `${API_URL}/studies/:studyId/assignments/:assignmentId`;

function setupEditAssignmentPage() {
  return setup(<EditAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/2/assignments/999/edit'],
      routes: (element) => (
        <Route path="/studies/:studyId/assignments/:assignmentId/edit" element={element} />
      ),
    }),
  });
}

async function findTitleInput() {
  return screen.findByRole('textbox', { name: '제목' });
}

function getSubmitButton() {
  return screen.getByRole('button', { name: '과제 수정하기' });
}

function submitForm() {
  fireEvent.submit(getSubmitButton().closest('form')!);
}

describe('과제 수정 페이지 테스트', () => {
  const leaderUserName = '피즈';

  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 2,
      name: '협곡 스터디',
      description: '피즈의 피즈 강의',
      inviteLink: 'bronze',
    });
    await memberTable.create({
      id: 1,
      studyId: 2,
      userId: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    });
    login(leaderUserName);
  });

  afterEach(() => {
    logout();
  });

  describe('과제 수정 폼', () => {
    beforeEach(async () => {
      await assignmentTable.create({
        id: 999,
        studyId: 2,
        title: '큐 궁 쓰면 궁 잘맞음',
        content: '큐 궁 쓰세요',
        submissionMethod: '링크로 제출하세요',
        closeAt: '2999-12-31T23:59:59',
        submissionTarget: 'MEMBERS_ONLY',
        completeUserIds: [],
      });
    });

    test('저장된 리드 제출 여부를 체크박스에 반영한다', async () => {
      setupEditAssignmentPage();

      await findTitleInput();
      expect(screen.getByRole('checkbox')).not.toBeChecked();
    });

    test('필드를 비우고 수정하면 에러메시지가 표시 된다', async () => {
      const { user } = setupEditAssignmentPage();

      await user.clear(await findTitleInput());
      submitForm();

      expect(await screen.findByText('과제 제목은 필수입니다.')).toBeInTheDocument();
    });

    test('성공한 필드의 에러메시지는 지워진다', async () => {
      const { user } = setupEditAssignmentPage();

      await user.clear(await findTitleInput());
      submitForm();
      expect(await screen.findByText('과제 제목은 필수입니다.')).toBeInTheDocument();

      await user.type(await findTitleInput(), '치킨 먹고싶다');
      await user.clear(screen.getByRole('textbox', { name: '제출 방법' }));
      submitForm();

      expect(await screen.findByText('제출 방법은 필수입니다.')).toBeInTheDocument();
      expect(screen.queryByText('과제 제목은 필수입니다.')).not.toBeInTheDocument();
    });

    test.each([
      {
        title: '스터디 리더가 아니면',
        handler: http.patch(ASSIGNMENT_DETAIL_URL, () =>
          HttpResponse.json(
            { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '요청한 작업을 수행할 권한이 없습니다.',
      },
      {
        title: '네트워크 에러가 발생하면',
        handler: http.patch(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()),
        message: '과제 수정에 실패했습니다.',
      },
    ])('$title 에러 메시지를 토스트로 표시하고 입력값을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupEditAssignmentPage();

      const titleInput = await findTitleInput();
      await user.clear(titleInput);
      await user.type(titleInput, '수정한 피즈 궁 연습');
      submitForm();

      const toast = await screen.findByRole('status', {}, { timeout: 3000 });
      expect(toast).toHaveTextContent(message);
      expect(toast).toBeVisible();
      expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('수정한 피즈 궁 연습');
    });
  });

  describe('과제 조회 실패', () => {
    beforeEach(() => {
      jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    test.each([
      {
        title: '스터디에 대한 접근 권한이 없으면',
        handler: http.get(ASSIGNMENT_DETAIL_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '네트워크 에러가 발생하면',
        handler: http.get(ASSIGNMENT_DETAIL_URL, () => HttpResponse.error()),
        message: '과제 정보를 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupEditAssignmentPage();

      expect(await screen.findByText(message)).toBeVisible();
      expect(within(screen.getByRole('main')).getByText(message)).toBeVisible();
    });
  });
});

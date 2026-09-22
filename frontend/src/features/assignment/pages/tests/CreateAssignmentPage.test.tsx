import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import CreateAssignmentPage from '../CreateAssignmentPage';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { API_URL } from '../../../../../config';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';

const ASSIGNMENT_CREATE_URL = `${API_URL}/studies/:studyId/assignments`;

function setupCreateAssignmentPage() {
  return setup(<CreateAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/create'],
      routes: (element) => <Route path="/studies/:studyId/assignments/create" element={element} />,
    }),
  });
}

function getTitleInput() {
  return screen.getByRole('textbox', { name: '제목' });
}

function getSubmitButton() {
  return screen.getByRole('button', { name: '과제 올리기' });
}

describe('과제 생성 페이지 테스트', () => {
  const leaderUserName = '안톨리니';

  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 1,
      name: '객체지향 스터디',
      description: '객체지향 설계를 함께 연습하는 스터디',
      inviteLink: 'object-oriented-study',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
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

  describe('과제 입력 검증', () => {
    test.each([
      { leaderSubmits: true, submissionTarget: 'MEMBERS_AND_LEADER' },
      { leaderSubmits: false, submissionTarget: 'MEMBERS_ONLY' },
    ])(
      '리드 제출 $leaderSubmits 선택을 생성 요청에 반영한다',
      async ({ leaderSubmits, submissionTarget }) => {
        const requestBody = jest.fn();
        server.use(
          http.post(ASSIGNMENT_CREATE_URL, async ({ request }) => {
            requestBody(await request.json());
            return HttpResponse.json({ assignmentId: 999 }, { status: 201 });
          }),
        );
        const { user } = setupCreateAssignmentPage();
        const checkbox = screen.getByRole('checkbox');

        await user.type(getTitleInput(), '객체지향 설계 과제');
        await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
        await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
        expect(checkbox).toHaveProperty('checked', true);
        if (!leaderSubmits) await user.click(checkbox);
        await user.click(getSubmitButton());

        await waitFor(() =>
          expect(requestBody).toHaveBeenCalledWith(expect.objectContaining({ submissionTarget })),
        );
      },
    );

    test('제목 입력은 20자로 제한된다', async () => {
      const { user } = setupCreateAssignmentPage();

      const titleInput = getTitleInput();
      await user.type(titleInput, '안톨리니'.repeat(20));

      expect(titleInput).toHaveValue('안톨리니'.repeat(5));
    });

    test('내용 입력은 10000자로 제한된다', async () => {
      const { user } = setupCreateAssignmentPage();

      const contentInput = screen.getByRole('textbox', { name: '내용' });
      await user.click(contentInput);
      await user.paste('안'.repeat(20000));

      expect(contentInput).toHaveValue('안'.repeat(10000));
    });

    test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
      server.use(
        http.post(ASSIGNMENT_CREATE_URL, () =>
          invalidInputResponse([
            { field: 'title', code: 'INVALID', reason: '제목이 이상해요' },
            { field: 'content', code: 'INVALID', reason: '내용이 이상해요' },
            { field: 'submissionMethod', code: 'INVALID', reason: '제출방식이 이상해요' },
            { field: 'closeAt', code: 'INVALID', reason: '마감 시각이 이상해요' },
          ]),
        ),
      );
      const { user } = setupCreateAssignmentPage();

      await user.type(getTitleInput(), '객체지향 설계 과제');
      await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
      await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
      await user.click(getSubmitButton());

      expect(await screen.findByText('제목이 이상해요')).toBeInTheDocument();
      expect(await screen.findByText('내용이 이상해요')).toBeInTheDocument();
      expect(await screen.findByText('제출방식이 이상해요')).toBeInTheDocument();
      expect(await screen.findByText('마감 시각이 이상해요')).toBeInTheDocument();
    });
  });

  describe('과제 생성 실패', () => {
    test.each([
      {
        title: '스터디 리더가 아니면',
        handler: http.post(ASSIGNMENT_CREATE_URL, () =>
          HttpResponse.json(
            { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '요청한 작업을 수행할 권한이 없습니다.',
      },
      {
        title: '네트워크 에러가 발생하면',
        handler: http.post(ASSIGNMENT_CREATE_URL, () => HttpResponse.error()),
        message: '과제를 생성하는데 실패했습니다.',
      },
    ])('$title 에러 메시지를 토스트로 표시하고 입력값을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupCreateAssignmentPage();

      await user.type(getTitleInput(), '객체지향 설계 과제');
      await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
      await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
      await user.click(getSubmitButton());

      const toast = await screen.findByRole('status');
      expect(toast).toHaveTextContent(message);
      expect(toast).toBeVisible();
      expect(getTitleInput()).toHaveValue('객체지향 설계 과제');
    });
  });
});

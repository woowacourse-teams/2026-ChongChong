import { screen, waitFor, fireEvent, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, Route, RouterProvider } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CreateAssignmentPage from '../CreateAssignmentPage';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { createTextWithLength, insertTextInMiddle } from '../../../../test/input';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { API_URL } from '../../../../../config';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { ToastProvider } from '../../../../shared/providers/ToastProvider';

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

  test('제출 인원을 선택하고 공개 범위를 바꾼 뒤 이전 단계로 돌아가도 선택을 유지한다', async () => {
    const requestBody = jest.fn();
    server.use(
      http.post(ASSIGNMENT_CREATE_URL, async ({ request }) => {
        requestBody(await request.json());
        return invalidInputResponse([
          { field: 'submissionVisibility', code: 'INVALID', reason: '공개 범위를 확인해주세요.' },
        ]);
      }),
    );
    await memberTable.create({
      id: 2,
      studyId: 1,
      userId: 2,
      name: '피즈',
      profileImage: null,
      role: 'MEMBER',
    });
    const { user } = setupCreateAssignmentPage();

    expect(screen.queryByText('제목을 입력해주세요.')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByText('제목을 입력해주세요.')).toBeVisible();
    expect(screen.getByText('내용을 입력해주세요.')).toBeVisible();
    expect(screen.getByText('제출 방법을 입력해주세요.')).toBeVisible();
    await user.type(getTitleInput(), '과제 제목');
    await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '   ');
    expect(screen.queryByText('제목을 입력해주세요.')).not.toBeInTheDocument();
    expect(screen.queryByText('내용을 입력해주세요.')).not.toBeInTheDocument();
    expect(screen.getByText('제출 방법을 입력해주세요.')).toBeVisible();
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByRole('textbox', { name: '제출 방법' })).toBeVisible();
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
    expect(screen.queryByText('제출 방법을 입력해주세요.')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.queryByText('마감 시각을 선택해주세요.')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByText('마감 시각을 선택해주세요.')).toBeVisible();
    await user.click(screen.getByRole('button', { name: '마감 시각' }));
    const dialog = within(screen.getByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: '다음' }));
    await user.click(dialog.getByRole('button', { name: '완료' }));
    expect(screen.queryByText('마감 시각을 선택해주세요.')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(await screen.findByRole('checkbox', { name: '전체 선택' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByText('제출할 멤버를 한 명 이상 선택해주세요.')).toBeVisible();
    await user.click(screen.getByRole('checkbox', { name: '피즈' }));
    expect(screen.queryByText('제출할 멤버를 한 명 이상 선택해주세요.')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '다음' }));

    expect(screen.getByText('제출 인원 1명 · 피즈')).toBeVisible();
    await user.click(screen.getByRole('radio', { name: '제출한 사람과 리더만' }));
    await user.click(screen.getByRole('button', { name: '뒤로 가기' }));
    expect(screen.getByRole('checkbox', { name: '피즈' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: '안톨리니 (나)' })).not.toBeChecked();
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByRole('radio', { name: '제출한 사람과 리더만' })).toBeChecked();
    await user.click(screen.getByRole('button', { name: '과제 만들기' }));
    await waitFor(() => expect(requestBody).toHaveBeenCalledTimes(1));
    expect(requestBody.mock.calls[0][0]).toMatchObject({
      title: '과제 제목',
      content: '과제 내용',
      submitterIds: [2],
      submissionVisibility: 'LEADER_ONLY',
      attachmentFileIds: [],
    });
    expect(requestBody.mock.calls[0][0]).not.toHaveProperty('submissionTarget');
    expect(await screen.findByText('공개 범위를 확인해주세요.')).toBeVisible();
  });

  async function proceedToVisibility(user: ReturnType<typeof setupCreateAssignmentPage>['user']) {
    await user.type(getTitleInput(), '과제 제목');
    await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '마감 시각' }));
    const dialog = within(screen.getByRole('dialog'));
    await user.click(dialog.getByRole('button', { name: '다음' }));
    await user.click(dialog.getByRole('button', { name: '완료' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    await screen.findByRole('checkbox', { name: '전체 선택' });
    await user.click(screen.getByRole('button', { name: '다음' }));
  }

  test.each([
    ['title', '어떤 과제를 함께 할까요?'],
    ['remindAts[0]', '과제 일정을 정해주세요'],
    ['submitterIds', '누가 과제를 제출하나요?'],
  ])('서버의 %s 오류가 있는 단계로 돌아간다', async (field, heading) => {
    server.use(
      http.post(ASSIGNMENT_CREATE_URL, () =>
        invalidInputResponse([{ field, code: 'INVALID', reason: '입력값을 확인해주세요.' }]),
      ),
    );
    const { user } = setupCreateAssignmentPage();
    await proceedToVisibility(user);
    await user.click(screen.getByRole('button', { name: '과제 만들기' }));
    expect(await screen.findByRole('heading', { name: heading })).toBeVisible();
    expect(screen.getByText('입력값을 확인해주세요.')).toBeVisible();
    if (field === 'title') expect(getTitleInput()).toHaveValue('과제 제목');
  });

  test('이미지 업로드 중이거나 실패하면 생성을 막고 삭제하면 다시 허용한다', async () => {
    jest.spyOn(URL, 'createObjectURL').mockReturnValue('blob:assignment-image');
    jest.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    let failUpload!: () => void;
    const uploadPending = new Promise<void>((resolve) => {
      failUpload = resolve;
    });
    const createRequest = jest.fn();
    server.use(
      http.post(`${API_URL}/files/presigned-url`, async () => {
        await uploadPending;
        return HttpResponse.json({ message: '업로드 실패' }, { status: 400 });
      }),
      http.post(ASSIGNMENT_CREATE_URL, () => {
        createRequest();
        return HttpResponse.json({ assignmentId: 1 }, { status: 201 });
      }),
    );
    const { user } = setupCreateAssignmentPage();
    await user.upload(
      document.querySelector<HTMLInputElement>('input[type="file"]')!,
      new File(['image'], '과제.png', { type: 'image/png' }),
    );
    await proceedToVisibility(user);
    expect(screen.getByRole('button', { name: '과제 만들기' })).toBeDisabled();
    expect(
      screen.getByText(
        '이미지 업로드가 완료되지 않았어요. 내용 단계에서 업로드 상태를 확인해주세요.',
      ),
    ).toBeVisible();
    failUpload();
    for (let i = 0; i < 3; i++) await user.click(screen.getByRole('button', { name: '뒤로 가기' }));
    await screen.findByText('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.');
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByRole('button', { name: '과제 만들기' })).toBeDisabled();
    expect(createRequest).not.toHaveBeenCalled();
    for (let i = 0; i < 3; i++) await user.click(screen.getByRole('button', { name: '뒤로 가기' }));
    await user.click(screen.getByRole('button', { name: '과제.png 삭제' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    await user.click(screen.getByRole('button', { name: '다음' }));
    expect(screen.getByRole('button', { name: '과제 만들기' })).toBeEnabled();
  });

  describe('과제 입력 검증', () => {
    test.each([
      { label: '공개', submissionVisibility: 'ALL_STUDY_MEMBERS' },
      { label: '비공개', submissionVisibility: 'LEADER_ONLY' },
    ])('$label 선택을 생성 요청에 반영한다', async ({ label, submissionVisibility }) => {
      const requestBody = jest.fn();
      server.use(
        http.post(ASSIGNMENT_CREATE_URL, async ({ request }) => {
          requestBody(await request.json());
          return HttpResponse.json({ assignmentId: 999 }, { status: 201 });
        }),
      );
      const { user } = setupCreateAssignmentPage();

      await user.type(getTitleInput(), '객체지향 설계 과제');
      await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
      await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
      await user.click(screen.getByRole('radio', { name: label }));
      await user.click(getSubmitButton());

      await waitFor(() =>
        expect(requestBody).toHaveBeenCalledWith(expect.objectContaining({ submissionVisibility })),
      );
    });

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

    test('빈 제목에 100자를 초과한 값이 전달되면 입력을 거절한다', () => {
      setupCreateAssignmentPage();
      const titleInput = getTitleInput();

      fireEvent.change(titleInput, { target: { value: createTextWithLength(101) } });

      expect(titleInput).toHaveValue('');
    });

    test('제목이 100자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
      setupCreateAssignmentPage();
      const titleInput = getTitleInput();
      const originalValue = createTextWithLength(100);

      fireEvent.change(titleInput, { target: { value: originalValue } });
      expect(titleInput).toHaveValue(originalValue);

      insertTextInMiddle(titleInput, originalValue);

      expect(titleInput).toHaveValue(originalValue);
    });

    test('제목의 중간 삽입 결과가 100자 이하면 입력을 반영한다', () => {
      setupCreateAssignmentPage();
      const titleInput = getTitleInput();
      const originalValue = createTextWithLength(99);

      fireEvent.change(titleInput, { target: { value: originalValue } });
      expect(titleInput).toHaveValue(originalValue);

      const insertedValue = insertTextInMiddle(titleInput, originalValue);

      expect(titleInput).toHaveValue(insertedValue);
    });

    test('빈 내용에 10000자를 초과한 값이 전달되면 입력을 거절한다', () => {
      setupCreateAssignmentPage();
      const contentInput = screen.getByRole('textbox', { name: '내용' });

      fireEvent.change(contentInput, { target: { value: createTextWithLength(10001) } });

      expect(contentInput).toHaveValue('');
    });

    test('내용이 10000자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
      setupCreateAssignmentPage();
      const contentInput = screen.getByRole('textbox', { name: '내용' });
      const originalValue = createTextWithLength(10000);

      fireEvent.change(contentInput, { target: { value: originalValue } });
      expect(contentInput).toHaveValue(originalValue);

      insertTextInMiddle(contentInput, originalValue);

      expect(contentInput).toHaveValue(originalValue);
    });

    test('내용의 중간 삽입 결과가 10000자 이하면 입력을 반영한다', () => {
      setupCreateAssignmentPage();
      const contentInput = screen.getByRole('textbox', { name: '내용' });
      const originalValue = createTextWithLength(9999);

      fireEvent.change(contentInput, { target: { value: originalValue } });
      expect(contentInput).toHaveValue(originalValue);

      const insertedValue = insertTextInMiddle(contentInput, originalValue);

      expect(contentInput).toHaveValue(insertedValue);
    });

    test('빈 제출 방법에 10000자를 초과한 값이 전달되면 입력을 거절한다', () => {
      setupCreateAssignmentPage();
      const submissionMethodInput = screen.getByRole('textbox', { name: '제출 방법' });

      fireEvent.change(submissionMethodInput, { target: { value: createTextWithLength(10001) } });

      expect(submissionMethodInput).toHaveValue('');
    });

    test('제출 방법이 10000자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
      setupCreateAssignmentPage();
      const submissionMethodInput = screen.getByRole('textbox', { name: '제출 방법' });
      const originalValue = createTextWithLength(10000);

      fireEvent.change(submissionMethodInput, { target: { value: originalValue } });
      expect(submissionMethodInput).toHaveValue(originalValue);

      insertTextInMiddle(submissionMethodInput, originalValue);

      expect(submissionMethodInput).toHaveValue(originalValue);
    });

    test('제출 방법의 중간 삽입 결과가 10000자 이하면 입력을 반영한다', () => {
      setupCreateAssignmentPage();
      const submissionMethodInput = screen.getByRole('textbox', { name: '제출 방법' });
      const originalValue = createTextWithLength(9999);

      fireEvent.change(submissionMethodInput, { target: { value: originalValue } });
      expect(submissionMethodInput).toHaveValue(originalValue);

      const insertedValue = insertTextInMiddle(submissionMethodInput, originalValue);

      expect(submissionMethodInput).toHaveValue(insertedValue);
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

  test('상세 페이지를 불러오는 동안 제출 버튼을 비활성화한다', async () => {
    const createRequest = jest.fn();
    server.use(
      http.post(ASSIGNMENT_CREATE_URL, () => {
        createRequest();
        return HttpResponse.json({ assignmentId: 999 }, { status: 201 });
      }),
    );

    let finishDetailLoad: () => void = () => {};
    const detailLoaded = new Promise<void>((resolve) => {
      finishDetailLoad = resolve;
    });
    const loadDetail = jest.fn(async () => {
      await detailLoaded;
      return { Component: () => <div>과제 상세</div> };
    });
    const router = createMemoryRouter(
      [
        { path: '/studies/:studyId/assignments/create', element: <CreateAssignmentPage /> },
        { path: '/studies/:studyId/assignments/:assignmentId', lazy: loadDetail },
      ],
      { initialEntries: ['/studies/1/assignments/create'] },
    );
    const { user } = setup(
      <QueryClientProvider client={new QueryClient()}>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </QueryClientProvider>,
    );

    await user.type(getTitleInput(), '객체지향 설계 과제');
    await user.type(screen.getByRole('textbox', { name: '내용' }), '과제 내용');
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
    const submitButton = getSubmitButton();
    await user.click(submitButton);

    await waitFor(() => expect(loadDetail).toHaveBeenCalled());
    expect(submitButton).toBeDisabled();
    await user.click(submitButton);
    expect(createRequest).toHaveBeenCalledTimes(1);

    finishDetailLoad();
    expect(await screen.findByText('과제 상세')).toBeInTheDocument();
  });
});

import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, Route, RouterProvider, Routes } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import { server } from '../../../../mocks/msw-node';
import { createTextWithLength, insertTextInMiddle } from '../../../../test/input';
import { createWrapper } from '../../../../test/render';
import CreateNoticePage from '../CreateNoticePage';
import { ToastProvider } from '../../../../shared/providers/ToastProvider';

describe('공지 생성 폼', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  function renderCreatePage() {
    render(
      <Routes>
        <Route path="studies/:studyId/notices/create" element={<CreateNoticePage />} />
      </Routes>,
      { wrapper: createWrapper({ initialEntries: ['/studies/1/notices/create'] }) },
    );
  }

  test('제목 빈 값에서 100자 제한을 초과한 값이 전달되면 빈 값을 유지한다', () => {
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    fireEvent.change(titleInput, {
      target: { value: createTextWithLength(101) },
    });

    expect(titleInput).toHaveValue('');
  });

  test('제목이 100자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    const originalValue = createTextWithLength(100);
    fireEvent.change(titleInput, { target: { value: originalValue } });
    expect(titleInput).toHaveValue(originalValue);

    insertTextInMiddle(titleInput, originalValue);

    expect(titleInput).toHaveValue(originalValue);
  });

  test('제목의 중간 삽입 결과가 100자 이하면 입력을 반영한다', () => {
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    const originalValue = createTextWithLength(99);
    fireEvent.change(titleInput, { target: { value: originalValue } });
    expect(titleInput).toHaveValue(originalValue);

    const insertedValue = insertTextInMiddle(titleInput, originalValue);

    expect(titleInput).toHaveValue(insertedValue);
  });

  test('내용 빈 값에서 10000자 제한을 초과한 값이 전달되면 빈 값을 유지한다', () => {
    renderCreatePage();

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    fireEvent.change(contentInput, {
      target: { value: createTextWithLength(10001) },
    });

    expect(contentInput).toHaveValue('');
  });

  test('내용이 10000자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
    renderCreatePage();

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    const originalValue = createTextWithLength(10000);
    fireEvent.change(contentInput, { target: { value: originalValue } });
    expect(contentInput).toHaveValue(originalValue);

    insertTextInMiddle(contentInput, originalValue);

    expect(contentInput).toHaveValue(originalValue);
  });

  test('내용의 중간 삽입 결과가 10000자 이하면 입력을 반영한다', () => {
    renderCreatePage();

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    const originalValue = createTextWithLength(9999);
    fireEvent.change(contentInput, { target: { value: originalValue } });
    expect(contentInput).toHaveValue(originalValue);

    const insertedValue = insertTextInMiddle(contentInput, originalValue);

    expect(contentInput).toHaveValue(insertedValue);
  });

  test('필드 에러가 발생하면 각 필드의 에러 메시지를 표시한다', async () => {
    server.use(
      http.post(`${API_URL}/studies/:studyId/notices`, () =>
        invalidInputResponse([
          { field: 'title', code: 'INVALID', reason: '제목이 이상해요' },
          { field: 'content', code: 'INVALID', reason: '내용이 이상해요' },
        ]),
      ),
    );

    renderCreatePage();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '공지 제목');
    await user.type(screen.getByRole('textbox', { name: '내용' }), '공지 내용');
    await user.click(screen.getByRole('button', { name: '공지 올리기' }));

    expect(await screen.findByText('제목이 이상해요')).toBeInTheDocument();
    expect(await screen.findByText('내용이 이상해요')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('스터디 리더가 아니면 공지 생성 권한 안내를 토스트로 표시한다', async () => {
    server.use(
      http.post(`${API_URL}/studies/:studyId/notices`, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderCreatePage();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '스터디 일정 안내');
    await user.type(
      screen.getByRole('textbox', { name: '내용' }),
      '이번 주는 토요일에 진행합니다.',
    );
    await user.click(screen.getByRole('button', { name: '공지 올리기' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('요청한 작업을 수행할 권한이 없습니다.');
    expect(toast).toBeVisible();
  });

  test('네트워크 에러가 발생하면 공지 생성 실패 안내를 토스트로 표시한다', async () => {
    server.use(http.post(`${API_URL}/studies/:studyId/notices`, () => HttpResponse.error()));
    renderCreatePage();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '스터디 일정 안내');
    await user.type(
      screen.getByRole('textbox', { name: '내용' }),
      '이번 주는 토요일에 진행합니다.',
    );
    await user.click(screen.getByRole('button', { name: '공지 올리기' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('공지 생성에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('스터디 일정 안내');
  });

  test('상세 페이지를 불러오는 동안 제출 버튼을 비활성화한다', async () => {
    const createRequest = jest.fn();
    server.use(
      http.post(`${API_URL}/studies/:studyId/notices`, () => {
        createRequest();
        return HttpResponse.json({ noticeId: 999 }, { status: 201 });
      }),
    );

    let finishDetailLoad: () => void = () => {};
    const detailLoaded = new Promise<void>((resolve) => {
      finishDetailLoad = resolve;
    });
    const loadDetail = jest.fn(async () => {
      await detailLoaded;
      return { Component: () => <div>공지 상세</div> };
    });
    const router = createMemoryRouter(
      [
        { path: '/studies/:studyId/notices/create', element: <CreateNoticePage /> },
        { path: '/studies/:studyId/notices/:noticeId', lazy: loadDetail },
      ],
      { initialEntries: ['/studies/1/notices/create'] },
    );

    render(
      <QueryClientProvider client={new QueryClient()}>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </QueryClientProvider>,
    );

    await user.type(screen.getByRole('textbox', { name: '제목' }), '공지 제목');
    await user.type(screen.getByRole('textbox', { name: '내용' }), '공지 내용');
    const submitButton = screen.getByRole('button', { name: '공지 올리기' });
    await user.click(submitButton);

    await waitFor(() => expect(loadDetail).toHaveBeenCalled());
    expect(submitButton).toBeDisabled();
    await user.click(submitButton);
    expect(createRequest).toHaveBeenCalledTimes(1);

    finishDetailLoad();
    expect(await screen.findByText('공지 상세')).toBeInTheDocument();
  });
});

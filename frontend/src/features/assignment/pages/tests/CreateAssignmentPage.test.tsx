import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import CreateAssignmentPage from '../CreateAssignmentPage';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { API_URL } from '../../../../../config';

describe('과제생성폼 테스트', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
  });

  function renderCreatePage() {
    render(
      <Routes>
        <Route path={'studies/:studyId/assignments/create'} element={<CreateAssignmentPage />} />
      </Routes>,
      { wrapper: createWrapper({ initialEntries: ['/studies/1/assignments/create'] }) },
    );
  }

  test('제목 입력은 20자로 제한된다', async () => {
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    await user.type(titleInput, '안톨리니'.repeat(20));

    expect(titleInput).toHaveValue('안톨리니'.repeat(5));
  });

  test('내용 입력은 10000자로 제한된다', async () => {
    renderCreatePage();

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    await user.click(contentInput);
    await user.paste('안'.repeat(20000));

    expect(contentInput).toHaveValue('안'.repeat(10000));
  });

  test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
    server.use(
      http.post(`${API_URL}/studies/:studyId/assignments`, () =>
        invalidInputResponse([
          { field: 'title', code: 'INVALID', reason: '제목이 이상해요' },
          { field: 'content', code: 'INVALID', reason: '내용이 이상해요' },
          { field: 'submissionMethod', code: 'INVALID', reason: '제출방식이 이상해요' },
          { field: 'closeAt', code: 'INVALID', reason: '마감 시각이 이상해요' },
        ]),
      ),
    );
    renderCreatePage();
    const submitButton = screen.getByRole('button', { name: '과제 올리기' });
    await user.click(submitButton);

    expect(await screen.findByText('제목이 이상해요')).toBeInTheDocument();
    expect(await screen.findByText('내용이 이상해요')).toBeInTheDocument();
    expect(await screen.findByText('제출방식이 이상해요')).toBeInTheDocument();
    expect(await screen.findByText('마감 시각이 이상해요')).toBeInTheDocument();
  });

  test('스터디 리더가 아니면 과제 생성 권한 안내를 토스트로 표시한다', async () => {
    server.use(
      http.post(`${API_URL}/studies/:studyId/assignments`, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderCreatePage();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '객체지향 설계 과제');
    await user.click(screen.getByRole('button', { name: '과제 올리기' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('요청한 작업을 수행할 권한이 없습니다.');
    expect(toast).toBeVisible();
  });

  test('네트워크 에러가 발생하면 과제 생성 실패 안내를 토스트로 표시한다', async () => {
    server.use(http.post(`${API_URL}/studies/:studyId/assignments`, () => HttpResponse.error()));
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    await user.type(titleInput, '객체지향 설계 과제');
    await user.click(screen.getByRole('button', { name: '과제 올리기' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('과제를 생성하는데 실패했습니다.');
    expect(toast).toBeVisible();
    expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('객체지향 설계 과제');
  });
});

import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import CreateNoticePage from '../CreateNoticePage';

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

  test('제목에 제한을 초과한 값이 전달되면 입력 수를 20자로 제한한다', () => {
    renderCreatePage();

    const titleInput = screen.getByRole('textbox', { name: '제목' });
    fireEvent.change(titleInput, {
      target: { value: '가'.repeat(21) },
    });

    expect(titleInput).toHaveValue('가'.repeat(20));
  });

  test('내용에 제한을 초과한 값이 전달되면 입력 수를 10000자로 제한한다', () => {
    renderCreatePage();

    const contentInput = screen.getByRole('textbox', { name: '내용' });
    fireEvent.change(contentInput, {
      target: { value: '가'.repeat(10001) },
    });

    expect(contentInput).toHaveValue('가'.repeat(10000));
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
});

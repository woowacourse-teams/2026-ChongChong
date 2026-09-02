import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http } from 'msw';
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
  });
});

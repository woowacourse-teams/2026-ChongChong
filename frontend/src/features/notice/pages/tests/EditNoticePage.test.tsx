import { Suspense } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import EditNoticePage from '../EditNoticePage';

describe('공지 수정 폼', () => {
  test('필수 필드를 비우고 수정하면 서버의 필드 에러 메시지를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}/studies/:studyId/notices/:noticeId`, () =>
        HttpResponse.json({
          id: 10,
          title: '기존 공지',
          writer: '스터디장',
          profileImageUrl: null,
          content: '기존 내용',
          createdAt: '2026-09-02T00:00:00',
        }),
      ),
      http.patch(`${API_URL}/studies/:studyId/notices/:noticeId`, async ({ request }) => {
        expect(await request.json()).toEqual({ title: '', content: '' });

        return invalidInputResponse([
          { field: 'title', code: 'REQUIRED', reason: '공지 제목은 필수입니다.' },
          { field: 'content', code: 'REQUIRED', reason: '공지 내용은 필수입니다.' },
        ]);
      }),
    );

    const user = userEvent.setup();
    render(
      <Suspense fallback={<div>로딩중</div>}>
        <Routes>
          <Route path="studies/:studyId/notices/:noticeId/edit" element={<EditNoticePage />} />
        </Routes>
      </Suspense>,
      { wrapper: createWrapper({ initialEntries: ['/studies/1/notices/10/edit'] }) },
    );

    await user.clear(await screen.findByRole('textbox', { name: '제목' }));
    await user.clear(screen.getByRole('textbox', { name: '내용' }));
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    expect(await screen.findByText('공지 제목은 필수입니다.')).toBeInTheDocument();
    expect(await screen.findByText('공지 내용은 필수입니다.')).toBeInTheDocument();
  });
});

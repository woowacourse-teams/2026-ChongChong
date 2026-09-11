import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import EditNoticePage from '../EditNoticePage';

function renderEditPage() {
  render(
    <Routes>
      <Route path="studies/:studyId/notices/:noticeId/edit" element={<EditNoticePage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1/notices/10/edit'] }) },
  );
}

describe('공지 수정 폼', () => {
  let user: ReturnType<typeof userEvent.setup>;

  beforeEach(() => {
    user = userEvent.setup();
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
    );
  });

  test('필수 필드를 비우고 수정하면 서버의 필드 에러 메시지를 표시한다', async () => {
    server.use(
      http.patch(`${API_URL}/studies/:studyId/notices/:noticeId`, async ({ request }) => {
        expect(await request.json()).toEqual({ title: '', content: '' });

        return invalidInputResponse([
          { field: 'title', code: 'REQUIRED', reason: '공지 제목은 필수입니다.' },
          { field: 'content', code: 'REQUIRED', reason: '공지 내용은 필수입니다.' },
        ]);
      }),
    );

    renderEditPage();

    await user.clear(await screen.findByRole('textbox', { name: '제목' }));
    await user.clear(screen.getByRole('textbox', { name: '내용' }));
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    expect(await screen.findByText('공지 제목은 필수입니다.')).toBeInTheDocument();
    expect(await screen.findByText('공지 내용은 필수입니다.')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('스터디 리더가 아니면 공지 수정 권한 안내를 토스트로 표시한다', async () => {
    server.use(
      http.patch(`${API_URL}/studies/:studyId/notices/:noticeId`, () =>
        HttpResponse.json(
          { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderEditPage();

    const titleInput = await screen.findByRole('textbox', { name: '제목' });
    await user.clear(titleInput);
    await user.type(titleInput, '수정한 공지');
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('요청한 작업을 수행할 권한이 없습니다.');
    expect(toast).toBeVisible();
  });

  test('네트워크 에러가 발생하면 공지 수정 실패 안내를 토스트로 표시한다', async () => {
    server.use(
      http.patch(`${API_URL}/studies/:studyId/notices/:noticeId`, () => HttpResponse.error()),
    );
    renderEditPage();

    const titleInput = await screen.findByRole('textbox', { name: '제목' });
    await user.clear(titleInput);
    await user.type(titleInput, '수정한 공지');
    await user.click(screen.getByRole('button', { name: '수정하기' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('공지 수정에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('수정한 공지');
  });
});

describe('공지 수정 페이지 조회 실패', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('스터디에 대한 접근 권한이 없으면 본문에 접근 권한 안내를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}/studies/:studyId/notices/:noticeId`, () =>
        HttpResponse.json(
          { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
          { status: 403 },
        ),
      ),
    );
    renderEditPage();

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('해당 스터디에 대한 접근 권한이 없습니다.'),
    ).toBeVisible();
  });

  test('네트워크 에러가 발생하면 본문에 공지 조회 실패 안내를 표시한다', async () => {
    server.use(
      http.get(`${API_URL}/studies/:studyId/notices/:noticeId`, () => HttpResponse.error()),
    );
    renderEditPage();

    expect(
      await screen.findByText('공지 정보를 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(
      within(screen.getByRole('main')).getByText('공지 정보를 불러오는데 실패했습니다.'),
    ).toBeVisible();
  });
});

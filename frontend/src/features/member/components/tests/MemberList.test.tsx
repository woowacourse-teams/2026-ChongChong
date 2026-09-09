import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Suspense } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { Route, Routes } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import { MEMBER_URLS } from '../../urls';
import MemberListContent from '../MemberListContent';

function renderMemberListContent(content: React.ReactNode) {
  render(
    <ErrorBoundary fallback={<p>목록 밖으로 전파된 오류</p>}>
      <Suspense fallback={null}>
        <Routes>
          <Route path="/studies/:studyId" element={content} />
        </Routes>
      </Suspense>
    </ErrorBoundary>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

describe.each([
  {
    role: '리드',
    Content: MemberListContent.Leader,
    actionName: '스터디 삭제하기',
  },
  {
    role: '스터디원',
    Content: MemberListContent.Member,
    actionName: '스터디 탈퇴하기',
  },
])('$role 멤버 목록 오류', ({ Content, actionName }) => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(
      http.get(`${API_URL}${STUDY_URLS.inviteLink}`, () =>
        HttpResponse.json({
          inviteLink: 'https://chongchong.app/join?token=member-list-test',
        }),
      ),
    );
  });

  test('현재 해당 스터디 멤버가 아닌경우 권한 에러를 목록에 표시하고 나머지 화면을 유지한다', async () => {
    server.use(
      http.get(`${API_URL}${MEMBER_URLS.list}`, () =>
        HttpResponse.json(
          {
            code: 'STUDY_ACCESS_DENIED',
            message: '해당 스터디에 대한 접근 권한이 없습니다.',
          },
          { status: 403 },
        ),
      ),
    );

    renderMemberListContent(<Content />);

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: actionName })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });

  test('네트워크 오류가 발생했을때 에러를 목록에 표시하고 나머지 화면을 유지한다', async () => {
    server.use(http.get(`${API_URL}${MEMBER_URLS.list}`, () => HttpResponse.error()));

    renderMemberListContent(<Content />);

    expect(
      await screen.findByText('멤버 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: actionName })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });
});

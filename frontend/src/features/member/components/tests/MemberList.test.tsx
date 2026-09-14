import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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

async function findInviteLinkErrorUI() {
  const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
  const copyButton = screen.getByRole('button', { name: '링크 복사' });

  return { alert, copyButton };
}

describe('리드 멤버 목록 오류', () => {
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

    renderMemberListContent(<MemberListContent.Leader />);

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });

  test('네트워크 오류가 발생했을때 에러를 목록에 표시하고 나머지 화면을 유지한다', async () => {
    server.use(http.get(`${API_URL}${MEMBER_URLS.list}`, () => HttpResponse.error()));

    renderMemberListContent(<MemberListContent.Leader />);

    expect(
      await screen.findByText('멤버 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });
});

describe('스터디원 멤버 목록 오류', () => {
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

    renderMemberListContent(<MemberListContent.Member />);

    expect(await screen.findByText('해당 스터디에 대한 접근 권한이 없습니다.')).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });

  test('네트워크 오류가 발생했을때 에러를 목록에 표시하고 나머지 화면을 유지한다', async () => {
    server.use(http.get(`${API_URL}${MEMBER_URLS.list}`, () => HttpResponse.error()));

    renderMemberListContent(<MemberListContent.Member />);

    expect(
      await screen.findByText('멤버 목록을 불러오는데 실패했습니다.', {}, { timeout: 3000 }),
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('https://chongchong.app/join?token=member-list-test')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
    expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
  });
});

describe('리드 초대 링크 오류', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('권한 오류를 표시하고 복사를 막는다', async () => {
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText');
    server.use(
      http.get(`${API_URL}${STUDY_URLS.inviteLink}`, () =>
        HttpResponse.json(
          {
            code: 'STUDY_ACCESS_DENIED',
            message: '해당 스터디에 대한 접근 권한이 없습니다.',
          },
          { status: 403 },
        ),
      ),
    );

    renderMemberListContent(<MemberListContent.Leader />);

    const { alert, copyButton } = await findInviteLinkErrorUI();

    expect(alert).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
    expect(alert).toBeVisible();
    expect(copyButton).toBeDisabled();
    await user.click(copyButton);
    expect(writeText).not.toHaveBeenCalled();

    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('안톨리니')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
    expect(screen.queryByText('목록 밖으로 전파된 오류')).not.toBeInTheDocument();
  });

  test('네트워크 오류를 표시하고 복사를 막는다', async () => {
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText');
    server.use(http.get(`${API_URL}${STUDY_URLS.inviteLink}`, () => HttpResponse.error()));

    renderMemberListContent(<MemberListContent.Leader />);

    const { alert, copyButton } = await findInviteLinkErrorUI();

    expect(alert).toHaveTextContent('초대 링크를 가져오는데 실패했습니다.');
    expect(alert).toBeVisible();
    expect(copyButton).toBeDisabled();
    await user.click(copyButton);
    expect(writeText).not.toHaveBeenCalled();

    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('안톨리니')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
    expect(screen.queryByText('목록 밖으로 전파된 오류')).not.toBeInTheDocument();
  });
});

describe('스터디원 초대 링크 오류', () => {
  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  test('권한 오류를 표시하고 복사를 막는다', async () => {
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText');
    server.use(
      http.get(`${API_URL}${STUDY_URLS.inviteLink}`, () =>
        HttpResponse.json(
          {
            code: 'STUDY_ACCESS_DENIED',
            message: '해당 스터디에 대한 접근 권한이 없습니다.',
          },
          { status: 403 },
        ),
      ),
    );

    renderMemberListContent(<MemberListContent.Member />);

    const { alert, copyButton } = await findInviteLinkErrorUI();

    expect(alert).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
    expect(alert).toBeVisible();
    expect(copyButton).toBeDisabled();
    await user.click(copyButton);
    expect(writeText).not.toHaveBeenCalled();

    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('안톨리니')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
    expect(screen.queryByText('목록 밖으로 전파된 오류')).not.toBeInTheDocument();
  });

  test('네트워크 오류를 표시하고 복사를 막는다', async () => {
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText');
    server.use(http.get(`${API_URL}${STUDY_URLS.inviteLink}`, () => HttpResponse.error()));

    renderMemberListContent(<MemberListContent.Member />);

    const { alert, copyButton } = await findInviteLinkErrorUI();

    expect(alert).toHaveTextContent('초대 링크를 가져오는데 실패했습니다.');
    expect(alert).toBeVisible();
    expect(copyButton).toBeDisabled();
    await user.click(copyButton);
    expect(writeText).not.toHaveBeenCalled();

    expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
    expect(screen.getByText('안톨리니')).toBeVisible();
    expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
    expect(screen.queryByText('목록 밖으로 전파된 오류')).not.toBeInTheDocument();
  });
});

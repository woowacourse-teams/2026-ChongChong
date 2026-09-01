import { render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import userEvent from '@testing-library/user-event';
import { Suspense } from 'react';
import { Route, Routes } from 'react-router';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../../study/urls';
import MemberListContent from '../MemberListContent';

async function findMemberRow(name: string) {
  const row = (await screen.findByText(name)).closest('[data-testid="member-row"]');
  if (!row) throw new Error(`${name} 스터디원의 행을 찾을 수 없습니다.`);
  return row as HTMLElement;
}

function renderMemberListContent(content: React.ReactNode) {
  render(
    <Routes>
      <Route path="/studies/:studyId" element={<Suspense fallback={null}>{content}</Suspense>} />
      <Route path="/studies" element={<h2>내 스터디</h2>} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

describe('초대 링크 테스트', () => {
  const STUDY_INVITE_LINK_URL = `${API_URL}${STUDY_URLS.inviteLink}`;
  const INVITE_LINK = 'https://chongchong.app/join?token=mock-token';

  function mockStudyInviteLink() {
    server.use(
      http.get(STUDY_INVITE_LINK_URL, () =>
        HttpResponse.json({
          inviteLink: INVITE_LINK,
        }),
      ),
    );
  }

  test('스터디 리드 화면에 API로 받은 초대 링크가 노출된다', async () => {
    mockStudyInviteLink();
    renderMemberListContent(<MemberListContent.Leader />);

    expect(await screen.findByText(INVITE_LINK)).toBeInTheDocument();
  });

  test('스터디원 화면에 API로 받은 초대 링크가 노출된다', async () => {
    mockStudyInviteLink();
    renderMemberListContent(<MemberListContent.Member />);

    expect(await screen.findByText(INVITE_LINK)).toBeInTheDocument();
  });
});

describe('스터디 리드 화면 테스트', () => {
  test('스터디 리드 행에는 방출하기 버튼이 존재하지 않는다', async () => {
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await findMemberRow('바니');
    expect(within(memberRow).queryByRole('button', { name: '방출하기' })).not.toBeInTheDocument();
  });

  test('스터디원을 추방하면 목록에서 추방한 스터디원이 사라진다', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await findMemberRow('안톨리니');
    expect(within(memberRow).getByText('안톨리니')).toBeInTheDocument();

    await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));
    await user.click(within(memberRow).getByRole('button', { name: '추방' }));

    await waitFor(() => expect(screen.queryByText('안톨리니')).not.toBeInTheDocument());
  });

  test('스터디를 삭제하면 스터디 리스트 페이지로 이동한다.', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
    await user.click(screen.getByRole('button', { name: '삭제' }));

    expect(await screen.findByText('내 스터디')).toBeInTheDocument();
  });
});

describe('스터디원 화면 테스트', () => {
  test('스터디원에게는 방출하기 버튼이 렌더링 되지 않는다', async () => {
    renderMemberListContent(<MemberListContent.Member />);

    expect(screen.queryByText('방출하기')).not.toBeInTheDocument();
  });

  test('스터디원에게 스터디리드는 리드 아이콘이 렌더링 된다', async () => {
    renderMemberListContent(<MemberListContent.Member />);

    const leaderRow = await findMemberRow('바니');
    expect(within(leaderRow).getByAltText('스터디 리드')).toBeInTheDocument();

    const memberRow = await findMemberRow('안톨리니');
    expect(within(memberRow).queryByAltText('스터디 리드')).not.toBeInTheDocument();
  });

  test('스터디를 탈퇴하면 스터디 리스트 페이지로 이동한다.', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Member />);

    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);

    expect(await screen.findByText('내 스터디')).toBeInTheDocument();
  });
});

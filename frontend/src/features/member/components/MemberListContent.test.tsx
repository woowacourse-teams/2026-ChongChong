import { render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import userEvent from '@testing-library/user-event';
import { Suspense } from 'react';
import { Route, Routes } from 'react-router';
import { createWrapper } from '../../../test/render';
import { InviteLinkBox } from './MemberListContent';
import { server } from '../../../mocks/msw-node';
import { BASE_URL } from '../../../../config';
import { STUDY_URLS } from '../../studies/urls';
import { MEMBER_URLS } from '../urls';
import MemberListContent from './MemberListContent';

const STUDY_INVITE_LINK_URL = `${BASE_URL}${STUDY_URLS.inviteLink}`;
const STUDY_MEMBER_LIST_URL = `${BASE_URL}${MEMBER_URLS.list}`;
const STUDY_MEMBER_KICK_URL = `${BASE_URL}${MEMBER_URLS.kick}`;

function mockStudyInviteLink() {
  server.use(
    http.get(STUDY_INVITE_LINK_URL, () =>
      HttpResponse.json({
        inviteLink: 'mock-chongchong-invite-link123',
      }),
    ),
  );
}

function mockMemberList() {
  let members = [
    {
      id: 1,
      name: '리드',
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    },
    {
      id: 2,
      name: '멤버',
      profileImage: 'http://localhost:8000',
      role: 'MEMBER',
    },
  ];

  server.use(
    http.get(STUDY_MEMBER_LIST_URL, () => HttpResponse.json({ members })),
    http.delete(STUDY_MEMBER_KICK_URL, ({ params }) => {
      members = members.filter((member) => member.id !== Number(params.memberId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
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
  test('버튼을 클릭하면 초대 링크가 복사된다', async () => {
    mockStudyInviteLink();
    const user = userEvent.setup();
    const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
    render(<InviteLinkBox inviteLink={'mock-chongchong-invite-link123'} />);
    const copyButton = await screen.findByRole('button', {
      name: '링크 복사',
    });
    await user.click(copyButton);

    // 이후에 API 값으로 전환
    expect(writeText).toHaveBeenCalledWith('mock-chongchong-invite-link123');
    writeText.mockRestore();
  });
});

describe('스터디 리드 화면 테스트', () => {
  test('스터디 리드 행에는 방출하기 버튼이 존재하지 않는다', async () => {
    mockStudyInviteLink();
    mockMemberList();
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await screen.findByTestId('member-1-row');
    expect(within(memberRow).queryByRole('button', { name: '방출하기' })).not.toBeInTheDocument();
  });

  test('스터디원을 추방하면 목록에서 추방한 스터디원이 사라진다', async () => {
    mockStudyInviteLink();
    mockMemberList();
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await screen.findByTestId('member-2-row');
    expect(within(memberRow).getByText('멤버')).toBeInTheDocument();

    await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));
    await user.click(within(memberRow).getByRole('button', { name: '추방' }));

    await waitFor(() => {
      expect(screen.queryByTestId('member-2-row')).not.toBeInTheDocument();
    });
    expect(screen.queryByText('멤버')).not.toBeInTheDocument();
    expect(screen.getByText('리드')).toBeInTheDocument();
  });
});

describe('스터디원 화면 테스트', () => {
  test('스터디원에게는 방출하기 버튼이 렌더링 되지 않는다', async () => {
    mockStudyInviteLink();
    mockMemberList();
    renderMemberListContent(<MemberListContent.Member />);

    expect(screen.queryByText('방출하기')).not.toBeInTheDocument();
  });

  test('스터디원에게 스터디리드는 리드 아이콘이 렌더링 된다', async () => {
    mockStudyInviteLink();
    mockMemberList();
    renderMemberListContent(<MemberListContent.Member />);

    const leaderRow = await screen.findByTestId('member-1-row');
    expect(within(leaderRow).getByAltText('스터디 리드')).toBeInTheDocument();

    const memberRow = screen.getByTestId('member-2-row');
    expect(within(memberRow).queryByAltText('스터디 리드')).not.toBeInTheDocument();
  });

  test('스터디를 탈퇴하면 스터디 리스트 페이지로 이동한다.', async () => {
    mockStudyInviteLink();
    mockMemberList();
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Member />);

    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);

    expect(await screen.findByText('내 스터디')).toBeInTheDocument();
  });
});

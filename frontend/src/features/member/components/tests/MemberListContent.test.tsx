import { render, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import userEvent from '@testing-library/user-event';
import { Suspense } from 'react';
import { Route, Routes } from 'react-router';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../../study/urls';
import { MEMBER_URLS } from '../../urls';
import MemberListContent from '../MemberListContent';

async function findMemberRow(name: string) {
  const row = (await screen.findByText(name)).closest('[data-testid="member-row"]');
  if (!row) throw new Error(`${name} 스터디원의 행을 찾을 수 없습니다.`);
  return row as HTMLElement;
}

function renderMemberListContent(content: React.ReactNode) {
  render(
    <Suspense fallback={null}>
      <Routes>
        <Route path="/studies/:studyId" element={content} />
        <Route path="/studies" element={<h2>내 스터디</h2>} />
      </Routes>
    </Suspense>,
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

  test('추방 권한 오류를 알리고 다이얼로그를 닫는다', async () => {
    server.use(
      http.delete(`${API_URL}${MEMBER_URLS.kick}`, () =>
        HttpResponse.json(
          {
            code: 'NOT_STUDY_LEADER',
            message: '스터디 리더만 수행할 수 있습니다.',
          },
          { status: 403 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await findMemberRow('안톨리니');
    await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));

    const dialog = within(memberRow).getByRole('alertdialog', {
      name: '안톨리니 님을 추방하시겠습니까?',
    });
    expect(dialog).toBeVisible();

    await user.click(within(dialog).getByRole('button', { name: '추방' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('스터디 리더만 수행할 수 있습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
    expect(within(memberRow).getByText('안톨리니')).toBeInTheDocument();
  });

  test('추방 네트워크 오류를 알리고 다이얼로그를 닫는다', async () => {
    server.use(http.delete(`${API_URL}${MEMBER_URLS.kick}`, () => HttpResponse.error()));
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    const memberRow = await findMemberRow('안톨리니');
    await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));

    const dialog = within(memberRow).getByRole('alertdialog', {
      name: '안톨리니 님을 추방하시겠습니까?',
    });
    expect(dialog).toBeVisible();

    await user.click(within(dialog).getByRole('button', { name: '추방' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('멤버를 추방하는데 실패했습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
    expect(within(memberRow).getByText('안톨리니')).toBeInTheDocument();
  });

  test('스터디를 삭제하면 스터디 리스트 페이지로 이동한다', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
    await user.click(screen.getByRole('button', { name: '삭제' }));

    expect(await screen.findByText('내 스터디')).toBeInTheDocument();
  });

  test('스터디 삭제 요청이 404로 실패하면 Toast를 표시하고 다이얼로그를 닫는다', async () => {
    server.use(
      http.delete(`${API_URL}${STUDY_URLS.remove}`, () =>
        HttpResponse.json(
          {
            code: 'STUDY_NOT_FOUND',
            message: '존재하지 않는 스터디입니다.',
          },
          { status: 404 },
        ),
      ),
    );
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
    const dialog = screen.getByRole('alertdialog', { name: '스터디를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('존재하지 않는 스터디입니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });

  test('스터디 삭제 중 네트워크 오류가 발생하면 기본 Toast를 표시하고 다이얼로그를 닫는다', async () => {
    server.use(http.delete(`${API_URL}${STUDY_URLS.remove}`, () => HttpResponse.error()));
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Leader />);

    await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
    const dialog = screen.getByRole('alertdialog', { name: '스터디를 삭제할까요?' });
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: '삭제' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('스터디를 삭제하는데 실패했습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });
});

describe('스터디원 화면 테스트', () => {
  test('스터디원에게는 방출하기 버튼이 렌더링 되지 않는다', async () => {
    renderMemberListContent(<MemberListContent.Member />);

    await findMemberRow('안톨리니');
    expect(screen.queryByText('방출하기')).not.toBeInTheDocument();
  });

  test('스터디원에게 스터디리드는 리드 아이콘이 렌더링 된다', async () => {
    renderMemberListContent(<MemberListContent.Member />);

    const leaderRow = await findMemberRow('바니');
    expect(within(leaderRow).getByAltText('스터디 리드')).toBeInTheDocument();

    const memberRow = await findMemberRow('안톨리니');
    expect(within(memberRow).queryByAltText('스터디 리드')).not.toBeInTheDocument();
  });

  test('스터디 탈퇴하기 버튼을 누르면 확인 다이얼로그가 렌더링 된다', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Member />);
    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);
    const dialog = screen.getByRole('alertdialog', { name: '스터디를 탈퇴하시겠습니까?' });
    expect(dialog).toBeVisible();
  });

  test('스터디를 탈퇴하면 스터디 리스트 페이지로 이동한다.', async () => {
    const user = userEvent.setup();
    renderMemberListContent(<MemberListContent.Member />);

    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);

    const button = screen.getByRole('button', { name: '탈퇴' });
    await user.click(button);

    expect(await screen.findByText('내 스터디')).toBeInTheDocument();
  });

  test('스터디 접근 권한이 없는데 탈퇴하면 다이얼로그가 닫히고 에러 메시가 렌더링 된다', async () => {
    const user = userEvent.setup();
    server.use(
      http.delete(`${API_URL}${MEMBER_URLS.leave}`, () =>
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

    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);
    const dialog = screen.getByRole('alertdialog', {
      name: '스터디를 탈퇴하시겠습니까?',
    });
    expect(dialog).toBeVisible();

    await user.click(within(dialog).getByRole('button', { name: '탈퇴' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('해당 스터디에 대한 접근 권한이 없습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });

  test('네트워크 에러가 발생하면 다이얼로그가 닫히고 에러 메시지가 렌더링 된다', async () => {
    const user = userEvent.setup();
    server.use(http.delete(`${API_URL}${MEMBER_URLS.leave}`, () => HttpResponse.error()));

    renderMemberListContent(<MemberListContent.Member />);

    const leaveButton = await screen.findByRole('button', {
      name: '스터디 탈퇴하기',
    });
    await user.click(leaveButton);

    const dialog = screen.getByRole('alertdialog', {
      name: '스터디를 탈퇴하시겠습니까?',
    });
    expect(dialog).toBeVisible();

    await user.click(within(dialog).getByRole('button', { name: '탈퇴' }));

    const toast = await screen.findByRole('status', {}, { timeout: 3000 });
    expect(toast).toHaveTextContent('스터디 탈퇴에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(dialog).not.toBeVisible();
  });
});

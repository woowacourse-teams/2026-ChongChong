import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { login, setup, logout } from '../../../../test/render';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import MemberListPage from '../MemberListPage';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../mocks/db';
import { MEMBER_URLS } from '../../urls';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const STUDY_INVITE_LINK_URL = `${API_URL}${STUDY_URLS.inviteLink}`;
const MEMBER_LIST_URL = `${API_URL}${MEMBER_URLS.list}`;

function setupMemberListPage() {
  return setup(<MemberListPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/members'],
      routes: (element) => (
        <>
          <Route path="/studies/:studyId/members" element={element} />
        </>
      ),
    }),
  });
}

describe('멤버 목록 페이지 테스트', () => {
  const leaderUserName = '디움';
  const memberUserName = '피트';
  const INVITE_LINK = 'https://chongchong.app/join?token=mock-token';
  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
    });
    await userTable.create({
      id: 2,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 1,
      name: '축구 스터디',
      description: '세종대 공격수 디움의 축구 스터디',
      inviteLink: 'siu',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    });
    await memberTable.create({
      id: 2,
      studyId: 1,
      userId: 2,
      name: memberUserName,
      profileImage: 'http://localhost:8000',
      role: 'MEMBER',
    });
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    logout();
  });

  async function findMemberRow(name: string) {
    const row = (await screen.findByText(name)).closest('[data-testid="member-row"]');
    if (!row) throw new Error(`${name} 스터디원의 행을 찾을 수 없습니다.`);
    return row as HTMLElement;
  }

  async function findInviteLinkErrorUI() {
    const alert = await screen.findByRole('alert', {}, { timeout: 3000 });
    const copyButton = screen.getByRole('button', { name: '링크 복사' });

    return { alert, copyButton };
  }

  function mockStudyInviteLink() {
    server.use(
      http.get(STUDY_INVITE_LINK_URL, () =>
        HttpResponse.json({
          inviteLink: INVITE_LINK,
        }),
      ),
    );
  }

  describe('초대링크', () => {
    beforeEach(() => {
      mockStudyInviteLink();
    });

    test.each([leaderUserName, memberUserName])('초대링크가 표시된다', async (userName) => {
      login(userName);
      setupMemberListPage();
      expect(await screen.findByText(INVITE_LINK)).toBeInTheDocument();
    });

    test.each([leaderUserName, memberUserName])(
      '초대링크 버튼을 클릭하면 초대 링크가 복사된다',
      async (userName) => {
        login(userName);
        const { user } = setupMemberListPage();
        const writeText = jest.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
        const copyButton = await screen.findByRole('button', {
          name: '링크 복사',
        });
        await user.click(copyButton);
        expect(writeText).toHaveBeenCalledWith(INVITE_LINK);
        writeText.mockRestore();
      },
    );
  });

  describe('스터디 리드', () => {
    beforeEach(() => {
      login(leaderUserName);
    });

    test.each([
      {
        title: '멤버 목록 조회 권한이 없으면',
        handler: http.get(MEMBER_LIST_URL, () =>
          HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '멤버 목록 조회 중 네트워크 오류가 발생하면',
        handler: http.get(MEMBER_LIST_URL, () => HttpResponse.error()),
        message: '멤버 목록을 불러오는데 실패했습니다.',
      },
    ])(
      '$title 목록에 에러 메시지를 표시하고 나머지 화면을 유지한다',
      async ({ handler, message }) => {
        server.use(handler);
        mockStudyInviteLink();
        setupMemberListPage();

        expect(await screen.findByText(message)).toBeVisible();
        expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
        expect(screen.getByText(INVITE_LINK)).toBeVisible();
        expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
        expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
      },
    );

    test.each([
      {
        title: '초대 링크 조회 권한이 없으면',
        handler: http.get(STUDY_INVITE_LINK_URL, () =>
          HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '초대 링크 조회 중 네트워크 오류가 발생하면',
        handler: http.get(STUDY_INVITE_LINK_URL, () => HttpResponse.error()),
        message: '초대 링크를 가져오는데 실패했습니다.',
      },
    ])('$title 에러 메시지를 표시하고 복사를 막는다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupMemberListPage();
      const writeText = jest.spyOn(navigator.clipboard, 'writeText');

      const { alert, copyButton } = await findInviteLinkErrorUI();

      expect(alert).toHaveTextContent(message);
      expect(alert).toBeVisible();
      expect(copyButton).toBeDisabled();
      await user.click(copyButton);
      expect(writeText).not.toHaveBeenCalled();

      expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
      expect(await findMemberRow(leaderUserName)).toBeVisible();
      expect(screen.getByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
    });

    test('스터디 리드 행에는 방출하기 버튼이 존재하지 않는다', async () => {
      setupMemberListPage();

      const memberRow = await findMemberRow(leaderUserName);
      expect(within(memberRow).queryByRole('button', { name: '방출하기' })).not.toBeInTheDocument();
    });

    test('스터디원을 추방하면 목록에서 추방한 스터디원이 사라진다', async () => {
      const { user } = setupMemberListPage();

      const memberRow = await findMemberRow(memberUserName);
      await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));
      await user.click(within(memberRow).getByRole('button', { name: '추방' }));

      expect(screen.queryByText('피트')).not.toBeInTheDocument();
    });

    // E2E 테스트로 전환합니다.
    // test('스터디를 삭제하면 스터디 리스트 페이지로 이동한다', async () => {
    //   const user = userEvent.setup();
    //   renderMemberListContent(<MemberListContent.Leader />);

    //   await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
    //   await user.click(screen.getByRole('button', { name: '삭제' }));

    //   expect(await screen.findByText('내 스터디')).toBeInTheDocument();
    // });

    test.each([
      {
        title: '추방 권한이 없는데 추방을 시도하면',
        handler: http.delete(`${API_URL}${MEMBER_URLS.kick}`, () =>
          HttpResponse.json(
            {
              code: 'NOT_STUDY_LEADER',
              message: '스터디 리더만 수행할 수 있습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '스터디 리더만 수행할 수 있습니다.',
      },
      {
        title: '네트워크 오류로 추방이 실패하면',
        handler: http.delete(`${API_URL}${MEMBER_URLS.kick}`, () => HttpResponse.error()),
        message: '멤버를 추방하는데 실패했습니다.',
      },
    ])('에러 메시지를 토스트로 표시하고 다이얼로그를 닫는다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupMemberListPage();

      const memberRow = await findMemberRow(memberUserName);
      await user.click(within(memberRow).getByRole('button', { name: '방출하기' }));

      const dialog = within(memberRow).getByRole('alertdialog', {
        name: '피트 님을 추방하시겠습니까?',
      });
      expect(dialog).toBeVisible();

      await user.click(within(dialog).getByRole('button', { name: '추방' }));

      const toast = await screen.findByRole('status');
      expect(toast).toHaveTextContent(message);
      expect(toast).toBeVisible();
      expect(dialog).not.toBeVisible();
      expect(within(memberRow).getByText(memberUserName)).toBeInTheDocument();
    });

    test.each([
      {
        title: '스터디 삭제 중 네트워크 오류가 발생하면',
        handler: http.delete(`${API_URL}${STUDY_URLS.remove}`, () => HttpResponse.error()),
        message: '스터디를 삭제하는데 실패했습니다.',
      },
      {
        title: '존재하지 않은 스터디 삭제를 요청하면',
        handler: http.delete(`${API_URL}${STUDY_URLS.remove}`, () =>
          HttpResponse.json(
            {
              code: 'STUDY_NOT_FOUND',
              message: '존재하지 않는 스터디입니다.',
            },
            { status: 404 },
          ),
        ),
        message: '존재하지 않는 스터디입니다.',
      },
    ])(
      '$title 에러 메시지를 토스트로 표시하고 다이얼로그를 닫는다',
      async ({ handler, message }) => {
        server.use(handler);
        const { user } = setupMemberListPage();

        await user.click(await screen.findByRole('button', { name: '스터디 삭제하기' }));
        const dialog = screen.getByRole('alertdialog', { name: '스터디를 삭제할까요?' });
        expect(dialog).toBeVisible();
        await user.click(within(dialog).getByRole('button', { name: '삭제' }));

        const toast = await screen.findByRole('status', {}, { timeout: 3000 });
        expect(toast).toHaveTextContent(message);
        expect(toast).toBeVisible();
        expect(dialog).not.toBeVisible();
      },
    );
  });

  describe('스터디 원', () => {
    beforeEach(() => {
      login(memberUserName);
    });

    test.each([
      {
        title: '멤버 목록 조회 권한이 없으면',
        handler: http.get(MEMBER_LIST_URL, () =>
          HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '멤버 목록 조회 중 네트워크 오류가 발생하면',
        handler: http.get(MEMBER_LIST_URL, () => HttpResponse.error()),
        message: '멤버 목록을 불러오는데 실패했습니다.',
      },
    ])(
      '$title 목록에 에러 메시지를 표시하고 나머지 화면을 유지한다',
      async ({ handler, message }) => {
        server.use(handler);
        mockStudyInviteLink();
        setupMemberListPage();

        expect(await screen.findByText(message, {}, { timeout: 3000 })).toBeVisible();
        expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
        expect(screen.getByText(INVITE_LINK)).toBeVisible();
        expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
        expect(screen.queryAllByTestId('member-row')).toHaveLength(0);
      },
    );

    test.each([
      {
        title: '초대 링크 조회 권한이 없으면',
        handler: http.get(STUDY_INVITE_LINK_URL, () =>
          HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '초대 링크 조회 중 네트워크 오류가 발생하면',
        handler: http.get(STUDY_INVITE_LINK_URL, () => HttpResponse.error()),
        message: '초대 링크를 가져오는데 실패했습니다.',
      },
    ])('$title 에러 메시지를 표시하고 복사를 막는다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupMemberListPage();
      const writeText = jest.spyOn(navigator.clipboard, 'writeText');

      const { alert, copyButton } = await findInviteLinkErrorUI();

      expect(alert).toHaveTextContent(message);
      expect(alert).toBeVisible();
      expect(copyButton).toBeDisabled();
      await user.click(copyButton);
      expect(writeText).not.toHaveBeenCalled();

      expect(screen.getByRole('heading', { name: '스터디 멤버' })).toBeVisible();
      expect(await findMemberRow(leaderUserName)).toBeVisible();
      expect(screen.getByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
    });

    test('스터디원에게는 방출하기 버튼이 표시 되지 않는다', async () => {
      setupMemberListPage();

      await findMemberRow('디움');
      expect(screen.queryByText('방출하기')).not.toBeInTheDocument();
    });

    test('스터디원에게 스터디리드는 리드 아이콘이 표시 된다', async () => {
      setupMemberListPage();

      const leaderRow = await findMemberRow(leaderUserName);
      expect(within(leaderRow).getByAltText('스터디 리드')).toBeInTheDocument();

      const memberRow = await findMemberRow(memberUserName);
      expect(within(memberRow).queryByAltText('스터디 리드')).not.toBeInTheDocument();
    });

    test('스터디 탈퇴하기 버튼을 누르면 확인 다이얼로그가 렌더링 된다', async () => {
      const { user } = setupMemberListPage();
      const leaveButton = await screen.findByRole('button', {
        name: '스터디 탈퇴하기',
      });
      await user.click(leaveButton);
      const dialog = screen.getByRole('alertdialog', { name: '스터디를 탈퇴하시겠습니까?' });
      expect(dialog).toBeVisible();
    });

    // E2E로 전환합니다.
    // test('스터디를 탈퇴하면 스터디 리스트 페이지로 이동한다.', async () => {
    //   const user = userEvent.setup();
    //   renderMemberListContent(<MemberListContent.Member />);

    //   const leaveButton = await screen.findByRole('button', {
    //     name: '스터디 탈퇴하기',
    //   });
    //   await user.click(leaveButton);

    //   const button = screen.getByRole('button', { name: '탈퇴' });
    //   await user.click(button);

    //   expect(await screen.findByText('내 스터디')).toBeInTheDocument();
    // });

    test.each([
      {
        title: '스터디 접근 권한이 없는데 탈퇴하면',
        handler: http.delete(`${API_URL}${MEMBER_URLS.leave}`, () =>
          HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '스터디 탈퇴중 네트워크 에러가 발생하면',
        handler: http.delete(`${API_URL}${MEMBER_URLS.leave}`, () => HttpResponse.error()),
        message: '스터디 탈퇴에 실패했습니다.',
      },
    ])(
      '$title 에러 메시지를 토스트로 표시하고 다이얼로그를 닫는다',
      async ({ handler, message }) => {
        server.use(handler);

        const { user } = setupMemberListPage();

        const leaveButton = await screen.findByRole('button', {
          name: '스터디 탈퇴하기',
        });
        await user.click(leaveButton);

        const dialog = screen.getByRole('alertdialog', {
          name: '스터디를 탈퇴하시겠습니까?',
        });
        expect(dialog).toBeVisible();

        await user.click(within(dialog).getByRole('button', { name: '탈퇴' }));

        const toast = await screen.findByRole('status');
        expect(toast).toHaveTextContent(message);
        expect(toast).toBeVisible();
        expect(dialog).not.toBeVisible();
      },
    );
  });

  describe('조회 테스트', () => {
    beforeEach(() => {
      login(leaderUserName);
    });

    test.each([
      {
        title: '접근 권한이 없으면',
        handler: http.get(STUDY_INFO_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '네트워크 에러가 발생하면',
        handler: http.get(STUDY_INFO_URL, () => HttpResponse.error()),
        message: '스터디 정보를 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupMemberListPage();

      expect(await screen.findByText(message)).toBeVisible();

      const header = screen.getByRole('banner');
      expect(within(header).getByRole('heading', { name: '멤버' })).toBeVisible();
      expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    });
  });
});

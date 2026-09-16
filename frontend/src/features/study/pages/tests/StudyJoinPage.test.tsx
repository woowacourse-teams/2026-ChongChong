import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { createWrapper, setup, login } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { API_URL } from '../../../../../config';
import StudyJoinPage from '../StudyJoinPage';
import { STUDY_URLS } from '../../urls';
import { studyTable } from '../../mocks/db';
import { invalidInputResponse } from '../../../../mocks/errors';
import { userTable } from '../../../user/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { clearAccessToken as logout } from '../../../login/accessToken';

const STUDY_JOIN_URL = `${API_URL}${STUDY_URLS.join}`;

describe('스터디 참가 폼 테스트', () => {
  afterEach(() => {
    logout();
  });

  function createInviteLink(token: string) {
    return new URL(`${STUDY_URLS.join}?token=${token}`, 'http://localhost').href;
  }

  function studyJoinButton() {
    return screen.getByRole('button', { name: '스터디 참여하기' });
  }

  function inviteLinkInput() {
    return screen.getByRole('textbox', { name: '초대 링크' });
  }

  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    setup(<StudyJoinPage />, { wrapper: createWrapper() });

    expect(studyJoinButton()).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const { user } = setup(<StudyJoinPage />, { wrapper: createWrapper() });

    await user.type(inviteLinkInput(), '씨없는팀으로 초대합니다');
    expect(studyJoinButton()).toBeEnabled();
  });

  test('참여 요청이 진행 중이면 버튼은 비활성화 된다', async () => {
    let finishJoin: () => void;
    const blocker = new Promise<void>((resolve) => {
      finishJoin = resolve;
    });

    server.use(
      http.post(STUDY_JOIN_URL, async () => {
        await blocker;
        return HttpResponse.json({ studyId: 10000 });
      }),
    );

    const { user } = setup(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = inviteLinkInput();
    await user.type(linkInput, createInviteLink('join-please'));
    const button = studyJoinButton();
    await user.click(button);

    await waitFor(() => expect(button).toBeDisabled());

    finishJoin!();
    await waitFor(() => expect(button).toBeEnabled());
  });

  // E2E 테스트로 전환합니다.
  // test.skip('스터디 참여에 성공했을때 스터디 디테일 페이지로 이동한다', async () => {
  //   const { inviteLink } = mockStudies[1];
  //   const user = userEvent.setup();
  //   render(
  //     <Suspense fallback={null}>
  //       <Routes>
  //         <Route path={STUDY_URLS.join} element={<StudyJoinPage />}></Route>
  //         <Route path="/studies/:studyId" element={<StudyDetailPage />}></Route>
  //       </Routes>
  //     </Suspense>,
  //     { wrapper: createWrapper({ initialEntries: [STUDY_URLS.join] }) },
  //   );

  //   const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
  //   await user.type(linkInput, createInviteLink(inviteLink));
  //   await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));

  //   expect(await screen.findByText('농구 스터디')).toBeInTheDocument();
  //   expect(await screen.findByText('안톨리니 · 스터디원')).toBeInTheDocument();
  // });

  test.each([
    '/studies/join',
    '/studies/join?lunch=chicken',
    '/studies/join#token=some-token-exist',
  ])('쿼리 파라미터로 토큰 값이 존재하지 않으면 스터디 참여 입력은 빈값이다', (path) => {
    setup(<StudyJoinPage />, { wrapper: createWrapper({ initialEntries: [path] }) });

    expect(inviteLinkInput()).toHaveValue('');
  });

  test.each([
    [
      '/studies/join?token=some-token-exist',
      'http://localhost/studies/join?token=some-token-exist',
    ],
    [
      '/studies/join?token=some-token-exist&lunch=chicken',
      'http://localhost/studies/join?token=some-token-exist&lunch=chicken',
    ],
    [
      '/studies/join?token=hello-world#chongchong',
      'http://localhost/studies/join?token=hello-world#chongchong',
    ],
  ])(
    '쿼리 파라미터에 토큰 값이 존재하면 스터디 참여 입력에 전체 초대 링크가 채워진다',
    (path, expectedLink) => {
      setup(<StudyJoinPage />, { wrapper: createWrapper({ initialEntries: [path] }) });

      expect(inviteLinkInput()).toHaveValue(expectedLink);
    },
  );

  test('이미 참여한 스터디를 참여하려고 하면 Toast 에러 메시지가 렌더링 된다', async () => {
    const benji = await userTable.create({
      id: 1,
      name: '벤지',
      profileImage: 'http://localhost:8000',
    });
    const study = await studyTable.create({
      id: 1,
      name: '탄자니아 스터디',
      description: '이미 참여한 스터디입니다',
      inviteLink: 'tanzania',
    });
    await memberTable.create({
      id: 1,
      studyId: study.id,
      userId: benji.id,
      name: benji.name,
      profileImage: benji.profileImage,
      role: 'LEADER',
    });

    login('벤지');
    const { user } = setup(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = inviteLinkInput();
    const inviteLink = createInviteLink(study.inviteLink);
    await user.type(linkInput, inviteLink);
    await user.click(studyJoinButton());

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('해당 스터디에 이미 가입되어 있습니다.');
    expect(toast).toBeVisible();
    expect(linkInput).toHaveValue(inviteLink);
  });

  test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
    server.use(
      http.post(STUDY_JOIN_URL, () =>
        invalidInputResponse([
          { field: 'token', code: 'SOME_ERROR', reason: '토큰값이 문제가 있어요' },
        ]),
      ),
    );
    const { user } = setup(<StudyJoinPage />, { wrapper: createWrapper() });

    await user.type(inviteLinkInput(), createInviteLink('세상에 존재하지 않는 스터디'));
    await user.click(studyJoinButton());
    expect(await screen.findByText('토큰값이 문제가 있어요')).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('네트워크 오류가 발생하면 Toast 에러 메시지가 렌더링 된다', async () => {
    server.use(http.post(STUDY_JOIN_URL, () => HttpResponse.error()));
    const { user } = setup(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = inviteLinkInput();
    await user.type(linkInput, 'https://www.naver.com?token=안녕하세요 저 안톨리니입니다.');
    await user.click(studyJoinButton());

    const toast = await screen.findByRole('status');
    expect(toast).toHaveTextContent('스터디 참여에 실패했습니다.');
    expect(toast).toBeVisible();
    expect(linkInput).toHaveValue('https://www.naver.com?token=안녕하세요 저 안톨리니입니다.');
  });
});

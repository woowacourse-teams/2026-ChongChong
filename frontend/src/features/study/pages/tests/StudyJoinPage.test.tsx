import { Suspense } from 'react';
import { Routes, Route } from 'react-router';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { createWrapper } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { API_URL } from '../../../../../config';
import StudyJoinPage from '../StudyJoinPage';
import { STUDY_URLS } from '../../urls';
import StudyDetailPage from '../StudyDetailPage';
import { setAccessToken, clearAccessToken } from '../../../login/accessToken';
import { mockStudies } from '../../mocks/db';
import { invalidInputResponse } from '../../../../mocks/errors';

const STUDY_JOIN_URL = `${API_URL}${STUDY_URLS.join}`;

describe('스터디 참가 폼 테스트', () => {
  afterEach(() => clearAccessToken());

  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 참여하기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(nameInput, '씨없는팀으로 초대합니다');
    const button = screen.getByRole('button', { name: '스터디 참여하기' });
    expect(button).toBeEnabled();
  });

  test('참여 요청이 진행 중이면 버튼은 비활성화 된다', async () => {
    setAccessToken('2');
    const { inviteLink, id } = mockStudies[1];

    let finishJoin: () => void;
    const blocker = new Promise<void>((resolve) => {
      finishJoin = resolve;
    });

    server.use(
      http.post(STUDY_JOIN_URL, async () => {
        await blocker;
        return HttpResponse.json({ studyId: id });
      }),
    );

    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(linkInput, inviteLink);
    const button = screen.getByRole('button', { name: '스터디 참여하기' });
    await user.click(button);

    await waitFor(() => expect(button).toBeDisabled());

    finishJoin!();
    await waitFor(() => expect(button).toBeEnabled());
  });

  test('스터디 참여에 성공했을때 스터디 디테일 페이지로 이동한다', async () => {
    setAccessToken('2');
    const { inviteLink } = mockStudies[1];
    const user = userEvent.setup();
    render(
      <Suspense fallback={null}>
        <Routes>
          <Route path={STUDY_URLS.join} element={<StudyJoinPage />}></Route>
          <Route path="/studies/:studyId" element={<StudyDetailPage />}></Route>
        </Routes>
      </Suspense>,
      { wrapper: createWrapper({ initialEntries: [STUDY_URLS.join] }) },
    );

    const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(linkInput, inviteLink);
    await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));

    expect(await screen.findByText('농구 스터디')).toBeInTheDocument();
    expect(await screen.findByText('안톨리니 · 스터디원')).toBeInTheDocument();
  });

  // 에러처리 + Toast UI가 추가된 뒤에 skip을 해제합니다.
  test.skip('이미 참여한 스터디를 참여하려고 하면 에러 메시지를 보여준다', async () => {
    setAccessToken('2');
    const { inviteLink } = mockStudies[0];
    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(linkInput, inviteLink);
    await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));

    expect(await screen.findByRole('status')).toHaveTextContent('이미 참여한 스터디예요.');
  });

  test.skip('유효하지 않은 초대 링크로 참여하면 에러 메시지를 보여준다', async () => {
    setAccessToken('2');
    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(linkInput, 'chongchong.app/join/없는링크');
    await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));

    expect(await screen.findByRole('status')).toHaveTextContent('스터디 참여에 실패했습니다.');
  });

  test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(STUDY_JOIN_URL, () =>
        invalidInputResponse([
          { field: 'token', code: 'SOME_ERROR', reason: '토큰값이 문제가 있어요' },
        ]),
      ),
    );

    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const inviteLinkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(inviteLinkInput, '우아한테크코스9기');
    await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));
    expect(await screen.findByText('토큰값이 문제가 있어요')).toBeInTheDocument();
  });
});

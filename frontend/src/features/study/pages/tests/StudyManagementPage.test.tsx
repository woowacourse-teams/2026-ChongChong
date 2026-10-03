import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { login, setup, logout, createWrapper } from '../../../../test/render';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { STUDY_URLS } from '../../urls';
import StudyManagementPage from '../StudyManagementPage';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { MEMBER_URLS } from '../../../member/urls';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;

function setupStudyManagementPage() {
  return setup(<StudyManagementPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/management'],
      routes: (element) => <Route path="/studies/:studyId/management" element={element} />,
    }),
  });
}

describe('스터디 관리 페이지 테스트', () => {
  const leaderUserName = '디움';
  const memberUserName = '찰리';
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

  describe('컨텐츠 렌더링 테스트', () => {
    test('스터디 정보를 가져오지 못하면 에러 텍스트를 표시한다', async () => {
      server.use(
        http.get(STUDY_INFO_URL, () =>
          HttpResponse.json(
            {
              code: 'SOME_CODE',
              message: '스터디 기본 정보 응답 형식이 올바르지 않습니다.',
            },
            { status: 400 },
          ),
        ),
      );
      setupStudyManagementPage();

      expect(
        await screen.findByText('스터디 기본 정보 응답 형식이 올바르지 않습니다.'),
      ).toBeInTheDocument();
    });
  });

  describe('스터디 리드', () => {
    beforeEach(() => {
      login(leaderUserName);
    });

    test('스터디 리드에게는 삭제 버튼만 표시된다', async () => {
      setupStudyManagementPage();

      expect(await screen.findByRole('button', { name: '스터디 삭제하기' })).toBeVisible();
      expect(screen.queryByRole('button', { name: '스터디 탈퇴하기' })).not.toBeInTheDocument();
    });

    // TODO: 스터디 삭제 후 목록 페이지 이동은 E2E 테스트로 검증합니다.

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
        const { user } = setupStudyManagementPage();

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

    test('스터디원에게는 탈퇴 버튼만 표시된다', async () => {
      setupStudyManagementPage();

      expect(await screen.findByRole('button', { name: '스터디 탈퇴하기' })).toBeVisible();
      expect(screen.queryByRole('button', { name: '스터디 삭제하기' })).not.toBeInTheDocument();
    });

    // TODO: 스터디 탈퇴 후 목록 페이지 이동은 E2E 테스트로 검증합니다.

    test('스터디 탈퇴하기 버튼을 누르면 확인 다이얼로그가 렌더링 된다', async () => {
      const { user } = setupStudyManagementPage();
      const leaveButton = await screen.findByRole('button', {
        name: '스터디 탈퇴하기',
      });
      await user.click(leaveButton);
      const dialog = screen.getByRole('alertdialog', { name: '스터디를 탈퇴하시겠습니까?' });
      expect(dialog).toBeVisible();
    });

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

        const { user } = setupStudyManagementPage();

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
});

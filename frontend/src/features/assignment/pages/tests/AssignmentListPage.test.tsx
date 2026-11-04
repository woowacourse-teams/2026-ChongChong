import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, setup, login, logout } from '../../../../test/render';
import { STUDY_URLS } from '../../../study/urls';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import AssignmentListPage from '../AssignmentListPage';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const ASSIGNMENT_LIST_URL = `${API_URL}/studies/:studyId/assignments`;

function setupAssignmentListPage() {
  return setup(<AssignmentListPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments'],
      routes: (element) => <Route path="/studies/:studyId/assignments" element={element} />,
    }),
  });
}

function assignmentListContent() {
  return within(screen.getByRole('main'));
}

describe('과제 목록 페이지 테스트', () => {
  const leaderUserName = '안톨리니';
  const memberUserName = '디움';

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
      name: '객체지향 스터디',
      description: '객체의 역할과 책임을 공부합니다.',
      inviteLink: 'object-oriented',
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

  test.each([leaderUserName, memberUserName])(
    '%s에게 역할에 맞는 과제 메뉴를 제공하고 닫으면 Floating으로 돌아온다',
    async (userName) => {
      login(userName);
      const { user } = setupAssignmentListPage();
      await screen.findByText('아직 과제가 없어요');
      expect(screen.queryByRole('link', { name: /과제 추가하기/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      if (userName === leaderUserName) {
        const trigger = screen.getByRole('button', { name: '과제 메뉴 열기' });
        await user.click(trigger);
        const menu = within(screen.getByRole('dialog', { name: '과제 메뉴' }));
        expect(menu.getAllByRole('link')).toHaveLength(1);
        expect(menu.getByRole('link', { name: /과제 추가하기/ })).toHaveAttribute(
          'href',
          '/studies/1/assignments/create',
        );
        const closeButton = menu.getByRole('button', { name: '닫기' });
        await waitFor(() => expect(closeButton).toBeVisible());
        await user.click(closeButton);
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();
      } else {
        expect(screen.queryByRole('button', { name: '과제 메뉴 열기' })).not.toBeInTheDocument();
      }
    },
  );

  test.each([
    { userName: leaderUserName, visibility: 'LEADER_ONLY', label: '미공개' },
    { userName: leaderUserName, visibility: 'ALL_STUDY_MEMBERS', label: '공개' },
    { userName: memberUserName, visibility: 'LEADER_ONLY', label: '미공개' },
    { userName: memberUserName, visibility: 'ALL_STUDY_MEMBERS', label: '공개' },
  ])(
    '$userName 목록에서 기존 뱃지 오른쪽에 $label 뱃지를 표시한다',
    async ({ userName, visibility, label }) => {
      login(userName);
      const isLeader = userName === leaderUserName;
      server.use(
        http.get(ASSIGNMENT_LIST_URL, () =>
          HttpResponse.json({
            nextCursor: null,
            hasNext: false,
            assignments: [
              {
                id: 1,
                title: '공개 여부 확인 과제',
                content: '과제 내용',
                submissionMethod: '링크 제출',
                closeAt: '2026-10-05T23:59:59',
                submissionStatus: 'SUBMITTED',
                submissionVisibility: visibility,
                ...(isLeader ? { memberCount: 2, completeCount: 2, isComplete: true } : {}),
              },
            ],
          }),
        ),
      );
      const detailRequest = jest.fn();
      server.use(
        http.get(`${ASSIGNMENT_LIST_URL}/:assignmentId`, () => {
          detailRequest();
          return new HttpResponse(null, { status: 500 });
        }),
      );
      setupAssignmentListPage();

      const assignment = await screen.findByRole('link', { name: /공개 여부 확인 과제/ });
      await within(assignment).findByText(label);
      const statusBadge = within(assignment).getByText(isLeader ? '모두 제출' : '제출 완료');
      expect(statusBadge.nextElementSibling).toBe(within(assignment).getByText(label));
      expect(detailRequest).not.toHaveBeenCalled();
    },
  );

  describe('기본 정보 조회 실패', () => {
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
        title: '네트워크 오류가 발생하면',
        handler: http.get(STUDY_INFO_URL, () => HttpResponse.error()),
        message: '스터디 정보를 불러오는데 실패했습니다.',
      },
    ])('$title 오류 안내와 뒤로가기 헤더 및 하단 탭을 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupAssignmentListPage();

      expect(await screen.findByText(message)).toBeVisible();
      const header = within(screen.getByRole('banner'));
      expect(header.getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(header.queryByRole('heading')).not.toBeInTheDocument();
      expect(screen.getByRole('navigation')).toBeVisible();
    });
  });

  describe('스터디 리드', () => {
    beforeEach(() => {
      login(leaderUserName);
    });

    test.each([
      {
        title: '과제 목록 조회 권한이 없으면',
        handler: http.get(ASSIGNMENT_LIST_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '과제 목록 조회 중 네트워크 오류가 발생하면',
        handler: http.get(ASSIGNMENT_LIST_URL, () => HttpResponse.error()),
        message: '과제 목록을 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupAssignmentListPage();

      expect(await screen.findByText(message)).toBeVisible();
      expect(assignmentListContent().getByText(message)).toBeVisible();
    });
  });

  describe('스터디원', () => {
    beforeEach(() => {
      login(memberUserName);
    });

    test.each([
      {
        title: '과제 목록 조회 권한이 없으면',
        handler: http.get(ASSIGNMENT_LIST_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '과제 목록 조회 중 네트워크 오류가 발생하면',
        handler: http.get(ASSIGNMENT_LIST_URL, () => HttpResponse.error()),
        message: '과제 목록을 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 에러 메시지를 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupAssignmentListPage();

      expect(await screen.findByText(message)).toBeVisible();
      expect(assignmentListContent().getByText(message)).toBeVisible();
    });

    test.each([
      { submissionStatus: 'LATE_SUBMITTED', badgeLabel: '지각 제출' },
      { submissionStatus: 'MISSING', badgeLabel: '마감 후 미제출' },
    ] as const)(
      '$submissionStatus 상태의 과제 목록을 표시한다',
      async ({ submissionStatus, badgeLabel }) => {
        server.use(
          http.get(ASSIGNMENT_LIST_URL, () =>
            HttpResponse.json({
              nextCursor: null,
              hasNext: false,
              assignments: [
                {
                  id: 1,
                  title: '마감된 과제',
                  content: '과제 내용',
                  submissionMethod: '링크 제출',
                  closeAt: '2026-10-05T23:59:59',
                  submissionStatus,
                  submissionVisibility: 'LEADER_ONLY',
                },
              ],
            }),
          ),
        );
        setupAssignmentListPage();

        expect(await screen.findByText('마감된 과제')).toBeVisible();
        expect(assignmentListContent().getByText(badgeLabel)).toBeVisible();
      },
    );
  });
});

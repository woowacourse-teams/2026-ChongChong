import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { createWrapper, setup, login } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import StudyDetailPage from '../StudyDetailPage';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { assignmentTable } from '../../../assignment/mocks/db';
import { clearAccessToken as logout } from '../../../login/accessToken';
import type { Role } from '../../types';

const STUDY_DETAIL_URL = `${API_URL}${STUDY_URLS.detail}`;

function setupStudyDetailPage(studyId = 1) {
  return setup(<StudyDetailPage />, {
    wrapper: createWrapper({
      initialEntries: [`/studies/${studyId}`],
      routes: (element) => (
        <>
          <Route path={STUDY_URLS.detail} element={element} />
          <Route path="/studies" element={<h1>내 스터디</h1>} />
        </>
      ),
    }),
  });
}

function unreadNoticesHeading() {
  return screen.queryByRole('heading', { name: '읽지 않은 공지' });
}

function unsubmittedAssignmentsHeading() {
  return screen.queryByRole('heading', { name: '제출하지 않은 과제' });
}

describe('스터디 상세 페이지', () => {
  const studyId = 1;
  const userId = 1;

  function createStudyMember(role: Role) {
    return memberTable.create({
      id: 1,
      studyId,
      userId,
      name: '안톨리니',
      profileImage: 'http://localhost:8000',
      role,
    });
  }

  beforeEach(async () => {
    await userTable.create({
      id: userId,
      name: '안톨리니',
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: studyId,
      name: '객체지향 스터디',
      description: '객체지향을 공부하는 스터디입니다',
      inviteLink: 'object-oriented',
    });
    login('안톨리니');
  });

  afterEach(() => {
    logout();
  });

  describe('리드의 진행 현황 조회', () => {
    beforeEach(async () => {
      await createStudyMember('LEADER');
    });

    test('스터디 현황에는 몇명이 과제/공지를 완료했는지 진행 상황이 렌더링된다', async () => {
      server.use(
        http.get(STUDY_DETAIL_URL, () =>
          HttpResponse.json({
            notices: {
              count: 1,
              items: [
                {
                  id: 1,
                  title: '광주 스터디룸에서 만나도록 합시다',
                  memberCount: 4,
                  completeCount: 2,
                },
              ],
            },
            assignments: {
              count: 1,
              items: [
                {
                  id: 1,
                  title: '객체지향 1장 읽기',
                  memberCount: 7,
                  completeCount: 3,
                },
              ],
            },
          }),
        ),
      );
      setupStudyDetailPage(studyId);

      expect(await screen.findByText('2/4 읽음')).toBeInTheDocument();
      expect(screen.getByText('3/7 제출')).toBeInTheDocument();
      expect(
        screen.queryByRole('heading', { name: '아직 진행 중인 공지나 과제가 없어요!' }),
      ).not.toBeInTheDocument();
    });

    test('진행 중인 공지와 과제가 모두 없으면 비어있는 상태를 렌더링한다', async () => {
      setupStudyDetailPage(studyId);

      expect(
        await screen.findByRole('heading', {
          name: '아직 진행 중인 공지나 과제가 없어요!',
        }),
      ).toBeInTheDocument();
      expect(screen.getByText('새로운 공지나 과제를 작성하러 가볼까요?')).toBeInTheDocument();
    });
  });

  describe('스터디원 할 일 조회', () => {
    beforeEach(async () => {
      await createStudyMember('MEMBER');
    });

    test('읽지 않은 공지와 제출하지 않은 과제가 모두 없으면 할 일 완료 화면을 렌더링한다', async () => {
      setupStudyDetailPage(studyId);

      expect(
        await screen.findByRole('heading', { name: '오늘 할 일을 모두 마쳤어요!' }),
      ).toBeInTheDocument();
      expect(unreadNoticesHeading()).not.toBeInTheDocument();
      expect(unsubmittedAssignmentsHeading()).not.toBeInTheDocument();
    });

    test('읽지 않은 공지만 있으면 공지 섹션만 렌더링한다', async () => {
      server.use(
        http.get(STUDY_DETAIL_URL, () =>
          HttpResponse.json({
            totalCount: 1,
            notices: { items: [{ id: 1, title: '내일 점심 장소 공지' }] },
            assignments: { items: [] },
          }),
        ),
      );
      setupStudyDetailPage(studyId);

      expect(await screen.findByText('내일 점심 장소 공지')).toBeInTheDocument();
      expect(unreadNoticesHeading()).toBeInTheDocument();
      expect(unsubmittedAssignmentsHeading()).not.toBeInTheDocument();
      expect(screen.getAllByText('내일 점심 장소 공지')).toHaveLength(1);
    });

    test('제출하지 않은 과제만 있으면 과제 섹션만 렌더링한다', async () => {
      await assignmentTable.create({
        id: 1,
        studyId,
        title: '코덱스 펫 만들기',
        content: '나만의 코덱스 펫을 만들어주세요.',
        submissionMethod: '링크로 제출하세요',
        closeAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        completeUserIds: [],
      });
      setupStudyDetailPage(studyId);

      expect(await screen.findByText('코덱스 펫 만들기')).toBeInTheDocument();
      expect(unsubmittedAssignmentsHeading()).toBeInTheDocument();
      expect(unreadNoticesHeading()).not.toBeInTheDocument();
    });
  });

  describe('상세 조회 실패', () => {
    beforeEach(() => {
      jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    describe('리드', () => {
      beforeEach(async () => {
        await createStudyMember('LEADER');
      });

      test.each([
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(STUDY_DETAIL_URL, () => HttpResponse.error()),
          message: '스터디 정보를 불러오는데 실패했습니다.',
        },
        {
          title: '접근이 거부되면',
          handler: http.get(STUDY_DETAIL_URL, () =>
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
      ])(
        '$title 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다',
        async ({ handler, message }) => {
          server.use(handler);
          setupStudyDetailPage(studyId);

          expect(await screen.findByText(message)).toBeVisible();

          const header = within(screen.getByRole('banner'));
          expect(header.getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
          expect(header.getByText('안톨리니 · 리드')).toBeVisible();
          expect(header.getByRole('button', { name: '뒤로 가기' })).toBeVisible();
          expect(screen.getByRole('navigation')).toBeVisible();
        },
      );
    });

    describe('스터디원', () => {
      beforeEach(async () => {
        await createStudyMember('MEMBER');
      });

      test.each([
        {
          title: '네트워크 오류가 발생하면',
          handler: http.get(STUDY_DETAIL_URL, () => HttpResponse.error()),
          message: '스터디 정보를 불러오는데 실패했습니다.',
        },
        {
          title: '접근이 거부되면',
          handler: http.get(STUDY_DETAIL_URL, () =>
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
      ])(
        '$title 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다',
        async ({ handler, message }) => {
          server.use(handler);
          setupStudyDetailPage(studyId);

          expect(await screen.findByText(message)).toBeVisible();

          const header = within(screen.getByRole('banner'));
          expect(header.getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
          expect(header.getByText('안톨리니 · 스터디원')).toBeVisible();
          expect(header.getByRole('button', { name: '뒤로 가기' })).toBeVisible();
          expect(screen.getByRole('navigation')).toBeVisible();
        },
      );
    });
  });
});

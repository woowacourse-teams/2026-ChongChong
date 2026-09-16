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
import { clearAccessToken as logout } from '../../../login/accessToken';

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

afterEach(() => {
  logout();
});

describe('상세 조회 실패', () => {
  const studyId = 1;
  const userId = 1;

  beforeEach(async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
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

  describe('리드', () => {
    beforeEach(async () => {
      await memberTable.create({
        id: 1,
        studyId,
        userId,
        name: '안톨리니',
        profileImage: 'http://localhost:8000',
        role: 'LEADER',
      });
    });

    test.each([
      {
        title: '네트워크 오류가 발생하면',
        handler: http.get(STUDY_DETAIL_URL, () => HttpResponse.error()),
        message: '스터디 정보를 불러오는데 실패했습니다.',
      },
      {
        title: '접근이 거부되면',
        handler: http.get(STUDY_DETAIL_URL, () => {
          return HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          );
        }),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
    ])('$title 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      setupStudyDetailPage(studyId);

      expect(await screen.findByText(message)).toBeVisible();

      const header = screen.getByRole('banner');
      expect(within(header).getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
      expect(within(header).getByText('안톨리니 · 리드')).toBeVisible();
      expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    });
  });

  describe('스터디원', () => {
    beforeEach(async () => {
      await memberTable.create({
        id: 1,
        studyId,
        userId,
        name: '안톨리니',
        profileImage: 'http://localhost:8000',
        role: 'MEMBER',
      });
    });

    test.each([
      {
        title: '네트워크 오류가 발생하면',
        handler: http.get(STUDY_DETAIL_URL, () => HttpResponse.error()),
        message: '스터디 정보를 불러오는데 실패했습니다.',
      },
      {
        title: '접근이 거부되면',
        handler: http.get(STUDY_DETAIL_URL, () => {
          return HttpResponse.json(
            {
              code: 'STUDY_ACCESS_DENIED',
              message: '해당 스터디에 대한 접근 권한이 없습니다.',
            },
            { status: 403 },
          );
        }),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
    ])('$title 본문에 오류를 표시하고 헤더와 하단 탭을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      setupStudyDetailPage(studyId);

      expect(await screen.findByText(message)).toBeVisible();

      const header = screen.getByRole('banner');
      expect(within(header).getByRole('heading', { name: '객체지향 스터디' })).toBeVisible();
      expect(within(header).getByText('안톨리니 · 스터디원')).toBeVisible();
      expect(within(header).getByRole('button', { name: '뒤로 가기' })).toBeVisible();
      expect(screen.getByRole('navigation')).toBeVisible();
    });
  });
});

import { server } from '../../../../mocks/msw-node';
import { Suspense } from 'react';
import { Routes, Route } from 'react-router';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { LeaderStudyDetailContent } from '../StudyDetailContent';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../urls';
import { API_URL } from '../../../../../config';

function renderStudyLeaderDetailContent() {
  render(
    <Routes>
      <Route
        path="/studies/:studyId"
        element={
          <Suspense fallback={null}>
            <LeaderStudyDetailContent username={'some-user'} />
          </Suspense>
        }
      ></Route>
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

const STUDY_DETAIL_URL = `${API_URL}${STUDY_URLS.detail}`;

describe('스터디 리드 상세 콘텐츠', () => {
  test('스터디 현황에는 몇명이 과제/공지를 완료했는지 진행 상황이 렌더링된다', async () => {
    server.use(
      http.get(STUDY_DETAIL_URL, () =>
        HttpResponse.json({
          notices: {
            count: 1,
            items: [
              {
                id: 1,
                title: '판교 스터디룸에서 만나도록 합시다',
                memberCount: 4,
                completeCount: 2,
              },
            ],
          },
          assignments: {
            count: 1,
            items: [{ id: 1, title: '그리디 3문제 풀기', memberCount: 7, completeCount: 3 }],
          },
        }),
      ),
    );
    renderStudyLeaderDetailContent();

    expect(await screen.findByText('2/4 읽음')).toBeInTheDocument();
    expect(screen.getByText('3/7 제출')).toBeInTheDocument();
  });
});

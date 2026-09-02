import { server } from '../../../../mocks/msw-node';
import { Suspense } from 'react';
import { Routes, Route } from 'react-router';
import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { LeaderStudyDetailContent, MemberStudyDetailContent } from '../StudyDetailContent';
import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../urls';
import { API_URL } from '../../../../../config';

const STUDY_DETAIL_URL = `${API_URL}${STUDY_URLS.detail}`;

function renderStudyDetailContent(node: React.ReactNode) {
  render(
    <Routes>
      <Route path="/studies/:studyId" element={<Suspense fallback={null}>{node}</Suspense>}></Route>
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

function mockMemberStudyDetail({
  notices = [],
  assignments = [],
}: {
  notices?: { id: number; title: string }[];
  assignments?: { id: number; title: string }[];
}) {
  server.use(
    http.get(STUDY_DETAIL_URL, () =>
      HttpResponse.json({
        totalCount: notices.length + assignments.length,
        notices,
        assignments,
      }),
    ),
  );
}

function mockLeaderStudyDetail({
  notices = [],
  assignments = [],
}: {
  notices?: { id: number; title: string; memberCount: number; completeCount: number }[];
  assignments?: { id: number; title: string; memberCount: number; completeCount: number }[];
}) {
  server.use(
    http.get(STUDY_DETAIL_URL, () =>
      HttpResponse.json({
        notices: { count: notices.length, items: notices },
        assignments: { count: assignments.length, items: assignments },
      }),
    ),
  );
}

describe('스터디 리드 상세 콘텐츠', () => {
  test('스터디 현황에는 몇명이 과제/공지를 완료했는지 진행 상황이 렌더링된다', async () => {
    mockLeaderStudyDetail({
      notices: [
        {
          id: 1,
          title: '광주 스터디룸에서 만나도록 합시다',
          memberCount: 4,
          completeCount: 2,
        },
      ],
      assignments: [{ id: 1, title: '객체지향 1장 읽기', memberCount: 7, completeCount: 3 }],
    });
    renderStudyDetailContent(<LeaderStudyDetailContent username={'some-user'} />);

    expect(await screen.findByText('2/4 읽음')).toBeInTheDocument();
    expect(screen.getByText('3/7 제출')).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: '아직 진행 중인 공지나 과제가 없어요!' }),
    ).not.toBeInTheDocument();
  });

  test('진행 중인 공지와 과제가 모두 없으면 비어있는 상태를 렌더링한다', async () => {
    mockLeaderStudyDetail({});

    renderStudyDetailContent(<LeaderStudyDetailContent username={'some-user'} />);

    expect(
      await screen.findByRole('heading', {
        name: '아직 진행 중인 공지나 과제가 없어요!',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('새로운 공지나 과제를 작성하러 가볼까요?')).toBeInTheDocument();
  });
});

describe('스터디원 상세 콘텐츠', () => {
  test('읽지 않은 공지와 제출하지 않은 과제가 모두 없으면 할 일 완료 화면을 렌더링한다', async () => {
    mockMemberStudyDetail({});

    renderStudyDetailContent(<MemberStudyDetailContent username={'some-user'} />);

    expect(
      await screen.findByRole('heading', { name: '오늘 할 일을 모두 마쳤어요!' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '읽지 않은 공지' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '제출하지 않은 과제' })).not.toBeInTheDocument();
  });

  test('읽지 않은 공지만 있으면 공지 섹션만 렌더링한다', async () => {
    mockMemberStudyDetail({
      notices: [{ id: 1, title: '내일 점심 장소 공지' }],
    });

    renderStudyDetailContent(<MemberStudyDetailContent username={'some-user'} />);

    expect(await screen.findByRole('heading', { name: '읽지 않은 공지' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '제출하지 않은 과제' })).not.toBeInTheDocument();
  });

  test('제출하지 않은 과제만 있으면 과제 섹션만 렌더링한다', async () => {
    mockMemberStudyDetail({
      assignments: [{ id: 1, title: '코덱스 펫 만들기' }],
    });

    renderStudyDetailContent(<MemberStudyDetailContent username={'some-user'} />);

    expect(await screen.findByRole('heading', { name: '제출하지 않은 과제' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '읽지 않은 공지' })).not.toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { createWrapper } from '../../../test/render';
import { server } from '../../../mocks/msw-node';
import StudyDetailPage from './StudyDetailPage';
import { BASE_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';

const STUDY_INFO_URL = `${BASE_URL}${STUDY_URLS.info}`;

function mockStudyInfo(role: 'LEADER' | 'MEMBER') {
  server.use(
    http.get(STUDY_INFO_URL, () =>
      HttpResponse.json({ studyName: '객체지향 스터디', role, userName: '안톨리니' }),
    ),
  );
}

function renderStudyDetailPage() {
  render(
    <Routes>
      <Route path="/studies/:studyId" element={<StudyDetailPage />} />
    </Routes>,
    { wrapper: createWrapper({ initialEntries: ['/studies/1'] }) },
  );
}

describe('스터디 리드', () => {
  beforeEach(() => {
    mockStudyInfo('LEADER');
  });

  // 현재 테스트는 외부 Header 블럭을 분리하여 테스트 해야합니다.
  test('스터디 리드일 경우 헤더에 리드로 렌더링 한다', async () => {
    renderStudyDetailPage();

    expect(await screen.findByText('안톨리니 · 리드')).toBeInTheDocument();
  });
});

describe('스터디원', () => {
  beforeEach(() => {
    mockStudyInfo('MEMBER');
  });

  test('스터디원일 경우 헤더에 스터디원으로 렌더링 한다', async () => {
    renderStudyDetailPage();

    expect(await screen.findByText('안톨리니 · 스터디원')).toBeInTheDocument();
  });
});

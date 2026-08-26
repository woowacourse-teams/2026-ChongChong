import { render, screen } from '@testing-library/react';
import { createWrapper, mockResponse } from '../../../test/render';
import { http, HttpResponse } from 'msw';
import { server } from '../../../mocks/msw-node';
import MyStudiesPage from './MyStudiesPage';
import { BASE_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';
import { Study } from '../types';

const studies: Study[] = [
  {
    id: '1',
    role: 'LEADER',
    name: '점심메뉴 스터디',
    description: '매주 진행하는 점심메뉴 정하기',
    memberCount: 3,
    noticeCount: 2,
    assignmentCount: 2,
  },
  {
    id: '2',
    role: 'MEMBER',
    name: '저녁메뉴 스터디',
    description: '매주 진행하는 저녁메뉴 정하기',
    memberCount: 5,
    noticeCount: 1,
    assignmentCount: 0,
  },
];

const STUDIES_URL = `${BASE_URL}${STUDY_URLS.list}`;

test('응답으로 받은 스터디들을 렌더링 한다', async () => {
  mockResponse(STUDIES_URL, studies);

  render(<MyStudiesPage />, { wrapper: createWrapper() });

  expect(await screen.findAllByRole('listitem')).toHaveLength(studies.length);

  expect(screen.getByText('점심메뉴 스터디')).toBeInTheDocument();
  expect(screen.getByText('저녁메뉴 스터디')).toBeInTheDocument();
});

test('스터디 역할에 따라 다른 뱃지를 렌더링 한다', async () => {
  mockResponse(STUDIES_URL, studies);

  render(<MyStudiesPage />, { wrapper: createWrapper() });

  expect(await screen.findByText('스터디 리드')).toBeInTheDocument();
  expect(screen.getByText('스터디원')).toBeInTheDocument();
});

test('스터디 목록 요청이 실패하면 에러 메시지가 렌더링 한다', async () => {
  server.use(http.get(STUDIES_URL, () => new HttpResponse(null, { status: 500 })));

  render(<MyStudiesPage />, { wrapper: createWrapper() });

  expect(await screen.findByText('스터디 목록을 불러오는데 실패했습니다.')).toBeInTheDocument();
});

test('참여 중인 스터디가 없으면 비어있는 상태를 렌더링 한다', async () => {
  mockResponse(STUDIES_URL, []);

  render(<MyStudiesPage />, { wrapper: createWrapper() });

  expect(await screen.findByRole('heading', { name: '내 스터디' })).toBeInTheDocument();
  expect(screen.queryAllByRole('listitem')).toHaveLength(0);
});

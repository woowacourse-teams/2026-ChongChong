import { render, screen } from '@testing-library/react';
import { createWrapper, mockResponse } from '../../../../test/render';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../mocks/msw-node';
import MyStudiesPage from '../MyStudiesPage';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../mocks/db';
import { memberTable } from '../../../member/mocks/db';

const STUDIES_URL = `${API_URL}${STUDY_URLS.list}`;

async function setUpMockData() {
  const userId = 1;
  await userTable.create({
    id: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
  });

  await studyTable.create({
    id: 1,
    name: '탄자니아 스터디',
    description: '탄자니아 출신 벤지와 함께하는 탄자니아 치안',
    inviteLink: 'tanzania',
  });
  await studyTable.create({
    id: 2,
    name: '농구 스터디',
    description: '2m 이든과 함께하는 농구 스터디',
    inviteLink: 'basketball',
  });
  await studyTable.create({
    id: 3,
    name: '제빵 스터디',
    description: '맛있는 소금빵 만듭니다',
    inviteLink: 'bread',
  });

  await memberTable.create({
    id: 1,
    studyId: 1,
    userId: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  });

  await memberTable.create({
    id: 2,
    studyId: 2,
    userId: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  });
}

describe('스터디 목록 페이지 테스트', () => {
  beforeEach(async () => {
    await setUpMockData();
  });

  test('응답으로 받은 스터디들을 렌더링 한다', async () => {
    render(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findAllByRole('listitem')).toHaveLength(2);

    expect(screen.getByText('탄자니아 스터디')).toBeInTheDocument();
    expect(screen.getByText('농구 스터디')).toBeInTheDocument();
  });

  test('스터디 역할에 따라 다른 뱃지를 렌더링 한다', async () => {
    render(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('스터디 리드')).toBeInTheDocument();
    expect(screen.getByText('스터디원')).toBeInTheDocument();
  });

  test('스터디 목록 요청이 실패하면 에러 메시지가 렌더링 한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(STUDIES_URL, () => HttpResponse.error()));

    render(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('스터디 목록을 불러오는데 실패했습니다.')).toBeInTheDocument();
  });

  test('참여 중인 스터디가 없으면 비어있는 상태를 렌더링 한다', async () => {
    mockResponse(STUDIES_URL, []);

    render(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByRole('heading', { name: '내 스터디' })).toBeInTheDocument();
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
    expect(await screen.findByText('아직 스터디가 없어요')).toBeInTheDocument();
  });
});

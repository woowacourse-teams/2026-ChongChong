import { screen } from '@testing-library/react';
import { createWrapper, mockResponse, setup } from '../../../../test/render';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../mocks/msw-node';
import MyStudiesPage from '../MyStudiesPage';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { setUpMockData } from '../../mocks/node-mock';

const STUDIES_URL = `${API_URL}${STUDY_URLS.list}`;

describe('스터디 목록 페이지 테스트', () => {
  beforeEach(async () => {
    await setUpMockData();
  });

  test('응답으로 받은 스터디들을 렌더링 한다', async () => {
    setup(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findAllByRole('listitem')).toHaveLength(2);

    expect(screen.getByText('탄자니아 스터디')).toBeInTheDocument();
    expect(screen.getByText('농구 스터디')).toBeInTheDocument();
  });

  test('스터디 역할에 따라 다른 뱃지를 렌더링 한다', async () => {
    setup(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('스터디 리드')).toBeInTheDocument();
    expect(screen.getByText('스터디원')).toBeInTheDocument();
  });

  test('스터디 목록 요청이 실패하면 에러 메시지가 렌더링 한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(STUDIES_URL, () => HttpResponse.error()));
    setup(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('스터디 목록을 불러오는데 실패했습니다.')).toBeInTheDocument();
  });

  test('참여 중인 스터디가 없으면 비어있는 상태를 렌더링 한다', async () => {
    mockResponse(STUDIES_URL, []);

    setup(<MyStudiesPage />, { wrapper: createWrapper() });

    expect(await screen.findByRole('heading', { name: '내 스터디' })).toBeInTheDocument();
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
    expect(await screen.findByText('아직 스터디가 없어요')).toBeInTheDocument();
  });
});

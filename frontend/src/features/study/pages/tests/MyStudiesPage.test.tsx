import { screen, waitFor, within } from '@testing-library/react';
import { createWrapper, setup, login } from '../../../../test/render';
import { http, HttpResponse } from 'msw';
import { server } from '../../../../mocks/msw-node';
import MyStudiesPage from '../MyStudiesPage';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { studyTable } from '../../mocks/db';
import { userTable } from '../../../user/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { clearAccessToken as logout } from '../../../login/accessToken';

const STUDIES_URL = `${API_URL}${STUDY_URLS.list}`;

async function selectStudyTab(
  user: ReturnType<typeof setup>['user'],
  name: '내 스터디' | '스터디 둘러보기',
) {
  await user.click(screen.getByRole('tab', { name }));
}

describe('스터디 목록 페이지 테스트', () => {
  const userId = 1;

  beforeEach(async () => {
    await userTable.create({
      id: userId,
      name: '벤지',
      profileImage: 'http://localhost:8000',
    });
    login('벤지');
  });

  afterEach(() => {
    logout();
  });

  describe('Floating UI', () => {
    test('Floating을 열면 스터디 만들기와 참여하기, 닫기 메뉴가 보인다', async () => {
      const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });
      const floating = await screen.findByRole('button', { name: '스터디 메뉴 열기' });

      await user.click(floating);

      const menu = within(screen.getByRole('dialog', { name: '스터디 메뉴' }));
      await waitFor(() => expect(menu.getByRole('link', { name: /스터디 만들기/ })).toBeVisible());
      await waitFor(() =>
        expect(menu.getByRole('link', { name: /스터디 참여하기/ })).toBeVisible(),
      );
      await waitFor(() => expect(menu.getByRole('button', { name: '닫기' })).toBeVisible());
    });
  });

  describe('참여 중인 스터디가 있는 경우', () => {
    beforeEach(async () => {
      await studyTable.create({
        id: 1,
        name: '탄자니아 스터디',
        description: '탄자니아 출신 벤지와 함께하는 탄자니아 치안',
        inviteLink: 'tanzania',
        imageUrl: 'https://example.com/tanzania.png',
      });
      await studyTable.create({
        id: 2,
        name: '농구 스터디',
        description: '2m 이든과 함께하는 농구 스터디',
        inviteLink: 'basketball',
      });
      await memberTable.create({
        id: 1,
        studyId: 1,
        userId,
        name: '벤지',
        profileImage: 'http://localhost:8000',
        role: 'LEADER',
      });
      await memberTable.create({
        id: 2,
        studyId: 2,
        userId,
        name: '벤지',
        profileImage: 'http://localhost:8000',
        role: 'MEMBER',
      });
    });

    test('스터디 프로필 이미지가 렌더링된다', async () => {
      const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });

      await selectStudyTab(user, '내 스터디');

      const card = await screen.findByRole('link', { name: /탄자니아 스터디/ });
      const profile = within(card)
        .getAllByRole('presentation')
        .find((image) => image.getAttribute('src') === 'https://example.com/tanzania.png');
      expect(profile).toBeVisible();
    });

    test('응답으로 받은 스터디들을 렌더링 한다', async () => {
      const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });

      await selectStudyTab(user, '내 스터디');

      expect(await screen.findAllByRole('listitem')).toHaveLength(2);

      const tanzaniaCard = within(screen.getByRole('link', { name: /탄자니아 스터디/ }));
      expect(tanzaniaCard.getByText('탄자니아 스터디')).toBeInTheDocument();
      expect(
        tanzaniaCard.getByText('탄자니아 출신 벤지와 함께하는 탄자니아 치안'),
      ).toBeInTheDocument();
      expect(tanzaniaCard.getByText('1명')).toBeInTheDocument();
    });

    test('스터디 역할에 따라 다른 뱃지를 렌더링 한다', async () => {
      const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });

      await selectStudyTab(user, '내 스터디');

      expect(await screen.findByText('스터디 리드')).toBeInTheDocument();
      expect(screen.getByText('스터디원')).toBeInTheDocument();
    });
  });

  test('스터디 목록 요청이 실패하면 에러 메시지가 렌더링 한다', async () => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
    server.use(http.get(STUDIES_URL, () => HttpResponse.error()));
    const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });

    await selectStudyTab(user, '내 스터디');

    expect(await screen.findByText('스터디 목록을 불러오는데 실패했습니다.')).toBeInTheDocument();
  });

  test('참여 중인 스터디가 없으면 비어있는 상태를 렌더링 한다', async () => {
    const { user } = setup(<MyStudiesPage />, { wrapper: createWrapper() });

    await selectStudyTab(user, '내 스터디');

    expect(await screen.findByText('아직 스터디가 없어요')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '내 스터디' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.queryAllByRole('listitem')).toHaveLength(0);
  });
});

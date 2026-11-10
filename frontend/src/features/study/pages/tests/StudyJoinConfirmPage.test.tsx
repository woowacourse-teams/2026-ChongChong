import { screen } from '@testing-library/react';
import { Route } from 'react-router';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../mocks/db';
import StudyJoinConfirmPage from '../StudyJoinConfirmPage';
import StudyJoinPage from '../StudyJoinPage';

describe('스터디 참여 확인 페이지 테스트', () => {
  beforeEach(async () => {
    sessionStorage.clear();
    await userTable.create({
      id: 1,
      name: '쿠무',
      profileImage: 'http://localhost:8000',
    });
    login('쿠무');
  });

  afterEach(() => {
    logout();
    sessionStorage.clear();
  });

  test('유효한 초대 토큰으로 접근하면 해당 스터디 정보가 보인다', async () => {
    await studyTable.create({
      id: 1,
      name: '타입스크립트 스터디',
      description: '타입스크립트를 함께 공부합니다',
      inviteLink: 'typescript',
      imageUrl: 'http://localhost:8000/study.png',
    });

    setup(<StudyJoinConfirmPage />, {
      wrapper: createWrapper({
        initialEntries: ['/studies/join/confirm?token=typescript'],
      }),
    });

    expect(await screen.findByRole('heading', { name: '타입스크립트 스터디' })).toBeVisible();
    expect(screen.getByText('타입스크립트를 함께 공부합니다')).toBeVisible();
    expect(screen.getByRole('img', { name: '타입스크립트 스터디 프로필' })).toHaveAttribute(
      'src',
      'http://localhost:8000/study.png',
    );
  });

  test('토큰 없이 스터디 참여 확인 페이지에 접근하면 참여 입력 페이지로 이동한다', async () => {
    setup(<StudyJoinConfirmPage />, {
      wrapper: createWrapper({
        initialEntries: ['/studies/join/confirm'],
        routes: (element) => (
          <>
            <Route path="/studies/join/confirm" element={element} />
            <Route path="/studies/join" element={<StudyJoinPage />} />
          </>
        ),
      }),
    });

    expect(await screen.findByRole('textbox', { name: '초대 링크' })).toBeVisible();
    expect(screen.getByRole('textbox', { name: '초대 링크' })).toHaveValue('');
  });
});

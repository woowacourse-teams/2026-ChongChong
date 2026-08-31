import { Suspense } from 'react';
import { Routes, Route } from 'react-router';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createWrapper } from '../../../../test/render';
import StudyJoinPage from '../StudyJoinPage';
import { STUDY_URLS } from '../../urls';
import StudyDetailPage from '../StudyDetailPage';
import { setAccessToken, clearAccessToken } from '../../../login/accessToken';
import { mockStudies } from '../../mocks/db';

describe('스터디 참가 폼 테스트', () => {
  afterEach(() => clearAccessToken());

  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 참여하기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const user = userEvent.setup();
    render(<StudyJoinPage />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(nameInput, '씨없는팀으로 초대합니다');
    const button = screen.getByRole('button', { name: '스터디 참여하기' });
    expect(button).toBeEnabled();
  });

  test('스터디 참여에 성공했을때 스터디 디테일 페이지로 이동한다', async () => {
    setAccessToken('2');
    const { inviteLink } = mockStudies[1];
    const user = userEvent.setup();
    render(
      <Suspense fallback={null}>
        <Routes>
          <Route path={STUDY_URLS.join} element={<StudyJoinPage />}></Route>
          <Route path="/studies/:studyId" element={<StudyDetailPage />}></Route>
        </Routes>
      </Suspense>,
      { wrapper: createWrapper({ initialEntries: [STUDY_URLS.join] }) },
    );

    const linkInput = screen.getByRole('textbox', { name: '초대 링크' });
    await user.type(linkInput, inviteLink);
    await user.click(screen.getByRole('button', { name: '스터디 참여하기' }));

    expect(await screen.findByText('안톨리니 · 스터디원')).toBeInTheDocument();
  });
});

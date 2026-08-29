import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useParams } from 'react-router';

import { createWrapper } from '../../../test/render';
import { STUDY_URLS } from '../urls';
import StudyForm from './StudyForm';

function StudyDetailPath() {
  const { studyId } = useParams();

  return <p>스터디 ID: {studyId}</p>;
}

describe('StudyForm 테스트', () => {
  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 만들기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    const user = userEvent.setup();
    render(<StudyForm />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '스터디 이름' });
    await user.type(nameInput, '치킨');
    const button = screen.getByRole('button', { name: '스터디 만들기' });
    expect(button).toBeEnabled();
  });

  test('스터디를 생성하면 해당 스터디 페이지로 이동한다.', async () => {
    const user = userEvent.setup();
    render(
      <Routes>
        <Route path={STUDY_URLS.create} element={<StudyForm />} />
        <Route path="/studies/:studyId" element={<StudyDetailPath />} />
      </Routes>,
      { wrapper: createWrapper({ initialEntries: [STUDY_URLS.create] }) },
    );

    const nameInput = screen.getByRole('textbox', { name: '스터디 이름' });
    await user.type(nameInput, '피자');
    const button = screen.getByRole('button', { name: '스터디 만들기' });
    await user.click(button);

    expect(await screen.findByText(/^스터디 ID: \d+$/)).toBeInTheDocument();
  });
});

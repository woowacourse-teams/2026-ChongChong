import { http } from 'msw';
import { render, screen } from '@testing-library/react';
import { server } from '../../../../mocks/msw-node';
import userEvent from '@testing-library/user-event';
import { Route, Routes, useParams } from 'react-router';

import { createWrapper } from '../../../../test/render';
import { STUDY_URLS } from '../../urls';
import { setAccessToken } from '../../../login/accessToken';
import { API_URL } from '../../../../../config';
import { invalidInputResponse } from '../../../../mocks/errors';
import StudyForm from '../StudyForm';

function StudyDetailPath() {
  const { studyId } = useParams();

  return <p>스터디 ID: {studyId}</p>;
}

describe('스터디폼 테스트', () => {
  let user: ReturnType<typeof userEvent.setup>;
  beforeEach(() => {
    user = userEvent.setup();
    // 스터디 1의 리더만 과제를 생성할 수 있습니다.
    setAccessToken('5');
  });

  test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const button = screen.getByRole('button', { name: '스터디 만들기' });

    expect(button).toBeDisabled();
  });

  test('입력이 유효하면 버튼은 활성화 된다', async () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const nameInput = screen.getByRole('textbox', { name: '스터디 이름' });
    await user.type(nameInput, '치킨');
    const button = screen.getByRole('button', { name: '스터디 만들기' });
    expect(button).toBeEnabled();
  });

  test('스터디를 생성하면 해당 스터디 페이지로 이동한다', async () => {
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

  test('제목 입력은 15자로 제한된다', async () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const name = screen.getByRole('textbox', { name: '스터디 이름' });
    await user.type(name, '안톨리니'.repeat(20));
    expect(name).toHaveValue('안톨리니안톨리니안톨리니안톨리');
  });

  test('설명 입력은 30자로 제한된다', async () => {
    render(<StudyForm />, { wrapper: createWrapper() });

    const description = screen.getByRole('textbox', { name: '어떤 스터디인가요?' });
    await user.type(description, '디움'.repeat(50));

    expect(description).toHaveValue('디움'.repeat(15));
  });

  test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
    server.use(
      http.post(`${API_URL}${STUDY_URLS.create}`, () =>
        invalidInputResponse([
          { field: 'name', code: 'SOME_ERROR', reason: '이름에 문제가 있어요' },
          { field: 'description', code: 'SOME_ERROR', reason: '설명에 문제가 있어요' },
        ]),
      ),
    );

    render(<StudyForm />, { wrapper: createWrapper() });

    await user.type(screen.getByRole('textbox', { name: '스터디 이름' }), '치킨');
    await user.click(screen.getByRole('button', { name: '스터디 만들기' }));

    expect(await screen.findByText('이름에 문제가 있어요')).toBeInTheDocument();
    expect(await screen.findByText('설명에 문제가 있어요')).toBeInTheDocument();
  });
});

import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import StudyEditPage from '../StudyEditPage';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { studyTable } from '../../mocks/db';
import { userTable } from '../../../user/mocks/db';
import { memberTable } from '../../../member/mocks/db';

const STUDY_INFO_URL = `${API_URL}${STUDY_URLS.info}`;
const STUDY_DETAIL_URL = `${API_URL}${STUDY_URLS.detail}`;

function setupStudyEditPage() {
  return setup(<StudyEditPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/2/edit'],
      routes: (element) => (
        <>
          <Route path="/studies/:studyId/edit" element={element} />
          <Route path="/studies/2" element={<h1>스터디 상세</h1>} />
        </>
      ),
    }),
  });
}

async function findNameInput() {
  return screen.findByRole('textbox', { name: '스터디 이름' });
}

function getDescriptionInput() {
  return screen.getByRole('textbox', { name: '스터디 설명' });
}

function getSubmitButton() {
  return screen.getByRole('button', { name: '스터디 수정하기' });
}

describe('스터디 수정 페이지 테스트', () => {
  const leaderUserName = '피즈';

  beforeEach(async () => {
    await userTable.create({
      id: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
    });
    await studyTable.create({
      id: 2,
      name: '협곡 스터디',
      description: '피즈의 피즈 강의',
      inviteLink: 'bronze',
    });
    await memberTable.create({
      id: 1,
      studyId: 2,
      userId: 1,
      name: leaderUserName,
      profileImage: 'http://localhost:8000',
      role: 'LEADER',
    });
    login(leaderUserName);
  });

  afterEach(() => {
    logout();
  });

  describe('스터디 수정 폼', () => {
    test('기존 스터디 값을 입력 필드 초기값으로 반영한다', async () => {
      setupStudyEditPage();

      expect(await findNameInput()).toHaveValue('협곡 스터디');
      expect(getDescriptionInput()).toHaveValue('피즈의 피즈 강의');
      expect(getSubmitButton()).toBeEnabled();
    });

    test('필수 입력값이 채워지지 않으면 수정 버튼은 비활성화 된다', async () => {
      const { user } = setupStudyEditPage();
      const nameInput = await findNameInput();
      await user.clear(nameInput);

      expect(getSubmitButton()).toBeDisabled();
    });

    test('필수 입력값이 채워지면 수정 버튼은 활성화 된다', async () => {
      setupStudyEditPage();
      const nameInput = await findNameInput();
      expect(nameInput).toHaveValue();

      expect(getSubmitButton()).toBeEnabled();
    });

    test('필드 오류를 해당 입력 필드에 표시한다', async () => {
      server.use(
        http.patch(STUDY_DETAIL_URL, () =>
          invalidInputResponse([
            { field: 'name', code: 'INVALID', reason: '스터디 이름을 확인해주세요.' },
            { field: 'description', code: 'INVALID', reason: '스터디 설명을 확인해주세요.' },
          ]),
        ),
      );
      const { user } = setupStudyEditPage();

      await findNameInput();
      await user.click(getSubmitButton());

      expect(
        await within(screen.getByTestId('study-name-field')).findByText(
          '스터디 이름을 확인해주세요.',
        ),
      ).toBeVisible();
      expect(
        within(screen.getByTestId('study-description-field')).getByText(
          '스터디 설명을 확인해주세요.',
        ),
      ).toBeVisible();
    });

    test('다시 제출해 검증을 통과한 필드의 오류 메시지는 지워진다', async () => {
      let requestCount = 0;
      server.use(
        http.patch(STUDY_DETAIL_URL, () => {
          requestCount += 1;
          return invalidInputResponse([
            requestCount === 1
              ? { field: 'name', code: 'INVALID', reason: '스터디 이름을 확인해주세요.' }
              : { field: 'description', code: 'INVALID', reason: '스터디 설명을 확인해주세요.' },
          ]);
        }),
      );
      const { user } = setupStudyEditPage();

      const nameInput = await findNameInput();
      await user.click(getSubmitButton());
      expect(await screen.findByText('스터디 이름을 확인해주세요.')).toBeVisible();

      await user.clear(nameInput);
      await user.type(nameInput, '수정한 협곡 스터디');
      await user.click(getSubmitButton());

      expect(await screen.findByText('스터디 설명을 확인해주세요.')).toBeVisible();
      expect(screen.queryByText('스터디 이름을 확인해주세요.')).not.toBeInTheDocument();
      expect(nameInput).toHaveValue('수정한 협곡 스터디');
    });

    test.each([
      {
        title: '스터디 리더가 아니면',
        handler: http.patch(STUDY_DETAIL_URL, () =>
          HttpResponse.json(
            { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '요청한 작업을 수행할 권한이 없습니다.',
      },
      {
        title: '네트워크 오류가 발생하면',
        handler: http.patch(STUDY_DETAIL_URL, () => HttpResponse.error()),
        message: '스터디를 수정하는데 실패했습니다.',
      },
    ])('$title 오류 메시지를 토스트로 표시하고 입력값을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupStudyEditPage();

      const nameInput = await findNameInput();
      await user.clear(nameInput);
      await user.type(nameInput, '수정한 협곡 스터디');
      await user.clear(getDescriptionInput());
      await user.type(getDescriptionInput(), '매주 토요일에 모여요');
      await user.click(getSubmitButton());

      const toast = await screen.findByRole('status', {}, { timeout: 3000 });
      expect(toast).toHaveTextContent(message);
      expect(toast).toBeVisible();
      expect(nameInput).toHaveValue('수정한 협곡 스터디');
      expect(getDescriptionInput()).toHaveValue('매주 토요일에 모여요');
      expect(getSubmitButton()).toBeEnabled();
    });
  });

  describe('스터디 조회 실패', () => {
    beforeEach(() => {
      jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    test.each([
      {
        title: '스터디에 대한 접근 권한이 없으면',
        handler: http.get(STUDY_INFO_URL, () =>
          HttpResponse.json(
            { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
            { status: 403 },
          ),
        ),
        message: '해당 스터디에 대한 접근 권한이 없습니다.',
      },
      {
        title: '네트워크 오류가 발생하면',
        handler: http.get(STUDY_INFO_URL, () => HttpResponse.error()),
        message: '스터디 정보를 불러오는데 실패했습니다.',
      },
    ])('$title 본문에 오류 메시지를 표시한다', async ({ handler, message }) => {
      server.use(handler);
      setupStudyEditPage();

      expect(await within(screen.getByRole('main')).findByText(message)).toBeVisible();
      expect(screen.queryByRole('textbox', { name: '스터디 이름' })).not.toBeInTheDocument();
    });
  });
});

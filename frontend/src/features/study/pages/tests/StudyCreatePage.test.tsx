import { invalidImageFileCases } from './fixtures/invalidImageFiles';
import userEvent from '@testing-library/user-event';
import rabbit from '../../../../shared/assets/icons/header-icon.svg?width=210&as=webp';
import {
  mockImageUpload,
  PROFILE_PREVIEW_URL,
  setupImageBrowserMocks,
} from './fixtures/imageUpload';
import { screen, fireEvent, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { createWrapper, setup, login } from '../../../../test/render';
import { createTextWithLength, insertTextInMiddle } from '../../../../test/input';
import { server } from '../../../../mocks/msw-node';
import { API_URL } from '../../../../../config';
import { STUDY_URLS } from '../../urls';
import { invalidInputResponse } from '../../../../mocks/errors';
import { userTable } from '../../../user/mocks/db';
import { clearAccessToken as logout } from '../../../login/accessToken';
import CreateStudyPage from '../CreateStudyPage';

const STUDY_CREATE_URL = `${API_URL}${STUDY_URLS.create}`;

function setupStudyCreatePage() {
  return setup(<CreateStudyPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/new'],
      routes: (element) => (
        <>
          <Route path="/studies/new" element={element} />
        </>
      ),
    }),
  });
}

function studyNameInput() {
  return screen.getByRole('textbox', { name: '스터디 이름' });
}

function studyDescriptionInput() {
  return screen.getByRole('textbox', { name: '스터디 설명' });
}

function studyCreateButton() {
  return screen.getByRole('button', { name: '스터디 만들기' });
}

describe('스터디 생성 페이지', () => {
  describe('입력값 검증', () => {
    test('입력이 유효하지 않으면 버튼은 비활성화 된다', () => {
      setupStudyCreatePage();

      expect(studyCreateButton()).toBeDisabled();
    });

    test('입력이 유효하면 버튼은 활성화 된다', async () => {
      const { user } = setupStudyCreatePage();

      await user.type(studyNameInput(), '치킨');

      expect(studyCreateButton()).toBeEnabled();
    });

    test('이름이 15자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
      setupStudyCreatePage();
      const nameInput = studyNameInput();
      const originalValue = createTextWithLength(15);

      fireEvent.change(nameInput, { target: { value: originalValue } });
      expect(nameInput).toHaveValue(originalValue);

      insertTextInMiddle(nameInput, originalValue);

      expect(nameInput).toHaveValue(originalValue);
    });

    test('이름의 중간 삽입 결과가 15자 이하면 입력을 반영한다', () => {
      setupStudyCreatePage();
      const nameInput = studyNameInput();
      const originalValue = createTextWithLength(14);

      fireEvent.change(nameInput, { target: { value: originalValue } });
      expect(nameInput).toHaveValue(originalValue);

      const insertedValue = insertTextInMiddle(nameInput, originalValue);

      expect(nameInput).toHaveValue(insertedValue);
    });

    test('빈 설명에 30자를 초과한 값이 전달되면 입력을 막는다', () => {
      setupStudyCreatePage();
      const descriptionInput = studyDescriptionInput();

      fireEvent.change(descriptionInput, { target: { value: createTextWithLength(31) } });

      expect(descriptionInput).toHaveValue('');
    });

    test('설명이 30자일 때 중간에 글자를 삽입해도 기존 값을 유지한다', () => {
      setupStudyCreatePage();
      const descriptionInput = studyDescriptionInput();
      const originalValue = createTextWithLength(30);

      fireEvent.change(descriptionInput, { target: { value: originalValue } });
      expect(descriptionInput).toHaveValue(originalValue);

      insertTextInMiddle(descriptionInput, originalValue);

      expect(descriptionInput).toHaveValue(originalValue);
    });

    test('설명의 중간 삽입 결과가 30자 이하면 입력을 반영한다', () => {
      setupStudyCreatePage();
      const descriptionInput = studyDescriptionInput();
      const originalValue = createTextWithLength(29);

      fireEvent.change(descriptionInput, { target: { value: originalValue } });
      expect(descriptionInput).toHaveValue(originalValue);

      const insertedValue = insertTextInMiddle(descriptionInput, originalValue);

      expect(descriptionInput).toHaveValue(insertedValue);
    });
  });

  describe('스터디 생성 요청', () => {
    beforeEach(async () => {
      await userTable.create({
        id: 1,
        name: '안톨리니',
        profileImage: 'http://localhost:8000',
      });
      login('안톨리니');
    });

    afterEach(() => {
      logout();
    });

    // E2E 테스트로 전환합니다.
    // test('스터디를 생성하면 해당 스터디 페이지로 이동한다', async () => {
    //   const { user } = setupStudyCreatePage();

    //   await user.type(studyNameInput(), '피자 스터디');
    //   await user.click(studyCreateButton());

    //   expect(await screen.findByRole('heading', { name: '피자 스터디' })).toBeInTheDocument();
    // });

    test('필드 에러가 발생하면 에러메시지가 표시 된다', async () => {
      server.use(
        http.post(STUDY_CREATE_URL, () =>
          invalidInputResponse([
            { field: 'name', code: 'SOME_ERROR', reason: '이름에 문제가 있어요' },
            { field: 'description', code: 'SOME_ERROR', reason: '설명에 문제가 있어요' },
          ]),
        ),
      );
      const { user } = setupStudyCreatePage();

      await user.type(studyNameInput(), '치킨');
      await user.click(studyCreateButton());

      expect(await screen.findByText('이름에 문제가 있어요')).toBeInTheDocument();
      expect(await screen.findByText('설명에 문제가 있어요')).toBeInTheDocument();
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    test.each([
      {
        title: '스터디 가입 상한을 초과하면',
        handler: http.post(STUDY_CREATE_URL, () =>
          HttpResponse.json(
            {
              code: 'JOINED_STUDY_LIMIT_EXCEEDED',
              message: '가입할 수 있는 스터디는 최대 50개입니다.',
            },
            { status: 409 },
          ),
        ),
        message: '가입할 수 있는 스터디는 최대 50개입니다.',
      },
      {
        title: '네트워크 오류가 발생하면',
        handler: http.post(STUDY_CREATE_URL, () => HttpResponse.error()),
        message: '스터디를 생성하는데 실패했습니다.',
      },
    ])('$title Toast 에러 메시지를 표시하고 입력값을 유지한다', async ({ handler, message }) => {
      server.use(handler);
      const { user } = setupStudyCreatePage();

      await user.type(studyNameInput(), '치킨');
      await user.click(studyCreateButton());

      const toast = await screen.findByRole('status');
      expect(toast).toHaveTextContent(message);
      expect(toast).toBeVisible();
      expect(studyNameInput()).toHaveValue('치킨');
    });
  });

  describe('스터디 프로필 이미지', () => {
    setupImageBrowserMocks();

    test('이미지가 없으면 바텀시트에 이미지 선택과 취소만 보인다', async () => {
      const { user } = setupStudyCreatePage();

      await user.click(await screen.findByRole('button', { name: '스터디 프로필 이미지 변경' }));

      const sheet = within(screen.getByRole('dialog', { name: '프로필 이미지' }));
      expect(sheet.getByRole('button', { name: '프로필 이미지 선택' })).toBeVisible();
      expect(sheet.getByRole('button', { name: '취소' })).toBeVisible();
    });

    test('이미지를 선택했을때는 바텀시트에 다른 액션들도 보인다', async () => {
      mockImageUpload();
      const { user } = setupStudyCreatePage();
      const file = new File(['profile-image'], 'profile.png', { type: 'image/png' });

      await user.click(await screen.findByRole('button', { name: '스터디 프로필 이미지 변경' }));
      await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
      await user.upload(screen.getByLabelText('프로필 이미지 파일'), file);
      await waitFor(() => {
        expect(screen.getByRole('button', { name: '스터디 프로필 이미지 변경' })).toBeEnabled();
      });
      await user.click(screen.getByRole('button', { name: '스터디 프로필 이미지 변경' }));

      const sheet = within(screen.getByRole('dialog', { name: '프로필 이미지' }));
      expect(sheet.getByRole('button', { name: '프로필 이미지 선택' })).toBeVisible();
      expect(sheet.getByRole('button', { name: '기본 프로필 이미지 사용' })).toBeVisible();
      expect(sheet.getByRole('button', { name: '취소' })).toBeVisible();
    });

    test('프로필 이미지가 없으면 기본 프로필 이미지가 보인다', async () => {
      setupStudyCreatePage();

      const profile = await screen.findByRole('img', { name: '스터디 프로필' });
      expect(profile).toHaveAttribute('src', rabbit);
      expect(profile).toBeVisible();
    });

    test('프로필 이미지를 변경하면 선택한 프로필 이미지가 보인다', async () => {
      mockImageUpload();
      const { user } = setupStudyCreatePage();
      const file = new File(['profile-image'], 'profile.png', { type: 'image/png' });

      await user.click(await screen.findByRole('button', { name: '스터디 프로필 이미지 변경' }));
      await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
      await user.upload(screen.getByLabelText('프로필 이미지 파일'), file);

      await waitFor(() => {
        expect(screen.getByRole('img', { name: '스터디 프로필' })).toHaveAttribute(
          'src',
          PROFILE_PREVIEW_URL,
        );
      });
      expect(screen.getByRole('img', { name: '스터디 프로필' })).toBeVisible();
    });

    test.each(invalidImageFileCases)(
      '$title을 선택하면 에러 토스트가 보이고 기존 이미지를 유지한다',
      async ({ createFile, message }) => {
        setupStudyCreatePage();
        // accept 필터를 통과시켜 애플리케이션의 실제 파일 검증을 실행합니다.
        const user = userEvent.setup({ applyAccept: false });

        await user.click(await screen.findByRole('button', { name: '스터디 프로필 이미지 변경' }));
        await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
        await user.upload(screen.getByLabelText('프로필 이미지 파일'), createFile());

        const toast = await screen.findByRole('status');
        expect(toast).toHaveTextContent(message);
        expect(toast).toBeVisible();
        expect(screen.getByRole('img', { name: '스터디 프로필' })).toHaveAttribute('src', rabbit);
      },
    );

    test('프로필 이미지 업로드에 실패하면 토스트 에러 메시지가 보인다', async () => {
      mockImageUpload();
      server.use(
        http.post(`${API_URL}/files/100/complete`, () =>
          HttpResponse.json(
            { code: 'FILE_UPLOAD_NOT_FOUND', message: '업로드한 파일을 찾을 수 없습니다.' },
            { status: 409 },
          ),
        ),
      );
      const { user } = setupStudyCreatePage();
      const file = new File(['profile-image'], 'profile.png', { type: 'image/png' });

      await user.click(await screen.findByRole('button', { name: '스터디 프로필 이미지 변경' }));
      await user.click(screen.getByRole('button', { name: '프로필 이미지 선택' }));
      await user.upload(screen.getByLabelText('프로필 이미지 파일'), file);

      const toast = await screen.findByRole('status');
      expect(toast).toHaveTextContent('업로드한 파일을 찾을 수 없습니다.');
      expect(toast).toBeVisible();
    });
  });
});

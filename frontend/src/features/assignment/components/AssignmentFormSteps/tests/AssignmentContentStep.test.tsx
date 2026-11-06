// Node fetch가 파일 본문을 직렬화할 수 있도록 jsdom 대신 Node의 File을 사용한다.
import { File } from 'node:buffer';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../../../config';
import { createWrapper, login, logout, setup } from '../../../../../test/render';
import { server } from '../../../../../mocks/msw-node';
import { userTable } from '../../../../user/mocks/db';
import { Route } from 'react-router';
import { studyTable } from '../../../../study/mocks/db';
import { memberTable } from '../../../../member/mocks/db';
import CreateAssignmentPage from '../../../pages/CreateAssignmentPage';

function setupContentStep() {
  return setup(<CreateAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/assignments/create'],
      routes: (element) => <Route path="/studies/:studyId/assignments/create" element={element} />,
    }),
  });
}

describe('AssignmentContentStep Tests', () => {
  beforeEach(async () => {
    await userTable.create({ id: 1, name: '안톨리니', profileImage: null });
    await studyTable.create({
      id: 1,
      name: '리액트 스터디',
      description: '리액트 상태 관리를 함께 공부합니다.',
      inviteLink: 'react-study',
    });
    await memberTable.create({
      id: 1,
      studyId: 1,
      userId: 1,
      name: '안톨리니',
      profileImage: null,
      role: 'LEADER',
    });
    login('안톨리니');
  });

  afterEach(logout);

  test('입력값이 채워지지 않은 상태에서 다음 버튼을 클릭하면 다음 페이지로 이동되지 않는다', async () => {
    const { user } = setupContentStep();

    await user.click(screen.getByRole('button', { name: '다음' }));

    expect(screen.getByRole('heading', { name: '어떤 과제를 함께 할까요?' })).toBeVisible();
  });

  test.each(['제목', '내용', '제출 방법'])(
    '필수 입력값인 %s이 채워지지 않으면 다음 버튼이 비활성화되어 있다',
    async (label) => {
      const { user } = setupContentStep();
      await user.type(screen.getByRole('textbox', { name: '제목' }), '리액트 상태 관리');
      await user.type(
        screen.getByRole('textbox', { name: '내용' }),
        'Context 사용 경험을 정리해주세요.',
      );
      await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '블로그 링크 제출');

      await user.clear(screen.getByRole('textbox', { name: label }));
      await user.type(screen.getByRole('textbox', { name: label }), '   ');

      expect(screen.getByRole('button', { name: '다음' })).toBeDisabled();
    },
  );

  test('필수 입력값이 채워지면 다음 버튼이 활성화 된다', async () => {
    const { user } = setupContentStep();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '리액트 상태 관리');
    await user.type(
      screen.getByRole('textbox', { name: '내용' }),
      'Context 사용 경험을 정리해주세요.',
    );
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '블로그 링크 제출');

    expect(screen.getByRole('button', { name: '다음' })).toBeEnabled();
  });

  test('유효한 입력일 때 다음 버튼을 클릭하면 다음 페이지로 이동한다', async () => {
    const { user } = setupContentStep();

    await user.type(screen.getByRole('textbox', { name: '제목' }), '리액트 상태 관리');
    await user.type(
      screen.getByRole('textbox', { name: '내용' }),
      'Context 사용 경험을 정리해주세요.',
    );
    await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '블로그 링크 제출');
    await user.click(screen.getByRole('button', { name: '다음' }));

    expect(screen.getByRole('heading', { name: '과제 일정을 정해주세요' })).toBeVisible();
  });

  describe('이미지 업로드', () => {
    beforeEach(() => {
      jest.spyOn(URL, 'createObjectURL').mockImplementation(() => `blob:${crypto.randomUUID()}`);
      jest.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    });

    test('이미지를 업로드하면 이미지 이름과 사진이 보인다', async () => {
      const { user } = setupContentStep();
      const file = new File(['diagram'], '상태관리.png', { type: 'image/png' });

      await user.upload(screen.getByLabelText('이미지 업로드'), file);

      const attachments = within(screen.getByRole('list', { name: '첨부 파일' }));
      await attachments.findByText('업로드 완료');
      expect(attachments.getByText('상태관리.png')).toBeVisible();
      expect(attachments.getByRole('img', { name: '상태관리.png 미리보기' })).toBeVisible();
    });

    test('엑스 버튼을 누르면 업로드된 이미지를 제외할 수 있다', async () => {
      const { user } = setupContentStep();
      await user.upload(
        screen.getByLabelText('이미지 업로드'),
        new File(['diagram'], '상태관리.png', { type: 'image/png' }),
      );
      const attachments = within(screen.getByRole('list', { name: '첨부 파일' }));
      await attachments.findByText('업로드 완료');

      await user.click(attachments.getByRole('button', { name: '상태관리.png 삭제' }));

      expect(attachments.queryByText('상태관리.png')).not.toBeInTheDocument();
    });

    test('업로드된 이미지를 클릭하면 사진을 크게 볼 수 있다', async () => {
      const { user } = setupContentStep();
      await user.upload(
        screen.getByLabelText('이미지 업로드'),
        new File(['diagram'], '상태관리.png', { type: 'image/png' }),
      );
      await screen.findByText('업로드 완료');

      await user.click(screen.getByRole('button', { name: '상태관리.png 크게 보기' }));

      const dialog = within(screen.getByRole('dialog', { name: '상태관리.png 크게 보기' }));
      expect(dialog.getByRole('img', { name: '상태관리.png' })).toBeVisible();
    });

    test('이미지가 여러개 업로드된 상태일때 클릭하면 확대된 사진을 넘기며 볼 수 있다', async () => {
      const { user } = setupContentStep();
      await user.upload(screen.getByLabelText('이미지 업로드'), [
        new File(['before'], '변경전.png', { type: 'image/png' }),
        new File(['after'], '변경후.png', { type: 'image/png' }),
      ]);
      await waitFor(() => expect(screen.getAllByText('업로드 완료')).toHaveLength(2));
      await user.click(screen.getByRole('button', { name: '변경전.png 크게 보기' }));

      await user.click(
        within(screen.getByRole('dialog')).getByRole('button', { name: '다음 이미지' }),
      );
      expect(
        within(screen.getByRole('dialog')).getByRole('img', { name: '변경후.png' }),
      ).toBeVisible();

      await user.click(
        within(screen.getByRole('dialog')).getByRole('button', { name: '이전 이미지' }),
      );
      expect(
        within(screen.getByRole('dialog')).getByRole('img', { name: '변경전.png' }),
      ).toBeVisible();
    });

    test('이미지 업로드에 실패하면 해당 파일에 에러 메시지가 렌더링된다', async () => {
      server.use(
        http.post(`${API_URL}/files/:fileId/complete`, () =>
          HttpResponse.json(
            { code: 'FILE_UPLOAD_NOT_FOUND', message: '업로드한 파일을 찾을 수 없습니다.' },
            { status: 409 },
          ),
        ),
      );
      const { user } = setupContentStep();

      await user.upload(
        screen.getByLabelText('이미지 업로드'),
        new File(['diagram'], '상태관리.png', { type: 'image/png' }),
      );

      const file = within(screen.getByRole('list', { name: '첨부 파일' })).getByRole('listitem');
      expect(await within(file).findByRole('alert')).toHaveTextContent(
        '업로드한 파일을 찾을 수 없습니다.',
      );
    });

    test('허용하지 않는 확장자 업로드 시도하면 에러 메시지가 렌더링 된다', async () => {
      setupContentStep();
      const user = userEvent.setup({ applyAccept: false });

      await user.upload(
        screen.getByLabelText('이미지 업로드'),
        new File(['text'], '과제.txt', { type: 'text/plain' }),
      );

      expect(screen.getByRole('alert')).toHaveTextContent(
        'JPEG, PNG, WebP 이미지만 첨부할 수 있어요.',
      );
    });

    test('이미지를 10장 초과 업로드 시도하면 에러 메시지가 렌더링 된다', async () => {
      const { user } = setupContentStep();
      const files = Array.from(
        { length: 11 },
        (_, index) => new File(['image'], `과제${index}.png`, { type: 'image/png' }),
      );

      await user.upload(screen.getByLabelText('이미지 업로드'), files);

      expect(screen.getByRole('alert')).toHaveTextContent(
        '이미지는 최대 10장까지 첨부할 수 있어요.',
      );
    });

    test('5MB가 넘는 이미지를 업로드 시도하면 에러 메시지가 렌더링 된다', async () => {
      const { user } = setupContentStep();
      const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], '과제.png', {
        type: 'image/png',
      });

      await user.upload(screen.getByLabelText('이미지 업로드'), file);

      expect(screen.getByRole('alert')).toHaveTextContent('파일당 최대 5MB까지 첨부할 수 있어요.');
    });
  });
});

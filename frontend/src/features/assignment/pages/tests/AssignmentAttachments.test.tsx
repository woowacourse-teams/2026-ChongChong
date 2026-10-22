import { screen, waitFor, within, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../../shared/modules/files/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { assignmentTable } from '../../mocks/db';
import CreateAssignmentPage from '../CreateAssignmentPage';
import EditAssignmentPage from '../EditAssignmentPage';
import AssignmentDetailPage from '../AssignmentDetailPage';

const image = (name = '자료.png') => new File(['image'], name, { type: 'image/png' });

async function prepareStudy() {
  await studyTable.create({
    id: 1,
    name: '첨부 스터디',
    description: '이미지 과제',
    inviteLink: 'images',
  });
  await memberTable.create({
    id: 1,
    studyId: 1,
    userId: 1,
    name: '리더',
    profileImage: null,
    role: 'LEADER',
  });
}

async function setupPage(mode: 'create' | 'edit') {
  await prepareStudy();
  return setup(mode === 'create' ? <CreateAssignmentPage /> : <EditAssignmentPage />, {
    wrapper: createWrapper({
      initialEntries: [
        mode === 'create' ? '/studies/1/assignments/create' : '/studies/1/assignments/10/edit',
      ],
      routes: (element) => (
        <>
          <Route
            path={
              mode === 'create'
                ? '/studies/:studyId/assignments/create'
                : '/studies/:studyId/assignments/:assignmentId/edit'
            }
            element={element}
          />
          <Route
            path="/studies/:studyId/assignments/:assignmentId"
            element={<AssignmentDetailPage />}
          />
        </>
      ),
    }),
  });
}

async function existingAssignment(ids: number[]) {
  for (const id of ids) {
    await fileTable.create({
      id,
      ownerUserId: 1,
      originalFileName: `기존-${id}.png`,
      contentType: 'image/png',
      size: 5,
      uploadedSize: 5,
      status: 'COMPLETED',
    });
  }
  await assignmentTable.create({
    id: 10,
    studyId: 1,
    title: '기존 과제',
    content: '기존 내용',
    submissionMethod: '링크 제출',
    closeAt: '2099-12-31T23:59:00',
    submissionTarget: 'MEMBERS_ONLY',
    submissionVisibility: 'LEADER_ONLY',
    completeUserIds: [],
    attachmentFileIds: ids,
  });
}

beforeEach(async () => {
  await userTable.create({ id: 1, name: '리더', profileImage: null });
  login('리더');
});
afterEach(logout);

test('이미지를 업로드한 뒤 과제를 생성하고 상세에서 표시한다', async () => {
  const { user } = await setupPage('create');
  await user.type(screen.getByRole('textbox', { name: '제목' }), '이미지 과제');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '예시를 확인해주세요');
  await user.type(screen.getByRole('textbox', { name: '제출 방법' }), '링크 제출');
  await user.click(screen.getByRole('button', { name: '마감 시각' }));
  const dialog = screen.getByRole('dialog');
  await user.click(within(dialog).getByRole('button', { name: '다음 달' }));
  const nextMonth = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1);
  await user.click(
    within(dialog).getByRole('button', {
      name: `${nextMonth.getFullYear()}년 ${nextMonth.getMonth() + 1}월 1일`,
    }),
  );
  await user.click(within(dialog).getByRole('button', { name: '다음' }));
  await user.click(within(dialog).getByRole('button', { name: '완료' }));
  await user.upload(screen.getByLabelText('이미지 업로드'), image());
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  await user.click(screen.getByRole('button', { name: '과제 올리기' }));
  const article = await screen.findByRole('article', { name: '과제 상세' });
  expect(within(article).getByRole('img', { name: '자료.png' })).toHaveAttribute(
    'src',
    expect.stringContaining(`${MOCK_UPLOAD_ORIGIN}/images/`),
  );
  expect(within(article).getByText('예시를 확인해주세요')).toBeVisible();
});

test('완료 API가 끝나기 전에는 과제를 저장할 수 없다', async () => {
  let release!: () => void;
  const pending = new Promise<void>((resolve) => {
    release = resolve;
  });
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, async () => {
      await pending;
    }),
  );
  const { user } = await setupPage('create');
  try {
    await user.upload(screen.getByLabelText('이미지 업로드'), image());
    expect(await screen.findByText('업로드 중...')).toBeVisible();
    expect(screen.getByRole('button', { name: '과제 올리기' })).toBeDisabled();
  } finally {
    release();
  }
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  expect(screen.getByRole('button', { name: '과제 올리기' })).toBeEnabled();
});

test('내용만 수정하면 기존 이미지를 재업로드하지 않고 유지한다', async () => {
  await existingAssignment([101]);
  const { user } = await setupPage('edit');
  expect(await screen.findByText('기존-101.png')).toBeVisible();
  await user.type(screen.getByRole('textbox', { name: '내용' }), ' 추가');
  await user.click(screen.getByRole('button', { name: '과제 수정하기' }));
  const article = await screen.findByRole('article', { name: '과제 상세' });
  expect(within(article).getByRole('img', { name: '기존-101.png' })).toBeVisible();
  expect(within(article).getByText('기존 내용 추가')).toBeVisible();
  expect(fileTable.findMany()).toHaveLength(1);
});

test('기존 이미지를 일부 삭제하고 새 이미지를 뒤에 추가한다', async () => {
  await existingAssignment([101, 102]);
  const { user } = await setupPage('edit');
  await user.click(await screen.findByRole('button', { name: '기존-101.png 삭제' }));
  await user.upload(screen.getByLabelText('이미지 업로드'), image('신규.png'));
  await waitFor(() => expect(screen.getAllByText('업로드 완료')).toHaveLength(2));
  await user.click(screen.getByRole('button', { name: '과제 수정하기' }));
  const gallery = await screen.findByRole('region', { name: '첨부 이미지' });
  expect(
    within(gallery)
      .getAllByRole('img')
      .map((img) => img.getAttribute('alt')),
  ).toEqual(['기존-102.png', '신규.png']);
});

test('기존 이미지를 전부 삭제한 상태로 저장한다', async () => {
  await existingAssignment([101]);
  const { user } = await setupPage('edit');
  await user.click(await screen.findByRole('button', { name: '기존-101.png 삭제' }));
  await user.click(screen.getByRole('button', { name: '과제 수정하기' }));
  await screen.findByRole('article', { name: '과제 상세' });
  expect(screen.queryByRole('region', { name: '첨부 이미지' })).not.toBeInTheDocument();
  expect(assignmentTable.findFirst((q) => q.where({ id: 10 }))?.attachmentFileIds).toEqual([]);
});

test('기존 이미지와 새 이미지를 합쳐 10장을 초과할 수 없다', async () => {
  await existingAssignment([101, 102, 103, 104, 105, 106, 107, 108, 109]);
  const { user } = await setupPage('edit');
  await screen.findByRole('textbox', { name: '제목' });
  await user.upload(screen.getByLabelText('이미지 업로드'), [image('새1.png'), image('새2.png')]);
  expect(await screen.findByRole('alert')).toHaveTextContent('10장');
  expect(fileTable.findMany()).toHaveLength(9);
});

test('업로드 실패 시 입력을 유지하고 실패 파일을 삭제하면 저장할 수 있다', async () => {
  await existingAssignment([]);
  server.use(
    http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, () => new HttpResponse(null, { status: 403 })),
  );
  const { user } = await setupPage('edit');
  await screen.findByRole('textbox', { name: '제목' });
  await user.upload(screen.getByLabelText('이미지 업로드'), image());
  expect(await screen.findByRole('alert')).toHaveTextContent('업로드');
  expect(screen.getByRole('textbox', { name: '내용' })).toHaveValue('기존 내용');
  expect(screen.getByRole('button', { name: '과제 수정하기' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: '자료.png 삭제' }));
  await user.click(screen.getByRole('button', { name: '과제 수정하기' }));
  expect(await screen.findByRole('article', { name: '과제 상세' })).toBeVisible();
});

test('이미지 실패 후 다시 불러오면 과제 상세를 재조회한다', async () => {
  await existingAssignment([101]);
  const { user } = await setupPage('edit');
  await user.click(await screen.findByRole('button', { name: '과제 수정하기' }));
  const picture = await screen.findByRole('img', { name: '기존-101.png' });
  // jsdom에서 실제 이미지 로딩은 실행되지 않아 실패 이벤트를 직접 발생시킨다.
  fireEvent.error(picture);
  const file = fileTable.findFirst((q) => q.where({ id: 101 }))!;
  await fileTable.update(file, {
    data(record) {
      record.originalFileName = '갱신.png';
    },
  });
  await user.click(screen.getByRole('button', { name: '이미지 다시 불러오기' }));
  expect(await screen.findByRole('img', { name: '갱신.png' })).toBeVisible();
});

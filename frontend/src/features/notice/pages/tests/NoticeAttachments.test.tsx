import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { invalidInputResponse } from '../../../../mocks/errors';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../../shared/modules/files/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { noticeTable } from '../../mocks/db';
import CreateNoticePage from '../CreateNoticePage';
import EditNoticePage from '../EditNoticePage';
import NoticeDetailPage from '../NoticeDetailPage';

function image(name = '자료.png', size = 4, type = 'image/png') {
  return new File([new Uint8Array(size)], name, { type });
}

function deferred() {
  let resolve!: () => void;
  const promise = new Promise<void>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

async function prepareStudy() {
  await studyTable.create({
    id: 1,
    name: '첨부 스터디',
    description: '이미지 자료',
    inviteLink: 'attachment',
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

async function setupCreatePage() {
  await prepareStudy();
  return setup(<CreateNoticePage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/notices/create'],
      routes: (element) => (
        <>
          <Route path="/studies/:studyId/notices/create" element={element} />
          <Route path="/studies/:studyId/notices/:noticeId" element={<NoticeDetailPage />} />
        </>
      ),
    }),
  });
}

async function setupEditPage() {
  await prepareStudy();
  return setup(<EditNoticePage />, {
    wrapper: createWrapper({
      initialEntries: ['/studies/1/notices/10/edit'],
      routes: (element) => (
        <>
          <Route path="/studies/:studyId/notices/:noticeId/edit" element={element} />
          <Route path="/studies/:studyId/notices/:noticeId" element={<NoticeDetailPage />} />
        </>
      ),
    }),
  });
}

async function existingNotice(ids: number[]) {
  for (const id of ids) {
    await fileTable.create({
      id,
      ownerUserId: 1,
      originalFileName: `기존-${id}.png`,
      contentType: 'image/png',
      size: 4,
      uploadedSize: 4,
      status: 'COMPLETED',
    });
  }
  await noticeTable.create({
    id: 10,
    studyId: 1,
    title: '기존 공지',
    content: '기존 내용',
    createdAt: '2026-10-22T10:00:00',
    attachmentFileIds: ids,
  });
}

beforeEach(async () => {
  await userTable.create({ id: 1, name: '리더', profileImage: null });
  login('리더');
});
afterEach(logout);

test('완료한 이미지를 첨부해 공지를 생성한다', async () => {
  const { user } = await setupCreatePage();
  await user.type(screen.getByRole('textbox', { name: '제목' }), '이미지 공지');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '자료를 확인해주세요');
  await user.upload(screen.getByLabelText('이미지 업로드'), image());
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(within(article).getByText('자료.png')).toBeVisible();
  expect(within(article).getByText('자료를 확인해주세요')).toBeVisible();
});

test('업로드 완료 확인 전에는 공지를 저장할 수 없다', async () => {
  const gate = deferred();
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, async () => {
      await gate.promise;
    }),
  );
  const { user } = await setupCreatePage();
  try {
    await user.upload(screen.getByLabelText('이미지 업로드'), image());
    expect(await screen.findByText('업로드 중...')).toBeVisible();
    expect(screen.getByRole('button', { name: '공지 올리기' })).toBeDisabled();
  } finally {
    gate.resolve();
  }
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeEnabled();
});

test('나중에 선택한 이미지가 먼저 업로드되어도 선택 순서로 저장한다', async () => {
  const gate = deferred();
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, async ({ params }) => {
      const file = fileTable.findFirst((q) => q.where({ id: Number(params.fileId) }));
      if (file?.originalFileName === '첫째.png') await gate.promise;
    }),
  );
  const { user } = await setupCreatePage();
  await user.type(screen.getByRole('textbox', { name: '제목' }), '순서 공지');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '순서를 확인해주세요');
  try {
    await user.upload(screen.getByLabelText('이미지 업로드'), image('첫째.png'));
    expect(await screen.findByText('업로드 중...')).toBeVisible();
    await user.upload(screen.getByLabelText('이미지 업로드'), image('둘째.png'));
    expect(await screen.findByText('업로드 완료')).toBeVisible();
    expect(screen.getByRole('button', { name: '공지 올리기' })).toBeDisabled();
  } finally {
    gate.resolve();
  }
  await waitFor(() => expect(screen.getAllByText('업로드 완료')).toHaveLength(2));
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(
    within(article)
      .getAllByRole('listitem')
      .map((item) => item.textContent),
  ).toEqual(['첫째.png', '둘째.png']);
});

test('업로드 중 삭제한 이미지는 늦게 완료되어도 다시 나타나지 않는다', async () => {
  const gate = deferred();
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, async () => {
      await gate.promise;
    }),
  );
  const { user } = await setupCreatePage();
  try {
    await user.upload(screen.getByLabelText('이미지 업로드'), image());
    await waitFor(() =>
      expect(fileTable.findMany().some((file) => file.uploadedSize !== null)).toBe(true),
    );
    await user.click(screen.getByRole('button', { name: '자료.png 삭제' }));
  } finally {
    gate.resolve();
  }
  await waitFor(() => expect(fileTable.findMany()[0].status).toBe('COMPLETED'));
  expect(screen.queryByText('자료.png')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeEnabled();
});

test('S3 업로드 실패를 안내하고 완료 처리하지 않는다', async () => {
  server.use(
    http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, () => new HttpResponse(null, { status: 403 })),
  );
  const { user } = await setupCreatePage();
  await user.upload(screen.getByLabelText('이미지 업로드'), image());
  expect(await screen.findByRole('alert')).toHaveTextContent('업로드');
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeDisabled();
  expect(fileTable.findMany()[0].status).toBe('PENDING');
});

test('일부 업로드가 실패해도 입력 내용과 성공한 이미지를 유지한다', async () => {
  server.use(
    http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, ({ params }) => {
      const file = fileTable.findFirst((q) => q.where({ id: Number(params.fileId) }));
      if (file?.originalFileName === '실패.png') return new HttpResponse(null, { status: 403 });
    }),
  );
  const { user } = await setupCreatePage();
  await user.type(screen.getByRole('textbox', { name: '제목' }), '부분 실패 공지');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '유지할 내용');
  await user.upload(screen.getByLabelText('이미지 업로드'), [image('성공.png'), image('실패.png')]);
  expect(await screen.findByRole('alert')).toHaveTextContent('업로드');
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  expect(screen.getByRole('textbox', { name: '제목' })).toHaveValue('부분 실패 공지');
  expect(screen.getByRole('textbox', { name: '내용' })).toHaveValue('유지할 내용');
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: '실패.png 삭제' }));
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(within(article).getByText('성공.png')).toBeVisible();
  expect(within(article).getByText('유지할 내용')).toBeVisible();
  expect(within(article).queryByText('실패.png')).not.toBeInTheDocument();
});

test('완료 확인에 실패한 파일은 삭제 후 같은 파일을 다시 선택할 수 있다', async () => {
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, () =>
      HttpResponse.json(
        { code: 'FILE_SIZE_MISMATCH', message: '파일 크기가 일치하지 않습니다.' },
        { status: 400 },
      ),
    ),
  );
  const { user } = await setupCreatePage();
  const selected = image();
  await user.upload(screen.getByLabelText('이미지 업로드'), selected);
  expect(await screen.findByRole('alert')).toHaveTextContent('파일 크기가 일치하지 않습니다.');
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: '자료.png 삭제' }));
  server.resetHandlers();
  await user.upload(screen.getByLabelText('이미지 업로드'), selected);
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  expect(screen.getByRole('button', { name: '공지 올리기' })).toBeEnabled();
});

test.each([
  ['움직임.gif', 'image/gif', 4],
  ['그림.svg', 'image/svg+xml', 4],
  ['큼.png', 'image/png', 5_242_881],
  ['빈.png', 'image/png', 0],
  ['위장.png', 'application/pdf', 4],
  ['payload.constructor', '', 4],
])('허용하지 않는 이미지 %s는 업로드하지 않고 이유를 안내한다', async (name, type, size) => {
  await setupCreatePage();
  const user = userEvent.setup({ applyAccept: false });
  await user.upload(screen.getByLabelText('이미지 업로드'), image(name, size, type));
  expect(await screen.findByRole('alert')).toBeVisible();
  expect(fileTable.findMany()).toHaveLength(0);
});

test('5MiB 이미지를 첨부할 수 있다', async () => {
  const { user } = await setupCreatePage();
  await user.upload(screen.getByLabelText('이미지 업로드'), image('최대.png', 5_242_880));
  expect(await screen.findByText('업로드 완료')).toBeVisible();
});

test('MIME이 비어 있는 대문자 JPEG 확장자 이미지를 첨부할 수 있다', async () => {
  await setupCreatePage();
  const user = userEvent.setup({ applyAccept: false });
  await user.upload(screen.getByLabelText('이미지 업로드'), image('사진.JPEG', 4, ''));
  expect(await screen.findByText('업로드 완료')).toBeVisible();
  expect(fileTable.findMany()[0].contentType).toBe('image/jpeg');
});

test('10장을 선택한 뒤 추가 선택하면 기존 이미지를 유지하고 개수 제한을 안내한다', async () => {
  const { user } = await setupCreatePage();
  await user.upload(
    screen.getByLabelText('이미지 업로드'),
    Array.from({ length: 10 }, (_, i) => image(`${i}.png`)),
  );
  await waitFor(() => expect(screen.getAllByText('업로드 완료')).toHaveLength(10));
  await user.upload(screen.getByLabelText('이미지 업로드'), image('열한째.png'));
  expect(await screen.findByRole('alert')).toHaveTextContent('10장');
  expect(screen.queryByText('열한째.png')).not.toBeInTheDocument();
  expect(fileTable.findMany()).toHaveLength(10);
});

test('공지 저장 실패 후 입력과 완료된 첨부를 유지해 다시 저장한다', async () => {
  server.use(
    http.post(`${API_URL}/studies/:studyId/notices`, () =>
      HttpResponse.json(
        { code: 'TEMPORARY_ERROR', message: '잠시 후 다시 저장해주세요.' },
        { status: 400 },
      ),
    ),
  );
  const { user } = await setupCreatePage();
  await user.type(screen.getByRole('textbox', { name: '제목' }), '재시도 공지');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '보존할 내용');
  await user.upload(screen.getByLabelText('이미지 업로드'), image());
  await screen.findByText('업로드 완료');
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  expect(await screen.findByRole('status')).toHaveTextContent('잠시 후 다시 저장해주세요.');
  expect(screen.getByRole('textbox', { name: '내용' })).toHaveValue('보존할 내용');
  expect(screen.getByText('자료.png')).toBeVisible();
  server.resetHandlers();
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  expect(
    within(await screen.findByRole('article', { name: '공지 상세' })).getByText('자료.png'),
  ).toBeVisible();
  expect(fileTable.findMany()).toHaveLength(1);
});

test('서버 첨부 검증 오류를 이미지 필드에 표시한다', async () => {
  server.use(
    http.post(`${API_URL}/studies/:studyId/notices`, () =>
      invalidInputResponse([
        { field: 'attachmentFileIds', code: 'INVALID', reason: '첨부한 파일을 확인해주세요.' },
      ]),
    ),
  );
  const { user } = await setupCreatePage();
  await user.type(screen.getByRole('textbox', { name: '제목' }), '공지');
  await user.type(screen.getByRole('textbox', { name: '내용' }), '내용');
  await user.click(screen.getByRole('button', { name: '공지 올리기' }));
  expect(
    await within(screen.getByTestId('notice-image-field')).findByText(
      '첨부한 파일을 확인해주세요.',
    ),
  ).toBeVisible();
});

test('본문만 수정하면 기존 이미지를 재업로드하지 않고 유지한다', async () => {
  await existingNotice([101]);
  const { user } = await setupEditPage();
  expect(await screen.findByText('기존-101.png')).toBeVisible();
  await user.type(screen.getByRole('textbox', { name: '내용' }), ' 추가 내용');
  await user.click(screen.getByRole('button', { name: '수정하기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(within(article).getByText('기존-101.png')).toBeVisible();
  expect(within(article).getByText('기존 내용 추가 내용')).toBeVisible();
  expect(fileTable.findMany()).toHaveLength(1);
});

test('기존 이미지 일부를 삭제하고 새 이미지를 뒤에 추가한다', async () => {
  await existingNotice([101, 102]);
  const { user } = await setupEditPage();
  await user.click(await screen.findByRole('button', { name: '기존-101.png 삭제' }));
  await user.upload(screen.getByLabelText('이미지 업로드'), image('신규.png'));
  await waitFor(() => expect(screen.getAllByText('업로드 완료')).toHaveLength(2));
  await user.click(screen.getByRole('button', { name: '수정하기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(
    within(article)
      .getAllByRole('listitem')
      .map((item) => item.textContent),
  ).toEqual(['기존-102.png', '신규.png']);
});

test('기존 이미지를 모두 삭제한 상태를 저장한다', async () => {
  await existingNotice([101]);
  const { user } = await setupEditPage();
  await user.click(await screen.findByRole('button', { name: '기존-101.png 삭제' }));
  await user.click(screen.getByRole('button', { name: '수정하기' }));
  const article = await screen.findByRole('article', { name: '공지 상세' });
  expect(within(article).queryByText('기존-101.png')).not.toBeInTheDocument();
  expect(noticeTable.findFirst((q) => q.where({ id: 10 }))?.attachmentFileIds).toEqual([]);
});

test('기존 이미지 9장이 있으면 새 이미지 2장 추가를 거부한다', async () => {
  await existingNotice([101, 102, 103, 104, 105, 106, 107, 108, 109]);
  const { user } = await setupEditPage();
  await screen.findByRole('textbox', { name: '제목' });
  await user.upload(screen.getByLabelText('이미지 업로드'), [image('새1.png'), image('새2.png')]);
  expect(await screen.findByRole('alert')).toHaveTextContent('10장');
  expect(screen.getByText('기존-109.png')).toBeVisible();
  expect(screen.queryByText('새1.png')).not.toBeInTheDocument();
  expect(fileTable.findMany()).toHaveLength(9);
});

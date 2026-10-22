import { QueryClient } from '@tanstack/react-query';
import { screen, within, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route } from 'react-router';
import { API_URL } from '../../../../../config';
import { server } from '../../../../mocks/msw-node';
import { createWrapper, login, logout, setup } from '../../../../test/render';
import { fileTable, MOCK_UPLOAD_ORIGIN } from '../../../../shared/modules/files/mocks/db';
import { userTable } from '../../../user/mocks/db';
import { studyTable } from '../../../study/mocks/db';
import { memberTable } from '../../../member/mocks/db';
import { assignmentTable, submissionTable } from '../../mocks/db';
import AssignmentDetailPage from '../AssignmentDetailPage';
import AssignmentSubmissionDetailPage from '../AssignmentSubmissionDetailPage';

const document = (name = '답안.pdf', size = 5, type = 'application/pdf') =>
  new File([new Uint8Array(size)], name, { type });

async function prepareSubmission(
  ids: number[] = [],
  submitted = false,
  content: string | null = null,
) {
  await studyTable.create({ id: 1, name: '파일 스터디', description: '과제', inviteLink: 'files' });
  await memberTable.create({
    id: 1,
    studyId: 1,
    userId: 1,
    name: '제출자',
    profileImage: null,
    role: 'MEMBER',
  });
  await assignmentTable.create({
    id: 1,
    studyId: 1,
    title: '파일 과제',
    content: '첨부해 주세요',
    submissionMethod: '파일 제출',
    closeAt: '2099-12-31T23:59:00',
    submissionTarget: 'MEMBERS_AND_LEADER',
    submissionVisibility: 'ALL_STUDY_MEMBERS',
    completeUserIds: submitted ? [1] : [],
  });
  for (const id of ids) {
    await fileTable.create({
      id,
      ownerUserId: 1,
      originalFileName: `기존-${id}.pdf`,
      contentType: 'application/pdf',
      size: 5,
      uploadedSize: 5,
      status: 'COMPLETED',
    });
  }
  await submissionTable.create({
    id: 1,
    assignmentId: 1,
    userId: 1,
    submitted,
    content,
    link: null,
    createdAt: submitted ? '2026-01-01T09:00:00' : null,
    attachmentFileIds: ids,
  });
}

function setupPage(detail = false, queryClient?: QueryClient) {
  return setup(detail ? <AssignmentSubmissionDetailPage /> : <AssignmentDetailPage />, {
    wrapper: createWrapper({
      queryClient,
      initialEntries: [
        detail ? '/studies/1/assignments/1/submissions/1' : '/studies/1/assignments/1',
      ],
      routes: (element) => (
        <>
          <Route
            path={
              detail
                ? '/studies/:studyId/assignments/:assignmentId/submissions/:submissionId'
                : '/studies/:studyId/assignments/:assignmentId'
            }
            element={element}
          />
          <Route
            path={
              detail
                ? '/studies/:studyId/assignments/:assignmentId'
                : '/studies/:studyId/assignments/:assignmentId/submissions/:submissionId'
            }
            element={detail ? <AssignmentDetailPage /> : <AssignmentSubmissionDetailPage />}
          />
        </>
      ),
    }),
  });
}

beforeEach(async () => {
  await userTable.create({ id: 1, name: '제출자', profileImage: null });
  login('제출자');
});
afterEach(logout);

async function mySubmission() {
  return within(await screen.findByRole('region', { name: '내 제출' }));
}

test('파일만 첨부하여 제출하고 내 제출에서 파일명을 확인한다', async () => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.upload(form.getByLabelText('파일 업로드'), document());
  expect(await form.findByText('업로드 완료')).toBeVisible();
  await user.click(form.getByRole('button', { name: '제출하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  expect((await mySubmission()).getByText('답안.pdf')).toBeVisible();
});

test('브라우저가 MIME을 제공하지 않는 파일도 제출한다', async () => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.upload(form.getByLabelText('파일 업로드'), document('자료.custom', 5, ''));
  expect(await form.findByText('업로드 완료')).toBeVisible();
  await user.click(form.getByRole('button', { name: '제출하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  expect((await mySubmission()).getByText('자료.custom')).toBeVisible();
});

test('링크만 입력해도 제출할 수 있다', async () => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.type(form.getByRole('textbox', { name: '링크' }), 'https://example.com/answer');
  await user.click(form.getByRole('button', { name: '제출하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  expect(
    (await mySubmission()).getByRole('link', { name: 'https://example.com/answer' }),
  ).toBeVisible();
});

test('업로드 완료 확인을 기다리는 동안 제출할 수 없다', async () => {
  await prepareSubmission();
  let release!: () => void;
  const pending = new Promise<void>((resolve) => {
    release = resolve;
  });
  server.use(
    http.post(`${API_URL}/files/:fileId/complete`, async () => {
      await pending;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  const { user } = setupPage();
  const form = await mySubmission();
  try {
    await user.upload(form.getByLabelText('파일 업로드'), document());
    expect(await form.findByText('업로드 중...')).toBeVisible();
    expect(form.getByRole('button', { name: '제출하기' })).toBeDisabled();
  } finally {
    release();
  }
  expect(await form.findByText('업로드 완료')).toBeVisible();
});

test('파일 10개까지 첨부할 수 있다', async () => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.upload(
    form.getByLabelText('파일 업로드'),
    Array.from({ length: 10 }, (_, i) => document(`답안-${i}.pdf`)),
  );
  await waitFor(() => expect(form.getAllByText('업로드 완료')).toHaveLength(10));
  await user.upload(form.getByLabelText('파일 업로드'), document('초과.pdf'));
  expect(await form.findByText('파일은 최대 10개까지 첨부할 수 있어요.')).toBeVisible();
  expect(form.queryByText('초과.pdf')).not.toBeInTheDocument();
});

test.each([0, 5_242_880])('크기가 %i바이트인 일반 파일은 첨부할 수 있다', async (size) => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.upload(form.getByLabelText('파일 업로드'), document('자료.txt', size, 'text/plain'));
  expect(await form.findByText('업로드 완료')).toBeVisible();
});

test('5MiB를 초과한 파일을 거부하고 입력 내용은 유지한다', async () => {
  await prepareSubmission();
  const { user } = setupPage();
  const form = await mySubmission();
  await user.type(form.getByRole('textbox', { name: '내용' }), '작성 중인 답안');
  await user.upload(form.getByLabelText('파일 업로드'), document('초과.pdf', 5_242_881));
  expect(await form.findByText('초과.pdf: 파일당 최대 5MB까지 첨부할 수 있어요.')).toBeVisible();
  expect(form.getByRole('textbox', { name: '내용' })).toHaveValue('작성 중인 답안');
});

test('기존 파일을 유지하고 삭제·추가한 순서로 수정한다', async () => {
  await prepareSubmission([101, 102], true);
  const { user } = setupPage();
  await user.click(await screen.findByRole('button', { name: '편집하기' }));
  const form = await mySubmission();
  await user.click(form.getByRole('button', { name: '기존-101.pdf 삭제' }));
  await user.upload(form.getByLabelText('파일 업로드'), document('신규.pdf'));
  await waitFor(() => expect(form.getAllByText('업로드 완료')).toHaveLength(2));
  await user.click(form.getByRole('button', { name: '수정하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  const files = within((await mySubmission()).getByRole('list', { name: '첨부 파일' }));
  expect(
    files.getAllByRole('listitem').map((item) => within(item).getByText(/\.pdf$/).textContent),
  ).toEqual(['기존-102.pdf', '신규.pdf']);
});

test('첨부를 모두 지워도 내용이 남으면 수정할 수 있다', async () => {
  await prepareSubmission([101], true, '남길 답안');
  const { user } = setupPage();
  await user.click(await screen.findByRole('button', { name: '편집하기' }));
  const form = await mySubmission();
  await user.click(form.getByRole('button', { name: '기존-101.pdf 삭제' }));
  await user.click(form.getByRole('button', { name: '수정하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  expect((await mySubmission()).queryByRole('list', { name: '첨부 파일' })).not.toBeInTheDocument();
  expect((await mySubmission()).getByText('남길 답안')).toBeVisible();
});

test('내용·링크·첨부를 모두 지운 제출은 저장할 수 없다', async () => {
  await prepareSubmission([101], true);
  const { user } = setupPage();
  await user.click(await screen.findByRole('button', { name: '편집하기' }));
  const form = await mySubmission();
  await user.click(form.getByRole('button', { name: '기존-101.pdf 삭제' }));
  await user.click(form.getByRole('button', { name: '수정하기' }));
  expect(await form.findByText('내용, 링크, 파일 중 하나 이상을 입력해주세요.')).toBeVisible();
  expect(form.getByRole('button', { name: '수정하기' })).toBeVisible();
});

test('업로드 실패한 파일을 제거하면 작성 중인 내용으로 제출할 수 있다', async () => {
  await prepareSubmission();
  server.use(
    http.put(`${MOCK_UPLOAD_ORIGIN}/files/:fileId`, () => new HttpResponse(null, { status: 403 })),
  );
  const { user } = setupPage();
  const form = await mySubmission();
  await user.type(form.getByRole('textbox', { name: '내용' }), '유지할 답안');
  await user.upload(form.getByLabelText('파일 업로드'), document());
  expect(
    await form.findByText('파일 업로드에 실패했어요. 삭제 후 다시 선택해주세요.'),
  ).toBeVisible();
  expect(form.getByRole('button', { name: '제출하기' })).toBeDisabled();
  await user.click(form.getByRole('button', { name: '답안.pdf 삭제' }));
  await user.click(form.getByRole('button', { name: '제출하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  expect((await mySubmission()).getByText('유지할 답안')).toBeVisible();
});

test('본문이 null인 제출 상세에서도 파일명을 확인한다', async () => {
  await prepareSubmission([101], true);
  setupPage(true);
  const article = within(await screen.findByRole('article', { name: '제출자' }));
  expect(article.getByRole('list', { name: '첨부 파일' })).toHaveTextContent('기존-101.pdf');
});

test('수정 후 다시 방문한 제출 상세에도 최신 첨부 목록을 표시한다', async () => {
  await prepareSubmission([101], true, '답안');
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  const { user } = setupPage(true, queryClient);
  expect(await screen.findByText('기존-101.pdf')).toBeVisible();
  await user.click(screen.getByRole('button', { name: '뒤로 가기' }));
  await user.click(await screen.findByRole('button', { name: '편집하기' }));
  const form = await mySubmission();
  await user.click(form.getByRole('button', { name: '기존-101.pdf 삭제' }));
  await user.upload(form.getByLabelText('파일 업로드'), document('교체.pdf'));
  expect(await form.findByText('업로드 완료')).toBeVisible();
  await user.click(form.getByRole('button', { name: '수정하기' }));
  expect(await screen.findByRole('button', { name: '편집하기' })).toBeVisible();
  await user.click(screen.getByRole('tab', { name: '현황' }));
  await user.click(await screen.findByRole('link', { name: '제출자 제출 내역 상세 보기' }));
  const article = within(await screen.findByRole('article', { name: '제출자' }));
  expect(await article.findByText('교체.pdf')).toBeVisible();
  expect(article.queryByText('기존-101.pdf')).not.toBeInTheDocument();
});

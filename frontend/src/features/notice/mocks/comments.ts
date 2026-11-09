import { http, HttpResponse, type PathParams } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable, mockMembers } from '../../member/mocks/db';
import { noticeTable, mockNotices } from './db';
import { commentTable } from './commentDb';
import { assignmentTable, submissionTable } from '../../assignment/mocks/db';
import { commentSchema } from '../../../shared/modules/comments/responseSchemas';

const bases = [
  `${API_URL}/studies/:studyId/notices/:noticeId/comments`,
  `${API_URL}/studies/:studyId/assignments/:assignmentId/comments`,
  `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId/comments`,
];
let nextCommentId = 1;

function context(request: Request, params: PathParams) {
  const studyId = Number(params.studyId);
  const scope = {
    studyId,
    noticeId: params.noticeId ? Number(params.noticeId) : null,
    assignmentId: params.assignmentId ? Number(params.assignmentId) : null,
    submissionId: params.submissionId ? Number(params.submissionId) : null,
  };
  const { noticeId, assignmentId, submissionId } = scope;
  const user = findUserFromHeader(request.headers);
  if (!user) return new HttpResponse(null, { status: 401 });
  const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
  if (!member)
    return HttpResponse.json(
      { code: 'STUDY_ACCESS_DENIED', message: '해당 스터디에 대한 접근 권한이 없습니다.' },
      { status: 403 },
    );
  if (noticeId !== null && !noticeTable.findFirst((q) => q.where({ id: noticeId, studyId })))
    return HttpResponse.json(
      { code: 'NOTICE_NOT_FOUND', message: '공지를 찾을 수 없습니다.' },
      { status: 404 },
    );
  if (assignmentId !== null) {
    const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
    if (!assignment) return new HttpResponse(null, { status: 404 });
    if (submissionId !== null) {
      const submission = submissionTable.findFirst((q) =>
        q.where({ id: submissionId, assignmentId: assignment.id }),
      );
      if (!submission) return new HttpResponse(null, { status: 404 });
      if (
        assignment.submissionVisibility !== 'ALL_STUDY_MEMBERS' &&
        member.role !== 'LEADER' &&
        submission.userId !== user.id
      )
        return denied();
      if (!submission.submitted)
        return HttpResponse.json(
          { code: 'COMMENT_TARGET_NOT_READY', message: '제출 후 댓글을 남길 수 있어요' },
          { status: 409 },
        );
    }
  }
  return { user, member, scope };
}
function notFound() {
  return HttpResponse.json(
    { code: 'COMMENT_NOT_FOUND', message: '해당 원문의 댓글을 찾을 수 없습니다.' },
    { status: 404 },
  );
}
function denied() {
  return HttpResponse.json(
    { code: 'ACCESS_DENIED', message: '요청한 작업을 수행할 권한이 없습니다.' },
    { status: 403 },
  );
}
async function readContent(request: Request) {
  const body: unknown = await request.json();
  const content =
    typeof body === 'object' &&
    body !== null &&
    'content' in body &&
    typeof body.content === 'string'
      ? body.content.trim()
      : '';
  if (!content || content.length > 1000) {
    return HttpResponse.json(
      {
        code: 'COMMENT_INVALID_CONTENT',
        message: '댓글은 앞뒤 공백을 제거한 뒤 1자 이상 1000자 이하여야 합니다.',
      },
      { status: 400 },
    );
  }
  return content;
}

export const handlers = bases.flatMap((base) => [
  http.get(base, ({ params, request }) => {
    const ctx = context(request, params);
    if (ctx instanceof Response) return ctx;
    const { user, member, scope } = ctx;
    const search = new URL(request.url).searchParams;
    const cursor = Number(search.get('cursor') ?? 0);
    const size = Number(search.get('size') ?? 10);
    const all = commentTable.findMany((q) => q.where(scope)).sort((a, b) => a.id - b.id);
    const remaining = all.filter((comment) => comment.id > cursor);
    const comments = remaining.slice(0, size).map((comment) =>
      commentSchema.parse({
        ...comment,
        isMine: comment.authorUserId === user.id,
        canEdit: comment.authorUserId === user.id,
        canDelete: comment.authorUserId === user.id || member.role === 'LEADER',
      }),
    );
    const hasNext = remaining.length > size;
    return HttpResponse.json({
      comments,
      totalCount: all.length,
      nextCursor: hasNext ? comments.at(-1)?.id : null,
      hasNext,
    });
  }),
  http.post(base, async ({ params, request }) => {
    const ctx = context(request, params);
    if (ctx instanceof Response) return ctx;
    const content = await readContent(request);
    if (content instanceof Response) return content;
    const { scope, user, member } = ctx;
    const maxId = Math.max(0, ...commentTable.findMany().map((comment) => comment.id));
    const id = Math.max(maxId + 1, nextCommentId++);
    nextCommentId = id + 1;
    await commentTable.create({
      id,
      ...scope,
      authorUserId: user.id,
      author: {
        name: member.name,
        profileImageUrl: user.profileImage,
        role: member.role,
        isDeparted: false,
      },
      content,
      createdAt: new Date().toISOString(),
      isEdited: false,
    });
    return HttpResponse.json({ commentId: id }, { status: 201 });
  }),
  http.patch(`${base}/:commentId`, async ({ params, request }) => {
    const ctx = context(request, params);
    if (ctx instanceof Response) return ctx;
    const comment = commentTable.findFirst((q) =>
      q.where({ id: Number(params.commentId), ...ctx.scope }),
    );
    if (!comment) return notFound();
    if (comment.authorUserId !== ctx.user.id) return denied();
    const content = await readContent(request);
    if (content instanceof Response) return content;
    commentTable.delete((q) => q.where({ id: comment.id }));
    await commentTable.create({
      ...comment,
      content,
      isEdited: comment.isEdited || content !== comment.content,
    });
    return new HttpResponse(null, { status: 204 });
  }),
  http.delete(`${base}/:commentId`, ({ params, request }) => {
    const ctx = context(request, params);
    if (ctx instanceof Response) return ctx;
    const comment = commentTable.findFirst((q) =>
      q.where({ id: Number(params.commentId), ...ctx.scope }),
    );
    if (!comment) return notFound();
    if (comment.authorUserId !== ctx.user.id && ctx.member.role !== 'LEADER') return denied();
    commentTable.delete((q) => q.where({ id: comment.id }));
    return new HttpResponse(null, { status: 204 });
  }),
]);

export async function createSeedComments() {
  const notices = mockNotices;
  let id = 1;
  for (const notice of notices) {
    const members = mockMembers.filter((member) => member.studyId === notice.studyId);
    if (members.length === 0) continue;
    for (let index = 0; index < 12; index++) {
      const author = members[index % members.length];
      await commentTable.create({
        id: id++,
        studyId: notice.studyId,
        noticeId: notice.id,
        authorUserId: author.userId,
        author: { name: author.name, profileImageUrl: null, role: author.role, isDeparted: false },
        content:
          index === 0
            ? '문맥 전환 비용도 비교해보면 좋겠어요.'
            : index === 1
              ? '피드백 반영했습니다!'
              : `함께 확인할 내용 ${index + 1}입니다.\n다음 모임에서 이야기해요.`,
        createdAt: `2026-10-30T13:${String(10 + index).padStart(2, '0')}:00`,
        isEdited: index === 1,
      });
    }
  }
}

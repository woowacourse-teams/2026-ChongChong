import { MOCK_UPLOAD_ORIGIN } from '../../../shared/modules/files/mocks/db';
import type { StudyFormValues, StudyJoinTarget } from '../types';
import { delay, http, HttpResponse } from 'msw';
import { studyTable } from './db';
import { API_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';
import { validateStudy, validateStudyJoin } from './validators';
import { invalidInputResponse } from '../../../mocks/errors';
import { assignmentTable, submissionTable } from '../../assignment/mocks/db';
import { noticeRecipientTable, noticeTable } from '../../notice/mocks/db';

function getIncompleteLeaderNotices(studyId: number) {
  return noticeTable
    .findMany((query) => query.where({ studyId }))
    .flatMap((notice) => {
      const recipients = noticeRecipientTable.findMany((query) =>
        query.where({ noticeId: notice.id }),
      );
      const completeCount = recipients.filter(({ readAt }) => readAt !== null).length;

      return completeCount < recipients.length
        ? [
            {
              id: notice.id,
              title: notice.title,
              memberCount: recipients.length,
              completeCount,
            },
          ]
        : [];
    });
}

function getUnreadMemberNotices(studyId: number, memberId: number) {
  return noticeTable
    .findMany((query) => query.where({ studyId }))
    .filter((notice) => {
      const recipient = noticeRecipientTable.findFirst((query) =>
        query.where({ noticeId: notice.id, memberId }),
      );
      return recipient?.readAt === null;
    });
}

function getOpenAssignments(studyId: number) {
  const now = new Date();

  return assignmentTable
    .findMany((query) => query.where({ studyId }))
    .filter((assignment) => new Date(assignment.closeAt) > now);
}

function getIncompleteLeaderAssignments(studyId: number) {
  return getOpenAssignments(studyId).flatMap((assignment) => {
    const submissions = submissionTable.findMany((query) =>
      query.where({ assignmentId: assignment.id }),
    );
    const completeCount = submissions.filter(({ submitted }) => submitted).length;

    return completeCount < submissions.length
      ? [{ assignment, memberCount: submissions.length, completeCount }]
      : [];
  });
}

function getIncompleteMemberAssignments(studyId: number, userId: number) {
  return getOpenAssignments(studyId).filter((assignment) => {
    const submission = submissionTable.findFirst((query) =>
      query.where({ assignmentId: assignment.id, userId }),
    );

    return submission?.submitted === false;
  });
}

function resolveJoinStudy({ token, studyId }: { token?: string; studyId?: number }) {
  if (token === undefined && (!Number.isSafeInteger(studyId) || (studyId ?? 0) <= 0)) {
    return HttpResponse.json(
      { code: 'INVALID_STUDY_ID', message: '유효한 스터디 ID가 필요합니다.' },
      { status: 400 },
    );
  }
  const study =
    token !== undefined
      ? studyTable.findFirst((q) => q.where({ inviteLink: token }))
      : studyTable.findFirst((q) => q.where({ id: studyId }));

  if (
    token !== undefined &&
    (!token.trim() || !study || (studyId !== undefined && study.id !== studyId))
  ) {
    return HttpResponse.json(
      { code: 'INVALID_INVITE_TOKEN', message: '초대 코드가 올바르지 않거나 만료되었습니다.' },
      { status: 400 },
    );
  }
  if (!study) {
    return HttpResponse.json(
      { code: 'STUDY_NOT_FOUND', message: '존재하지 않는 스터디입니다.' },
      { status: 404 },
    );
  }
  if (token === undefined && !study.isPublic) {
    return HttpResponse.json(
      { code: 'STUDY_NOT_PUBLIC', message: '공개 스터디가 아닙니다.' },
      { status: 403 },
    );
  }
  return study;
}

function toStudySummary(study: {
  id: number;
  name: string;
  description: string | null;
  imageFileId?: number | null;
  imageUrl?: string | null;
}) {
  return {
    studyId: study.id,
    studyName: study.name,
    description: study.description,
    imageUrl: study.imageFileId
      ? `${MOCK_UPLOAD_ORIGIN}/images/${study.imageFileId}`
      : (study.imageUrl ?? null),
    memberCount: memberTable.findMany((q) => q.where({ studyId: study.id })).length,
  };
}

export const handlers = [
  http.get(`${API_URL}${STUDY_URLS.list}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const memberships = await memberTable.findMany((q) => q.where({ userId: user.id }));
    const studies = await Promise.all(
      memberships.map(async (membership) => {
        const study = await studyTable.findFirst((q) => q.where({ id: membership.studyId }));
        if (!study) return null;
        const members = await memberTable.findMany((q) =>
          q.where({ studyId: study.id, userId: user.id }),
        );
        const noticeCount =
          membership.role === 'LEADER'
            ? getIncompleteLeaderNotices(study.id).length
            : getUnreadMemberNotices(study.id, membership.id).length;
        const assignmentCount =
          membership.role === 'LEADER'
            ? getIncompleteLeaderAssignments(study.id).length
            : getIncompleteMemberAssignments(study.id, user.id).length;
        return {
          id: study.id,
          role: membership.role,
          name: study.name,
          description: study.description,
          imageUrl: study.imageFileId
            ? `${MOCK_UPLOAD_ORIGIN}/images/${study.imageFileId}`
            : (study.imageUrl ?? null),
          memberCount: members.length,
          noticeCount,
          assignmentCount,
        };
      }),
    );
    return HttpResponse.json({ studies: studies.filter((study) => study !== null) });
  }),

  http.get(`${API_URL}${STUDY_URLS.publicList}`, async ({ request }) => {
    if (!findUserFromHeader(request.headers)) return new HttpResponse(null, { status: 401 });
    const params = new URL(request.url).searchParams;
    const cursor = params.has('cursor') ? Number(params.get('cursor')) : undefined;
    const size = Number(params.get('size') ?? 10);
    const rawKeyword = params.get('keyword');
    const keyword = rawKeyword?.trim().toLowerCase();
    if (rawKeyword !== null && (!keyword || rawKeyword.length > 15)) {
      return HttpResponse.json(
        { code: 'INVALID_REQUEST_PARAMETER', message: '검색어는 1자 이상 15자 이하여야 합니다.' },
        { status: 400 },
      );
    }
    if (
      !Number.isInteger(size) ||
      size < 1 ||
      size > 100 ||
      (cursor !== undefined && (!Number.isInteger(cursor) || cursor <= 0))
    ) {
      return HttpResponse.json(
        { code: 'INVALID_REQUEST_PARAMETER', message: '잘못된 페이지 요청입니다.' },
        { status: 400 },
      );
    }
    if (cursor !== undefined && !studyTable.findFirst((q) => q.where({ id: cursor }))) {
      return HttpResponse.json(
        { code: 'STUDY_NOT_FOUND', message: '존재하지 않는 스터디입니다.' },
        { status: 404 },
      );
    }
    const studies = studyTable
      .findMany((q) => q.where({ isPublic: true }))
      .filter((study) => !keyword || study.name.toLowerCase().includes(keyword))
      .filter((study) => cursor === undefined || study.id < cursor)
      .sort((a, b) => b.id - a.id)
      .map(toStudySummary)
      .filter((study) => study.memberCount < 50);
    const page = studies.slice(0, size);
    const hasNext = studies.length > size;
    // 다음 페이지의 로딩 Spinner를 확인하기 위한 목 응답 지연입니다.
    if (cursor !== undefined) await delay(500);
    return HttpResponse.json({
      studies: page,
      hasNext,
      nextCursor: hasNext ? page[page.length - 1].studyId : null,
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.joinInfo}`, ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const params = new URL(request.url).searchParams;
    const study = resolveJoinStudy({
      token: params.get('token') ?? undefined,
      studyId: params.has('studyId') ? Number(params.get('studyId')) : undefined,
    });
    if (study instanceof HttpResponse) return study;
    return HttpResponse.json(toStudySummary(study));
  }),

  http.get(`${API_URL}${STUDY_URLS.detail}`, async ({ request, params }) => {
    const studyId = Number(params.studyId);
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (!member) return new HttpResponse(null, { status: 403 });
    const isLead = member.role === 'LEADER';

    if (isLead) {
      const notices = getIncompleteLeaderNotices(studyId);
      const assignments = getIncompleteLeaderAssignments(studyId);
      return HttpResponse.json({
        notices: {
          count: notices.length,
          items: notices,
        },
        assignments: {
          count: assignments.length,
          items: assignments.map(({ assignment, memberCount, completeCount }) => ({
            id: assignment.id,
            title: assignment.title,
            memberCount,
            completeCount,
          })),
        },
      });
    } else {
      const assignments = getIncompleteMemberAssignments(studyId, user.id);
      const notices = getUnreadMemberNotices(studyId, member.id).map(({ id, title }) => ({
        id,
        title,
      }));
      return HttpResponse.json({
        totalCount: notices.length + assignments.length,
        notices: { items: notices },
        assignments: {
          items: assignments.map((assignment) => ({
            id: assignment.id,
            title: assignment.title,
          })),
        },
      });
    }
  }),

  http.post(`${API_URL}${STUDY_URLS.create}`, async ({ request }) => {
    const body = (await request.json()) as StudyFormValues;
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const fieldErrors = validateStudy(body);
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    if (typeof body.isPublic !== 'boolean') {
      return invalidInputResponse([
        { field: 'isPublic', code: 'REQUIRED', reason: '스터디 공개 여부는 필수입니다.' },
      ]);
    }
    const studyId = Date.now();
    await studyTable.create({
      id: studyId,
      inviteLink: String(studyId),
      isPublic: body.isPublic ?? false,
      ...body,
    });
    await memberTable.create({
      id: Date.now(),
      studyId,
      userId: user.id,
      name: user.name,
      profileImage: user.profileImage,
      role: 'LEADER',
    });
    return HttpResponse.json({ studyId }, { status: 201 });
  }),

  http.patch(`${API_URL}${STUDY_URLS.detail}`, async ({ params, request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const body = (await request.json()) as StudyFormValues;

    const fieldErrors = validateStudy(body);
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    const studyId = Number(params.studyId);
    const study = studyTable.findFirst((q) => q.where({ id: studyId }));
    if (!study) return new HttpResponse(null, { status: 404 });

    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (member?.role !== 'LEADER') {
      return new HttpResponse(null, { status: 403 });
    }

    await studyTable.update(study, {
      data(study) {
        study.name = body.name;
        study.description = body.description;
        if (body.isPublic !== undefined) {
          study.isPublic = body.isPublic;
        }
        if (body.imageFileId !== undefined) {
          study.imageFileId = body.imageFileId;
          study.imageUrl = null;
        }
      },
    });

    return new HttpResponse(null, { status: 204 });
  }),

  http.post(`${API_URL}${STUDY_URLS.join}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const body = (await request.json()) as StudyJoinTarget;
    if (body.token !== undefined) {
      const fieldErrors = validateStudyJoin({ token: body.token });
      if (fieldErrors.length > 0) return invalidInputResponse(fieldErrors);
    }
    const study = resolveJoinStudy(body);
    if (study instanceof HttpResponse) return study;
    if (memberTable.findFirst((q) => q.where({ studyId: study.id, userId: user.id }))) {
      return HttpResponse.json(
        {
          code: 'ALREADY_JOINED_STUDY',
          message: '해당 스터디에 이미 가입되어 있습니다.',
        },
        { status: 409 },
      );
    }
    if (memberTable.findMany((q) => q.where({ studyId: study.id })).length >= 50) {
      return HttpResponse.json(
        { code: 'STUDY_MEMBER_LIMIT_EXCEEDED', message: '스터디 정원이 가득 찼습니다.' },
        { status: 409 },
      );
    }
    const newMember = {
      id: Date.now(),
      studyId: study.id,
      userId: user.id,
      name: user.name,
      profileImage: user.profileImage,
      role: 'MEMBER' as const,
    };
    await memberTable.create(newMember);

    return HttpResponse.json({
      studyId: study.id,
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.info}`, async ({ request, params }) => {
    const { studyId } = params;
    const study = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!study) return new HttpResponse(null, { status: 404 });
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const member = await memberTable.findFirst((q) =>
      q.where({ studyId: study.id, userId: user.id }),
    );
    if (!member) return new HttpResponse(null, { status: 404 });
    return HttpResponse.json({
      studyName: study.name,
      role: member.role,
      isPublic: study.isPublic ?? false,
      userName: member.name,
      description: study.description,
      imageUrl: study.imageFileId
        ? `${MOCK_UPLOAD_ORIGIN}/images/${study.imageFileId}`
        : (study.imageUrl ?? null),
    });
  }),

  http.get(`${API_URL}${STUDY_URLS.inviteLink}`, async ({ params }) => {
    const { studyId } = params;
    const found = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!found) return new HttpResponse(null, { status: 404 });
    const inviteLink = `http://localhost:3005/studies/join?token=${found.inviteLink}`;
    return HttpResponse.json({
      inviteLink,
    });
  }),

  http.delete(`${API_URL}${STUDY_URLS.remove}`, async ({ params }) => {
    const { studyId } = params;
    const study = await studyTable.findFirst((q) => q.where({ id: Number(studyId) }));
    if (!study) return new HttpResponse(null, { status: 404 });
    studyTable.delete(study);
    return new HttpResponse(null, { status: 204 });
  }),
];

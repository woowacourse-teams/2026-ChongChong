import { http, HttpResponse } from 'msw';
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
        return {
          id: study.id,
          role: membership.role,
          name: study.name,
          description: study.description,
          memberCount: members.length,
          noticeCount,
          assignmentCount: 2,
        };
      }),
    );
    return HttpResponse.json({ studies: studies.filter((study) => study !== null) });
  }),

  http.get(`${API_URL}${STUDY_URLS.detail}`, async ({ request, params }) => {
    const studyId = Number(params.studyId);
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (!member) return new HttpResponse(null, { status: 403 });
    const isLead = member.role === 'LEADER';
    const now = new Date();
    const openAssignments = assignmentTable
      .findMany((q) => q.where({ studyId }))
      .filter((assignment) => new Date(assignment.closeAt) > now);

    if (isLead) {
      const notices = getIncompleteLeaderNotices(studyId);
      const assignments = openAssignments.flatMap((assignment) => {
        const submissions = submissionTable.findMany((query) =>
          query.where({ assignmentId: assignment.id }),
        );
        const completeCount = submissions.filter(({ submitted }) => submitted).length;

        return completeCount < submissions.length
          ? [{ assignment, memberCount: submissions.length, completeCount }]
          : [];
      });
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
      const assignments = openAssignments.filter((assignment) => {
        const submission = submissionTable.findFirst((query) =>
          query.where({ assignmentId: assignment.id, userId: user.id }),
        );
        return submission?.submitted === false;
      });
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
    const body = (await request.json()) as { name: string; description: string };
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const fieldErrors = validateStudy(body);
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    const studyId = Date.now();
    await studyTable.create({
      id: studyId,
      inviteLink: `https://chongchong.app/join?token=${studyId}`,
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

  http.post(`${API_URL}${STUDY_URLS.join}`, async ({ request }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const { token } = (await request.json()) as { token: string };
    const fieldErrors = validateStudyJoin({ token });
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    const study = studyTable.findFirst((q) => q.where({ inviteLink: token }));
    if (!study) return new HttpResponse(null, { status: 404 });
    if (memberTable.findFirst((q) => q.where({ studyId: study.id, userId: user.id }))) {
      return HttpResponse.json(
        { code: 'ALREADY_JOINED_STUDY', message: '해당 스터디에 이미 가입되어 있습니다.' },
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
      userName: member.name,
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

import { http, HttpResponse } from 'msw';
import { studyTable } from './db';
import { API_URL } from '../../../../config';
import { STUDY_URLS } from '../urls';
import { findUserFromHeader } from '../../../mocks/auth';
import { memberTable } from '../../member/mocks/db';
import { validateStudy, validateStudyJoin } from './validators';
import { invalidInputResponse } from '../../../mocks/errors';
import { assignmentTable } from '../../assignment/mocks/db';

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
        return {
          id: study.id,
          role: membership.role,
          name: study.name,
          description: study.description,
          memberCount: members.length,
          // 공지/과제는 아직 mock table 이 없어 고정값을 사용합니다.
          noticeCount: 2,
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
    const members = memberTable.findMany((q) => q.where({ studyId }));
    const memberCount = members.length;
    const openAssignments = assignmentTable
      .findMany((q) => q.where({ studyId }))
      .filter((assignment) => new Date(assignment.closeAt) > now);

    // 공지 MSW가 존재하지 않아 빈데이터로 표현합니다.
    if (isLead) {
      const assignments = openAssignments.filter(
        (assignment) => assignment.completeUserIds.length < memberCount,
      );
      return HttpResponse.json({
        notices: {
          count: 0,
          items: [],
        },
        assignments: {
          count: assignments.length,
          items: assignments.map((assignment) => ({
            id: assignment.id,
            title: assignment.title,
            memberCount,
            completeCount: assignment.completeUserIds.length,
          })),
        },
      });
    } else {
      const assignments = openAssignments.filter(
        (assignment) => !assignment.completeUserIds.includes(user.id),
      );
      const notices: { id: number; title: string }[] = [];
      return HttpResponse.json({
        totalCount: notices.length + assignments.length,
        notices,
        assignments: assignments.map((assignment) => ({
          id: assignment.id,
          title: assignment.title,
        })),
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

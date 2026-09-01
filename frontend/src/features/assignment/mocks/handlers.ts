import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { paginateByCursor } from '../../../mocks/pagination';
import { memberTable, MemberSchemaType } from '../../member/mocks/db';
import { AssignmentSubmissionValue, AssignmentValue, UpdateAssignmentValue } from '../types';
import { assignmentTable, AssignmentSchemaType, submissionTable } from './db';

export const handlers = [
  http.get(`${API_URL}/studies/:studyId/assignments`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const studyId = Number(params.studyId);
    const studyMembers = memberTable.findMany((q) => q.where({ studyId }));
    const member = studyMembers.find(({ userId }) => userId === user.id);
    if (!member) return new HttpResponse(null, { status: 403 });

    const studyAssignments: AssignmentSchemaType[] = assignmentTable.findMany((q) =>
      q.where({ studyId }),
    );
    const { page, nextCursor, hasNext } = paginateByCursor(
      studyAssignments,
      new URL(request.url).searchParams,
    );

    const memberCount = studyMembers.length;
    const isLeader = member.role === 'LEADER';

    const assignments = page.map((assignment) => {
      const completeCount = assignment.completeUserIds.length;
      const common = {
        id: assignment.id,
        title: assignment.title,
        content: assignment.content,
        submissionMethod: assignment.submissionMethod,
        closeAt: assignment.closeAt,
      };

      if (isLeader) {
        return {
          ...common,
          memberCount,
          completeCount,
          isComplete: memberCount > 0 && completeCount === memberCount,
        };
      }

      return {
        ...common,
        isComplete: assignment.completeUserIds.includes(user.id),
      };
    });

    return HttpResponse.json({ nextCursor, hasNext, assignments });
  }),

  http.post(`${API_URL}/studies/:studyId/assignments`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const studyId = Number(params.studyId);
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const { title, content, submissionMethod, closeAt } = (await request.json()) as AssignmentValue;

    const assignmentId = Date.now();
    await assignmentTable.create({
      id: assignmentId,
      studyId,
      title,
      content,
      submissionMethod,
      closeAt,
      completeUserIds: [],
    });

    return HttpResponse.json({ assignmentId }, { status: 201 });
  }),

  http.patch(
    `${API_URL}/studies/:studyId/assignments/:assignmentId`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const studyId = Number(params.studyId);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

      const assignmentId = Number(params.assignmentId);
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });

      const values = (await request.json()) as UpdateAssignmentValue;
      await assignmentTable.update(assignment, {
        data(assignment) {
          Object.assign(assignment, values);
        },
      });

      return new HttpResponse(null, { status: 204 });
    },
  ),

  http.delete(`${API_URL}/studies/:studyId/assignments/:assignmentId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const studyId = Number(params.studyId);
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const assignmentId = Number(params.assignmentId);
    const deleted = assignmentTable.delete((q) => q.where({ id: assignmentId, studyId }));
    if (!deleted) return new HttpResponse(null, { status: 404 });

    return new HttpResponse(null, { status: 204 });
  }),

  http.get(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/status`,
    ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });
      const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });
      const studyMembers = memberTable.findMany((q) => q.where({ studyId }));
      const isComplete = ({ userId }: MemberSchemaType) =>
        assignment.completeUserIds.includes(userId);

      const completeMembers = studyMembers.filter(isComplete);
      const incompleteMembers = studyMembers.filter((member) => !isComplete(member));

      return HttpResponse.json({
        id: assignment.id,
        memberCount: studyMembers.length,
        completeCount: completeMembers.length,
        incompleteCount: incompleteMembers.length,
        remindAt: '2025-04-16T16:44:10',
        completeMembers,
        incompleteMembers,
      });
    },
  ),

  http.get(`${API_URL}/studies/:studyId/assignments/:assignmentId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
    const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
    if (!assignment) return new HttpResponse(null, { status: 404 });

    const submission = submissionTable.findFirst((q) => q.where({ assignmentId, userId: user.id }));

    return HttpResponse.json({
      id: assignment.id,
      title: assignment.title,
      closeAt: assignment.closeAt,
      content: assignment.content,
      submissionMethod: assignment.submissionMethod,
      ...(submission && { submissionId: submission.id }),
    });
  }),

  http.get(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions`,
    ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });

      const submissions = submissionTable
        .findMany((q) => q.where({ assignmentId }))
        .map(({ id, userId, createdAt }) => {
          const submitter = memberTable.findFirst((q) => q.where({ studyId, userId }));

          return {
            id,
            name: submitter?.name ?? '',
            profileImage: submitter?.profileImage ?? '',
            createdAt,
          };
        });

      return HttpResponse.json({ submissions });
    },
  ),

  http.get(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`,
    ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId, submissionId] = [
        params.studyId,
        params.assignmentId,
        params.submissionId,
      ].map(Number);

      const submission = submissionTable.findFirst((q) =>
        q.where({ id: submissionId, assignmentId: assignmentId }),
      );

      if (!submission) return new HttpResponse(null, { status: 404 });

      const member = memberTable.findFirst((q) =>
        q.where({ studyId: studyId, userId: submission.userId }),
      );

      if (!member) return new HttpResponse(null, { status: 404 });

      return HttpResponse.json({
        id: submission.id,
        name: member.name,
        profileImage: member.profileImage,
        createdAt: submission.createdAt,
        content: submission.content,
        link: submission.link,
      });
    },
  ),

  http.post(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (!member) return new HttpResponse(null, { status: 403 });

      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });

      const submitted = submissionTable.findFirst((q) =>
        q.where({ assignmentId, userId: user.id }),
      );
      if (submitted) return new HttpResponse(null, { status: 409 });

      const { content, link } = (await request.json()) as AssignmentSubmissionValue;

      const submissionId = Date.now();
      await submissionTable.create({
        id: submissionId,
        assignmentId,
        userId: user.id,
        content,
        link,
        createdAt: new Date().toISOString(),
      });

      await assignmentTable.update(assignment, {
        data(assignment) {
          assignment.completeUserIds = [...assignment.completeUserIds, user.id];
        },
      });

      return HttpResponse.json({ submissionId }, { status: 201 });
    },
  ),

  http.patch(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [assignmentId, submissionId] = [params.assignmentId, params.submissionId].map(Number);
      const submission = submissionTable.findFirst((q) =>
        q.where({ id: submissionId, assignmentId }),
      );
      if (!submission) return new HttpResponse(null, { status: 404 });
      if (submission.userId !== user.id) return new HttpResponse(null, { status: 403 });

      const values = (await request.json()) as AssignmentSubmissionValue;
      await submissionTable.update(submission, {
        data(submission) {
          Object.assign(submission, values);
        },
      });

      return new HttpResponse(null, { status: 204 });
    },
  ),
];

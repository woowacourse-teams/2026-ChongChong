import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { paginateByCursor } from '../../../mocks/pagination';
import { memberTable } from '../../member/mocks/db';
import { AssignmentSubmissionValue, AssignmentValue, UpdateAssignmentValue } from '../types';
import { assignmentTable, AssignmentSchemaType, submissionTable } from './db';
import { validateAssignment } from './validators';
import { invalidInputResponse } from '../../../mocks/errors';

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

    const isLeader = member.role === 'LEADER';

    const assignments = page.map((assignment) => {
      const submissions = submissionTable.findMany((q) => q.where({ assignmentId: assignment.id }));
      const mySubmission = submissions.find(({ userId }) => userId === user.id);
      const submissionStatus = !mySubmission
        ? 'NOT_ASSIGNED'
        : mySubmission.submitted
          ? 'SUBMITTED'
          : 'NOT_SUBMITTED';
      const common = {
        id: assignment.id,
        title: assignment.title,
        content: assignment.content,
        submissionMethod: assignment.submissionMethod,
        closeAt: assignment.closeAt,
        submissionStatus,
      };

      if (isLeader) {
        const memberCount = submissions.length;
        const completeCount = submissions.filter(({ submitted }) => submitted).length;

        return {
          ...common,
          memberCount,
          completeCount,
          isComplete: completeCount === memberCount,
        };
      }

      return common;
    });

    return HttpResponse.json({ nextCursor, hasNext, assignments });
  }),

  http.post(`${API_URL}/studies/:studyId/assignments`, async ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });

    const body = (await request.json()) as AssignmentValue;
    const fieldErrors = validateAssignment(body);
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    const studyId = Number(params.studyId);
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const { title, content, submissionMethod, closeAt, submissionTarget } = body;
    const assignmentId = Date.now();
    await assignmentTable.create({
      id: assignmentId,
      studyId,
      title,
      content,
      submissionMethod,
      closeAt,
      submissionTarget,
      completeUserIds: [],
    });
    const submitters = memberTable
      .findMany((q) => q.where({ studyId }))
      .filter(({ role }) => submissionTarget === 'MEMBERS_AND_LEADER' || role !== 'LEADER');

    await Promise.all(
      submitters.map((submitter, index) =>
        submissionTable.create({
          id: assignmentId + index + 1,
          assignmentId,
          userId: submitter.userId,
          submitted: false,
        }),
      ),
    );

    return HttpResponse.json({ assignmentId }, { status: 201 });
  }),

  http.patch(
    `${API_URL}/studies/:studyId/assignments/:assignmentId`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });
      const body = (await request.json()) as UpdateAssignmentValue;
      const fieldErrors = validateAssignment(body);
      if (fieldErrors.length > 0) {
        return invalidInputResponse(fieldErrors);
      }

      const studyId = Number(params.studyId);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

      const assignmentId = Number(params.assignmentId);
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });

      await assignmentTable.update(assignment, {
        data(assignment) {
          Object.assign(assignment, body);
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
      const submissions = submissionTable.findMany((q) => q.where({ assignmentId }));
      const toMember = ({ userId }: (typeof submissions)[number]) =>
        memberTable.findFirst((q) => q.where({ studyId, userId }));
      const completeMembers = submissions
        .filter(({ submitted }) => submitted)
        .flatMap((submission) => {
          const member = toMember(submission);
          return member
            ? [{ id: member.id, name: member.name, profileImage: member.profileImage }]
            : [];
        });
      const incompleteMembers = submissions
        .filter(({ submitted }) => !submitted)
        .flatMap((submission) => {
          const member = toMember(submission);
          return member
            ? [
                {
                  id: member.id,
                  name: member.name,
                  profileImage: member.profileImage,
                  lastRemindAt: null,
                },
              ]
            : [];
        });

      return HttpResponse.json({
        id: assignment.id,
        memberCount: submissions.length,
        completeCount: completeMembers.length,
        incompleteCount: incompleteMembers.length,
        remindAt: null,
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

    return HttpResponse.json({
      id: assignment.id,
      title: assignment.title,
      closeAt: assignment.closeAt,
      content: assignment.content,
      submissionMethod: assignment.submissionMethod,
      submissionTarget: assignment.submissionTarget,
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
        .filter(({ submitted }) => submitted)
        .map(({ id, userId, createdAt }) => {
          const submitter = memberTable.findFirst((q) => q.where({ studyId, userId }));

          return {
            id,
            name: submitter?.name ?? '',
            profileImage: submitter?.profileImage ?? null,
            createdAt,
          };
        });

      return HttpResponse.json({ submissions });
    },
  ),

  http.get(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/my`,
    ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
      const member = memberTable.findFirst((query) => query.where({ studyId, userId: user.id }));
      if (!member) return new HttpResponse(null, { status: 403 });

      const submission = submissionTable.findFirst((query) =>
        query.where({ assignmentId, userId: user.id }),
      );

      if (!submission) {
        return HttpResponse.json({ submissionStatus: 'NOT_ASSIGNED' as const });
      }

      if (!submission.submitted) {
        return HttpResponse.json({
          submissionId: submission.id,
          submissionStatus: 'NOT_SUBMITTED' as const,
        });
      }

      return HttpResponse.json({
        submissionId: submission.id,
        submissionStatus: 'SUBMITTED' as const,
        createdAt: submission.createdAt,
        ...(submission.content === null ? {} : { content: submission.content }),
        ...(submission.link === null ? {} : { link: submission.link }),
      });
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

      if (!submission?.submitted) return new HttpResponse(null, { status: 404 });

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
      if (member.role === 'LEADER' && assignment.submissionTarget === 'MEMBERS_ONLY') {
        return new HttpResponse(null, { status: 404 });
      }

      const submission = submissionTable.findFirst((q) =>
        q.where({ assignmentId, userId: user.id }),
      );
      if (!submission) return new HttpResponse(null, { status: 404 });
      if (submission.submitted) return new HttpResponse(null, { status: 409 });

      const { content, link } = (await request.json()) as AssignmentSubmissionValue;

      const createdAt = new Date().toISOString();
      const submissionId = submission.id;

      submissionTable.delete((q) => q.where({ id: submission.id }));
      await submissionTable.create({
        ...submission,
        submitted: true,
        content,
        link: link ?? null,
        createdAt,
      });

      assignmentTable.delete((q) => q.where({ id: assignmentId, studyId }));
      await assignmentTable.create({
        ...assignment,
        completeUserIds: [...assignment.completeUserIds, user.id],
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
      submissionTable.delete((q) => q.where({ id: submission.id }));
      await submissionTable.create({
        ...submission,
        ...values,
      });

      return new HttpResponse(null, { status: 204 });
    },
  ),
];

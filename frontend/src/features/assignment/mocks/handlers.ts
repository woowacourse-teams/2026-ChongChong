import { fileAttachments } from '../../../shared/modules/files/mocks/attachments';
import { validateAssignmentAttachments } from './attachments';
import { http, HttpResponse } from 'msw';
import { API_URL } from '../../../../config';
import { findUserFromHeader } from '../../../mocks/auth';
import { paginateByCursor } from '../../../mocks/pagination';
import { memberTable, type MemberSchemaType } from '../../member/mocks/db';
import type { AssignmentSubmissionValue } from '../types';
import { assignmentTable, AssignmentSchemaType, submissionTable } from './db';
import { validateAssignment, type AssignmentWriteInput } from './validators';
import { invalidInputResponse } from '../../../mocks/errors';

function getCompletedSubmissionStatus(createdAt: string | null, closeAt: string) {
  return createdAt && new Date(createdAt) > new Date(closeAt)
    ? ('LATE_SUBMITTED' as const)
    : ('SUBMITTED' as const);
}

function getSubmitters(body: AssignmentWriteInput, actor: MemberSchemaType, assignmentId?: number) {
  const members = memberTable.findMany((q) => q.where({ studyId: actor.studyId }));
  if (body.submitterIds != null) {
    const ids = body.submitterIds;
    if (
      !Array.isArray(ids) ||
      ids.length === 0 ||
      new Set(ids).size !== ids.length ||
      ids.some((id) => !Number.isInteger(id) || !members.some((member) => member.id === id))
    )
      return null;
    return members.filter(({ id }) => ids.includes(id));
  }
  const current =
    assignmentId === undefined
      ? members
      : members.filter(({ userId }) =>
          submissionTable.findFirst((q) => q.where({ assignmentId, userId, assigned: true })),
        );
  const submitters =
    body.submissionTarget === 'MEMBERS_ONLY'
      ? current.filter(({ role }) => role !== 'LEADER')
      : [...current.filter(({ id }) => id !== actor.id), actor];
  return submitters.length ? submitters : null;
}

function invalidSubmittersResponse() {
  return invalidInputResponse([
    {
      field: 'submitterIds',
      code: 'INVALID',
      reason: '중복 없이 같은 스터디의 멤버를 한 명 이상 선택해주세요.',
    },
  ]);
}

async function replaceSubmitters(assignmentId: number, submitters: MemberSchemaType[]) {
  const userIds = submitters.map(({ userId }) => userId);
  const existing = submissionTable.findMany((q) => q.where({ assignmentId }));
  for (const submission of existing) {
    await submissionTable.update(submission, {
      data(record) {
        record.assigned = userIds.includes(record.userId);
      },
    });
  }
  let id = Math.max(0, ...submissionTable.findMany().map(({ id }) => id));
  for (const { userId } of submitters) {
    if (existing.some((submission) => submission.userId === userId)) continue;
    await submissionTable.create({ id: ++id, assignmentId, userId, submitted: false });
  }
}

function pendingReminders(assignment: AssignmentSchemaType) {
  return assignment.remindAts.filter((value) => Date.parse(value) > Date.now()).sort();
}

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
      const submissionStatus =
        !mySubmission || !mySubmission.assigned
          ? 'NOT_ASSIGNED'
          : mySubmission.submitted
            ? getCompletedSubmissionStatus(mySubmission.createdAt, assignment.closeAt)
            : Date.parse(assignment.closeAt) < Date.now()
              ? 'MISSING'
              : 'NOT_SUBMITTED';
      const common = {
        id: assignment.id,
        title: assignment.title,
        content: assignment.content,
        submissionMethod: assignment.submissionMethod,
        closeAt: assignment.closeAt,
        submissionStatus,
        submissionVisibility: assignment.submissionVisibility,
      };

      if (isLeader) {
        const assigned = submissions.filter(({ assigned }) => assigned);
        const memberCount = assigned.length;
        const completeCount = assigned.filter(({ submitted }) => submitted).length;

        return {
          ...common,
          memberCount,
          completeCount,
          remindAt: pendingReminders(assignment)[0],
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

    const body = (await request.json()) as AssignmentWriteInput;
    const fieldErrors = validateAssignment(body);
    if (fieldErrors.length > 0) {
      return invalidInputResponse(fieldErrors);
    }

    const studyId = Number(params.studyId);
    const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
    if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

    const { title, content, submissionMethod, closeAt, submissionVisibility } = body;
    const submitters = getSubmitters(body, member);
    if (!submitters) return invalidSubmittersResponse();
    const attachmentFileIds = body.attachmentFileIds ?? [];
    const attachmentError = validateAssignmentAttachments(attachmentFileIds, user.id);
    if (attachmentError) return attachmentError;
    const assignmentId = Date.now();
    await assignmentTable.create({
      id: assignmentId,
      studyId,
      title: title!,
      content: content!,
      submissionMethod: submissionMethod?.trim() ? submissionMethod : '',
      closeAt: closeAt!,
      submissionTarget: submitters.some(({ role }) => role === 'LEADER')
        ? 'MEMBERS_AND_LEADER'
        : 'MEMBERS_ONLY',
      submissionVisibility: submissionVisibility!,
      remindAts: [...new Set(body.remindAts ?? [])],
      completeUserIds: [],
      attachmentFileIds,
    });
    await replaceSubmitters(assignmentId, submitters);

    return HttpResponse.json({ assignmentId }, { status: 201 });
  }),

  http.patch(
    `${API_URL}/studies/:studyId/assignments/:assignmentId`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });
      const body = (await request.json()) as AssignmentWriteInput;
      const fieldErrors = validateAssignment(body, true);
      if (fieldErrors.length > 0) {
        return invalidInputResponse(fieldErrors);
      }

      const studyId = Number(params.studyId);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (member?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });

      const assignmentId = Number(params.assignmentId);
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });

      const attachmentFileIds = body.attachmentFileIds ?? assignment.attachmentFileIds ?? [];
      const attachmentError = validateAssignmentAttachments(
        attachmentFileIds,
        user.id,
        assignment.attachmentFileIds,
      );
      if (attachmentError) return attachmentError;

      const submitters =
        body.submitterIds != null || body.submissionTarget != null
          ? getSubmitters(body, member, assignmentId)
          : undefined;
      if (submitters === null) return invalidSubmittersResponse();

      await assignmentTable.update(assignment, {
        data(assignment) {
          for (const field of ['title', 'content', 'closeAt', 'submissionVisibility'] as const) {
            if (body[field] != null) Object.assign(assignment, { [field]: body[field] });
          }
          if (body.submissionMethod != null)
            assignment.submissionMethod = body.submissionMethod.trim() ? body.submissionMethod : '';
          if (body.remindAts != null) assignment.remindAts = [...new Set(body.remindAts)];
          assignment.attachmentFileIds = attachmentFileIds;
          if (submitters)
            assignment.submissionTarget = submitters.some(({ role }) => role === 'LEADER')
              ? 'MEMBERS_AND_LEADER'
              : 'MEMBERS_ONLY';
        },
      });
      if (submitters) await replaceSubmitters(assignmentId, submitters);

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
      const actor = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (actor?.role !== 'LEADER') return new HttpResponse(null, { status: 403 });
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });
      const submissions = submissionTable.findMany((q) =>
        q.where({ assignmentId, assigned: true }),
      );
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
        remindAt: pendingReminders(assignment)[0] ?? null,
        completeMembers,
        incompleteMembers,
      });
    },
  ),

  http.get(`${API_URL}/studies/:studyId/assignments/:assignmentId`, ({ request, params }) => {
    const user = findUserFromHeader(request.headers);
    if (!user) return new HttpResponse(null, { status: 401 });
    const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
    if (!memberTable.findFirst((q) => q.where({ studyId, userId: user.id })))
      return new HttpResponse(null, { status: 403 });
    const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
    if (!assignment) return new HttpResponse(null, { status: 404 });

    return HttpResponse.json({
      id: assignment.id,
      title: assignment.title,
      closeAt: assignment.closeAt,
      content: assignment.content,
      submissionMethod: assignment.submissionMethod,
      submitterIds: submissionTable
        .findMany((q) => q.where({ assignmentId, assigned: true }))
        .flatMap(({ userId }) => {
          const member = memberTable.findFirst((q) => q.where({ studyId, userId }));
          return member ? [member.id] : [];
        }),
      remindAts: pendingReminders(assignment),
      submissionTarget: assignment.submissionTarget,
      submissionVisibility: assignment.submissionVisibility,
      attachments: fileAttachments(assignment.attachmentFileIds, true),
    });
  }),

  http.get(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions`,
    ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId] = [params.studyId, params.assignmentId].map(Number);
      const member = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (!member) return new HttpResponse(null, { status: 403 });

      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });
      if (member.role !== 'LEADER' && assignment.submissionVisibility !== 'ALL_STUDY_MEMBERS') {
        return new HttpResponse(null, { status: 403 });
      }

      const submissions = submissionTable
        .findMany((q) => q.where({ assignmentId }))
        .filter(({ submitted }) => submitted)
        .map(({ id, userId, createdAt }) => {
          const submitter = memberTable.findFirst((q) => q.where({ studyId, userId }));

          return {
            id,
            name: submitter?.name ?? '',
            profileImage: submitter?.profileImage ?? null,
            submissionStatus: getCompletedSubmissionStatus(createdAt, assignment.closeAt),
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

      const assignment = assignmentTable.findFirst((query) =>
        query.where({ id: assignmentId, studyId }),
      );
      if (!assignment) return new HttpResponse(null, { status: 404 });

      const submission = submissionTable.findFirst((query) =>
        query.where({ assignmentId, userId: user.id }),
      );

      if (!submission || (!submission.assigned && !submission.submitted)) {
        return HttpResponse.json({
          submissionStatus: 'NOT_ASSIGNED' as const,
          assigned: false,
          attachments: [],
        });
      }

      if (!submission.submitted) {
        return HttpResponse.json({
          submissionId: submission.id,
          assigned: true,
          submissionStatus:
            Date.parse(assignment.closeAt) < Date.now() ? 'MISSING' : 'NOT_SUBMITTED',
        });
      }

      return HttpResponse.json({
        submissionId: submission.id,
        assigned: submission.assigned,
        submissionStatus: getCompletedSubmissionStatus(submission.createdAt, assignment.closeAt),
        createdAt: submission.createdAt,
        ...(submission.content === null ? {} : { content: submission.content }),
        ...(submission.link === null ? {} : { link: submission.link }),
        attachments: fileAttachments(submission.attachmentFileIds, true),
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

      const actor = memberTable.findFirst((q) => q.where({ studyId, userId: user.id }));
      if (!actor) return new HttpResponse(null, { status: 403 });
      const assignment = assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId }));
      if (!assignment) return new HttpResponse(null, { status: 404 });
      const submission = submissionTable.findFirst((q) =>
        q.where({ id: submissionId, assignmentId: assignmentId }),
      );

      if (!submission?.submitted) return new HttpResponse(null, { status: 404 });
      if (
        assignment.submissionVisibility !== 'ALL_STUDY_MEMBERS' &&
        actor.role !== 'LEADER' &&
        submission.userId !== user.id
      ) {
        return new HttpResponse(null, { status: 403 });
      }

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
        attachments: fileAttachments(submission.attachmentFileIds, true),
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

      const submission = submissionTable.findFirst((q) =>
        q.where({ assignmentId, userId: user.id }),
      );
      if (!submission) return new HttpResponse(null, { status: 404 });
      if (!submission.assigned) return new HttpResponse(null, { status: 403 });
      const body = (await request.json()) as AssignmentSubmissionValue & {
        attachmentFileIds?: number[] | null;
      };
      const content = body.content ?? null;
      const link = body.link ?? null;
      const attachmentFileIds = body.attachmentFileIds ?? submission.attachmentFileIds ?? [];
      const attachmentError = validateAssignmentAttachments(
        attachmentFileIds,
        user.id,
        submission.attachmentFileIds,
        'file',
      );
      if (attachmentError) return attachmentError;
      if (!content?.trim() && !link?.trim() && !attachmentFileIds.length) {
        return HttpResponse.json(
          { code: 'EMPTY_SUBMISSION', message: '내용, 링크, 파일 중 하나 이상을 입력해주세요.' },
          { status: 400 },
        );
      }

      const createdAt = submission.createdAt ?? new Date().toISOString();
      const submissionId = submission.id;

      submissionTable.delete((q) => q.where({ id: submission.id }));
      await submissionTable.create({
        ...submission,
        submitted: true,
        content,
        link,
        attachmentFileIds,
        createdAt,
      });

      assignmentTable.delete((q) => q.where({ id: assignmentId, studyId }));
      await assignmentTable.create({
        ...assignment,
        completeUserIds: [...new Set([...assignment.completeUserIds, user.id])],
      });

      return HttpResponse.json({ submissionId }, { status: 201 });
    },
  ),

  http.patch(
    `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId`,
    async ({ request, params }) => {
      const user = findUserFromHeader(request.headers);
      if (!user) return new HttpResponse(null, { status: 401 });

      const [studyId, assignmentId, submissionId] = [
        params.studyId,
        params.assignmentId,
        params.submissionId,
      ].map(Number);
      if (!memberTable.findFirst((q) => q.where({ studyId, userId: user.id })))
        return new HttpResponse(null, { status: 403 });
      if (!assignmentTable.findFirst((q) => q.where({ id: assignmentId, studyId })))
        return new HttpResponse(null, { status: 404 });
      const submission = submissionTable.findFirst((q) =>
        q.where({ id: submissionId, assignmentId }),
      );
      if (!submission) return new HttpResponse(null, { status: 404 });
      if (!submission.assigned || submission.userId !== user.id)
        return new HttpResponse(null, { status: 403 });

      const values = (await request.json()) as AssignmentSubmissionValue & {
        attachmentFileIds?: number[] | null;
      };
      const content = values.content ?? submission.content;
      const link = values.link ?? submission.link;
      const attachmentFileIds = values.attachmentFileIds ?? submission.attachmentFileIds ?? [];
      const attachmentError = validateAssignmentAttachments(
        attachmentFileIds,
        user.id,
        submission.attachmentFileIds,
        'file',
      );
      if (attachmentError) return attachmentError;
      if (!content?.trim() && !link?.trim() && !attachmentFileIds.length) {
        return HttpResponse.json(
          { code: 'EMPTY_SUBMISSION', message: '내용, 링크, 파일 중 하나 이상을 입력해주세요.' },
          { status: 400 },
        );
      }
      submissionTable.delete((q) => q.where({ id: submission.id }));
      await submissionTable.create({
        ...submission,
        content,
        link,
        attachmentFileIds,
      });

      return new HttpResponse(null, { status: 204 });
    },
  ),
];

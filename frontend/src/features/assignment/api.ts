import api from '../../client';
import {
  AssignmentSubmitStatus,
  AssignmentDetail,
  Submission,
  SubmissionDetail,
  AssignmentValue,
  UpdateAssignmentValue,
  AssignmentListResponse,
  AssignmentSubmissionValue,
  CreateAssignmentResponse,
  CreateSubmissionResponse,
} from './types';

export async function fetchAssignmentList(studyId: number, cursor?: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments`, {
      searchParams: cursor === undefined ? undefined : { cursor },
    });
    return await response.json<AssignmentListResponse>();
  } catch {
    throw new Error('과제 목록을 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignmentSubmitStatus(studyId: number, assignmentsId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentsId}/status`);
    return await response.json<AssignmentSubmitStatus>();
  } catch {
    throw new Error('과제 제출 현황을 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignment(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentId}`);

    return await response.json<AssignmentDetail>();
  } catch {
    throw new Error('과제 정보를 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignmentSubmission(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentId}/submissions`);

    return await response.json<{ submissions: Submission[] }>();
  } catch {
    throw new Error('제출 내역을 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignmentSubmissionDetail(
  studyId: number,
  assignmentId: number,
  submissionId: number,
) {
  try {
    const response = await api.get(
      `/studies/${studyId}/assignments/${assignmentId}/submissions/${submissionId}`,
    );

    return await response.json<SubmissionDetail>();
  } catch {
    throw new Error('제출 정보를 불러오는데 실패했습니다.');
  }
}

export async function createAssignmentSubmission(
  studyId: number,
  assignmentId: number,
  values: AssignmentSubmissionValue,
) {
  return api
    .post(`studies/${studyId}/assignments/${assignmentId}/submissions`, {
      json: values,
    })
    .json<CreateSubmissionResponse>();
}

export async function updateAssignmentSubmission(
  studyId: number,
  assignmentId: number,
  submissionId: number,
  values: AssignmentSubmissionValue,
) {
  await api.patch(`studies/${studyId}/assignments/${assignmentId}/submissions/${submissionId}`, {
    json: values,
  });
}

export async function createAssignment(studyId: number, values: AssignmentValue) {
  return api
    .post(`studies/${studyId}/assignments`, {
      json: values,
    })
    .json<CreateAssignmentResponse>();
}

export async function updateAssignment(
  studyId: number,
  assignmentId: number,
  values: UpdateAssignmentValue,
) {
  await api.patch(`studies/${studyId}/assignments/${assignmentId}`, {
    json: values,
  });
}

export async function deleteAssignment(studyId: number, assignmentId: number) {
  await api.delete(`studies/${studyId}/assignments/${assignmentId}`);
}

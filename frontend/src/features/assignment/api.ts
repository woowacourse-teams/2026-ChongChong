import api from '../../client';
import { AssignmentValue, UpdateAssignmentValue, AssignmentSubmissionValue } from './types';
import { getErrorResponse, ValidationError, FIELD_ERROR_CODE } from '../../shared/api/error';
import {
  isAssignmentListResponse,
  isCreateAssignmentResponse,
  isSubmissionDetailResponse,
  isAssignmentDetailResponse,
  isSubmissionListResponse,
  isAssignmentSubmitStatusResponse,
  isCreateSubmissionResponse,
  isUserAssignmentSubmitDetailResponse,
} from './responseSchemas';

export async function fetchAssignmentList(studyId: number, cursor?: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments`, {
      searchParams: cursor === undefined ? undefined : { cursor },
    });
    const data: unknown = await response.json();

    if (!isAssignmentListResponse(data)) {
      throw new Error('과제 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('과제 목록을 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignmentSubmitStatus(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentId}/status`);

    const data: unknown = await response.json();

    if (!isAssignmentSubmitStatusResponse(data)) {
      throw new Error('과제 제출 상태 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('과제 제출 현황을 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignment(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentId}`);

    const data: unknown = await response.json();

    if (!isAssignmentDetailResponse(data)) {
      throw new Error('과제 정보 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('과제 정보를 불러오는데 실패했습니다.');
  }
}

export async function fetchAssignmentSubmission(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(`/studies/${studyId}/assignments/${assignmentId}/submissions`);

    const data: unknown = await response.json();

    if (!isSubmissionListResponse(data)) {
      throw new Error('과제 제출 목록 응답 형식이 올바르지 않습니다.');
    }

    return data;
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

    const data: unknown = await response.json();

    if (!isSubmissionDetailResponse(data)) {
      throw new Error('과제 제출 상세 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('제출 정보를 불러오는데 실패했습니다.');
  }
}

export async function createAssignmentSubmission(
  studyId: number,
  assignmentId: number,
  values: AssignmentSubmissionValue,
) {
  try {
    const response = await api.post(`/studies/${studyId}/assignments/${assignmentId}/submissions`, {
      json: values,
    });

    const data: unknown = await response.json();

    if (!isCreateSubmissionResponse(data)) {
      throw new Error('과제 제출 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('과제 제출에 실패했습니다.');
  }
}

export async function updateAssignmentSubmission(
  studyId: number,
  assignmentId: number,
  submissionId: number,
  values: AssignmentSubmissionValue,
) {
  try {
    await api.patch(`/studies/${studyId}/assignments/${assignmentId}/submissions/${submissionId}`, {
      json: values,
    });
  } catch {
    throw new Error('과제 제출물 수정에 실패했습니다.');
  }
}

export async function createAssignment(studyId: number, values: AssignmentValue) {
  try {
    const response = await api.post(`/studies/${studyId}/assignments`, { json: values });
    const data: unknown = await response.json();

    if (!isCreateAssignmentResponse(data)) {
      throw new Error('과제 생성 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch (error) {
    const errorResponse = getErrorResponse(error);

    if (errorResponse?.code === FIELD_ERROR_CODE) {
      throw new ValidationError({
        message: errorResponse.message,
        errors: errorResponse.errors,
        options: {
          cause: error,
        },
      });
    }

    throw new Error('과제를 생성하는데 실패했습니다.', {
      cause: error,
    });
  }
}

export async function updateAssignment(
  studyId: number,
  assignmentId: number,
  values: UpdateAssignmentValue,
) {
  try {
    await api.patch(`/studies/${studyId}/assignments/${assignmentId}`, {
      json: values,
    });
  } catch (error) {
    const errorResponse = getErrorResponse(error);

    if (errorResponse?.code === FIELD_ERROR_CODE) {
      throw new ValidationError({
        message: errorResponse.message,
        errors: errorResponse.errors,
        options: {
          cause: error,
        },
      });
    }

    throw new Error('과제 수정에 실패했습니다.', { cause: error });
  }
}

export async function deleteAssignment(studyId: number, assignmentId: number) {
  try {
    await api.delete(`/studies/${studyId}/assignments/${assignmentId}`);
  } catch {
    throw new Error('과제 삭제에 실패했습니다.');
  }
}

export async function fetchMyAssignmentSubmission(studyId: number, assignmentId: number) {
  try {
    const response = await api.get(
      `/studies/${studyId}/assignments/${assignmentId}/submissions/my`,
    );

    const data: unknown = await response.json();

    if (!isUserAssignmentSubmitDetailResponse(data)) {
      throw new Error('과제 제출 정보 응답 형식이 올바르지 않습니다.');
    }

    return data;
  } catch {
    throw new Error('내 제출 정보를 불러오는데 실패했습니다.');
  }
}

import api from '../../../client';
import { ApiError, handleError } from '../../api/error';
import { isCommentListResponse } from './responseSchemas';
import type { CommentTarget } from './types';

function commentsPath(target: CommentTarget) {
  const base = `/studies/${target.studyId}`;
  switch (target.type) {
    case 'notice':
      return `${base}/notices/${target.noticeId}/comments`;
    case 'assignment':
      return `${base}/assignments/${target.assignmentId}/comments`;
    case 'submission':
      return `${base}/assignments/${target.assignmentId}/submissions/${target.submissionId}/comments`;
  }
}

export async function fetchComments(
  target: CommentTarget,
  cursor: number | null,
  signal?: AbortSignal,
) {
  try {
    const response = await api.get(commentsPath(target), {
      searchParams: cursor === null ? { size: 10 } : { size: 10, cursor },
      signal,
      retry: 0,
    });
    const data: unknown = await response.json();
    if (!isCommentListResponse(data)) {
      throw new Error('댓글 목록 응답 형식이 올바르지 않습니다.');
    }
    return data;
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('댓글을 불러오지 못했어요. 다시 시도해주세요.', { cause: error }),
    });
  }
}

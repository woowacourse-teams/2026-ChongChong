import api from '../../../client';
import { ApiError, handleError } from '../../api/error';
import { isCommentCreateResponse, isCommentListResponse } from './responseSchemas';
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

export async function createComment(target: CommentTarget, content: string) {
  try {
    const response = await api.post(commentsPath(target), { json: { content }, retry: 0 });
    const data: unknown = await response.json();
    if (!isCommentCreateResponse(data)) throw new Error('댓글 등록 응답 형식이 올바르지 않습니다.');
    return data;
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('댓글을 등록하지 못했어요. 다시 시도해주세요.', { cause: error }),
    });
  }
}

export async function updateComment(target: CommentTarget, commentId: number, content: string) {
  try {
    await api.patch(`${commentsPath(target)}/${commentId}`, { json: { content }, retry: 0 });
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('댓글을 수정하지 못했어요. 다시 시도해주세요.', { cause: error }),
    });
  }
}

export async function deleteComment(target: CommentTarget, commentId: number) {
  try {
    await api.delete(`${commentsPath(target)}/${commentId}`, { retry: 0 });
  } catch (error) {
    throw handleError(error, {
      mappers: [ApiError],
      fallback: new Error('댓글을 삭제하지 못했어요. 다시 시도해주세요.', { cause: error }),
    });
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

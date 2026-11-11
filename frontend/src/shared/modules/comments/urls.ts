import { API_URL } from '../../../../config';

export const COMMENT_URLS = {
  notice: {
    path: (studyId: number, noticeId: number) => `/studies/${studyId}/notices/${noticeId}/comments`,
    mock: `${API_URL}/studies/:studyId/notices/:noticeId/comments`,
  },
  assignment: {
    path: (studyId: number, assignmentId: number) =>
      `/studies/${studyId}/assignments/${assignmentId}/comments`,
    mock: `${API_URL}/studies/:studyId/assignments/:assignmentId/comments`,
  },
  submission: {
    path: (studyId: number, assignmentId: number, submissionId: number) =>
      `/studies/${studyId}/assignments/${assignmentId}/submissions/${submissionId}/comments`,
    mock: `${API_URL}/studies/:studyId/assignments/:assignmentId/submissions/:submissionId/comments`,
  },
};

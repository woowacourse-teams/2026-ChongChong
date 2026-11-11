export type CommentTarget = { studyId: number } & (
  | { type: 'notice'; noticeId: number }
  | { type: 'assignment'; assignmentId: number }
  | { type: 'submission'; assignmentId: number; submissionId: number }
);

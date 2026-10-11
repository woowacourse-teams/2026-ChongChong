import type { CompletedAssignment, UserAssignmentSubmitDetail } from './types';

export function isCompletedAssignment(
  submission: UserAssignmentSubmitDetail,
): submission is CompletedAssignment {
  return (
    submission.submissionStatus === 'SUBMITTED' || submission.submissionStatus === 'LATE_SUBMITTED'
  );
}

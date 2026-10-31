import { isCompletedAssignment } from '../submissionStatus';
import { UserAssignmentSubmitDetail } from '../types';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';
import CompletedAssignmentSubmission from './CompletedAssignmentSubmission';
import useCreateAssignmentSubmission from '../hooks/useCreateAssignmentSubmission';

interface Props {
  studyId: number;
  assignmentId: number;
  submission: UserAssignmentSubmitDetail;
}

export default function MyAssignmentSubmission({ studyId, assignmentId, submission }: Props) {
  const {
    mutate: handleCreateAssignmentSubmission,
    isPending,
    fieldErrors,
  } = useCreateAssignmentSubmission({ studyId, assignmentId });

  if (submission.submissionStatus === 'NOT_ASSIGNED') return null;

  return isCompletedAssignment(submission) ? (
    <CompletedAssignmentSubmission
      key={`${studyId}-${assignmentId}-${submission.submissionId}`}
      assignmentId={assignmentId}
      studyId={studyId}
      submission={submission}
    />
  ) : (
    <AssignmentSubmissionForm
      key={`${studyId}-${assignmentId}`}
      isSubmitting={isPending}
      onSubmit={handleCreateAssignmentSubmission}
      fieldErrors={fieldErrors}
    />
  );
}

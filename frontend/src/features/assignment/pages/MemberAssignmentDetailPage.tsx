import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import { createAssignmentSubmission } from '../api';
import AssignmentArticle from '../components/AssignmentArticle';
import AssignmentSubmissionForm from '../components/AssignmentSubmissionForm';
import CompletedAssignmentSubmission from '../components/CompletedAssignmentSubmission';
import assignmentQueries from '../queries';
import type { AssignmentSubmissionValue } from '../types';

export default function MemberAssignmentDetailPage() {
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();

  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(Number(studyId), Number(assignmentId)),
  );

  const createMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(Number(studyId), Number(assignmentId), values),
    onSuccess: ({ submissionId }) => {
      queryClient.setQueryData(
        assignmentQueries.detail(Number(studyId), Number(assignmentId)).queryKey,
        { ...assignment, submissionId },
      );
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(Number(studyId)) });
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />

      <Main>
        <AssignmentArticle assignment={assignment} />

        {assignment.submissionId ? (
          <CompletedAssignmentSubmission
            key={`${Number(studyId)}-${Number(assignmentId)}-${assignment.submissionId}`}
            assignmentId={Number(assignmentId)}
            studyId={Number(studyId)}
            submissionId={assignment.submissionId}
          />
        ) : (
          <AssignmentSubmissionForm
            key={`${Number(studyId)}-${Number(assignmentId)}`}
            isSubmitting={createMutation.isPending}
            onSubmit={createMutation.mutate}
          />
        )}
      </Main>
    </Page>
  );
}

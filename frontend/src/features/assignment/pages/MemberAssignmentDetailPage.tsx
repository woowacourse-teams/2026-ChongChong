import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import { createAssignmentSubmission } from '../api';
import AssignmentArticle from '../components/AssignmentArticle';
import AssignmentSubmissionForm from '../components/AssignmentSubmissionForm';
import assignmentQueries from '../queries';
import type { AssignmentSubmissionValue } from '../types';

export default function MemberAssignmentDetailPage() {
  const { studyId, assignmentId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const numericStudyId = Number(studyId);
  const numericAssignmentId = Number(assignmentId);

  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(numericStudyId, numericAssignmentId),
  );

  const submissionMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(numericStudyId, numericAssignmentId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(numericStudyId),
      });
      navigate(`/studies/${studyId}/assignments`);
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />

      <Main css={{ paddingTop: 0 }}>
        <AssignmentArticle assignment={assignment} />
        <AssignmentSubmissionForm
          isSubmitting={submissionMutation.isPending}
          onSubmit={submissionMutation.mutate}
        />
      </Main>
    </Page>
  );
}

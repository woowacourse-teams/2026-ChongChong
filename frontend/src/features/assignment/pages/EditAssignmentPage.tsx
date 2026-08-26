import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import { useParams, useNavigate } from 'react-router';
import assignmentQueries from '../queries';
import { AssignmentValue } from '../types';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { updateAssignment } from '../api';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function EditAssignmentPage() {
  const { studyId, assignmentId } = useParams();
  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(Number(studyId), Number(assignmentId)),
  );

  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: (values: AssignmentValue) =>
      updateAssignment(Number(studyId), Number(assignmentId), values),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(Number(studyId)),
      });

      queryClient.invalidateQueries({
        queryKey: assignmentQueries.detail(Number(studyId), Number(assignmentId)).queryKey,
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm
          submitLabel="과제 수정하기"
          onSubmit={updateMutation.mutate}
          initialValues={{
            title: assignment.title,
            content: assignment.content,
            submissionMethod: assignment.submissionMethod,
            closeAt: assignment.closeAt,
          }}
        />
      </Main>
      <BottomTab />
    </Page>
  );
}

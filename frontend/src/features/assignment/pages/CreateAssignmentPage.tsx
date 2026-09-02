import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import { AssignmentValue } from '../types';
import { useParams, useNavigate } from 'react-router';
import { createAssignment } from '../api';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import assignmentQueries from '../queries';

export default function CreateAssignmentPage() {
  const { studyId } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (values: AssignmentValue) => createAssignment(Number(studyId), values),

    onSuccess: ({ assignmentId }) => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(Number(studyId)),
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm
          isSubmitting={createMutation.isPending}
          error={createMutation.error}
          submitLabel="과제 올리기"
          onSubmit={(values) => createMutation.mutate(values)}
        />
      </Main>
      <BottomTab />
    </Page>
  );
}

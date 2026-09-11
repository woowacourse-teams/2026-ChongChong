import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import { AssignmentValue } from '../types';
import { useNavigate } from 'react-router';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { createAssignment } from '../api';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { ValidationError } from '../../../shared/api/error';
import { useMemo } from 'react';

export default function CreateAssignmentPage() {
  const { studyId } = useIntegerParams(['studyId']);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentValue) => createAssignment(studyId, values),

    onSuccess: ({ assignmentId }) => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },

    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status={'Error'} message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm
          isSubmitting={isPending}
          submitLabel="과제 올리기"
          onSubmit={(values) => mutate(values)}
          fieldErrors={fieldErrors}
        />
      </Main>
      <BottomTab />
    </Page>
  );
}

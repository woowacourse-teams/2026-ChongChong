import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import { useNavigate } from 'react-router';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import assignmentQueries from '../queries';
import { AssignmentValue } from '../types';
import { useQueryClient, useMutation } from '@tanstack/react-query';
import { updateAssignment } from '../api';
import { useSuspenseQuery } from '@tanstack/react-query';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { ValidationError } from '../../../shared/api/error';
import { Suspense, useMemo } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';

export default function EditAssignmentPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <EditAssignmentPage.Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
      <BottomTab />
    </Page>
  );
}

EditAssignmentPage.Content = function Content() {
  const { studyId, assignmentId } = useIntegerParams(['studyId', 'assignmentId']);
  const { data: assignment } = useSuspenseQuery(assignmentQueries.detail(studyId, assignmentId));
  const toast = useToast();

  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentValue) => updateAssignment(studyId, assignmentId, values),

    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });

      queryClient.invalidateQueries({
        queryKey: assignmentQueries.detail(studyId, assignmentId).queryKey,
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },

    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return (
    <AssignmentForm
      submitLabel="과제 수정하기"
      onSubmit={mutate}
      isSubmitting={isPending}
      initialValues={{
        title: assignment.title,
        content: assignment.content,
        submissionMethod: assignment.submissionMethod,
        closeAt: assignment.closeAt,
      }}
      fieldErrors={fieldErrors}
    />
  );
};

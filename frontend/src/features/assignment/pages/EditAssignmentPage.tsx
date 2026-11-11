import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AssignmentFormFunnel from '../components/AssignmentFormFunnel';
import Main from '../../../shared/ui/Main';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import assignmentQueries from '../queries';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import useEditAssignment from '../hooks/useEditAssignment';

export default function EditAssignmentPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}

function Content() {
  const { studyId, assignmentId } = useIntegerParams(['studyId', 'assignmentId']);
  const { data: assignment } = useSuspenseQuery(assignmentQueries.detail(studyId, assignmentId));
  const {
    mutate: handleEditAssignment,
    isPending,
    fieldErrors,
  } = useEditAssignment({ studyId, assignmentId });

  return (
    <AssignmentFormFunnel
      key={assignmentId}
      studyId={studyId}
      mode="edit"
      onSubmit={handleEditAssignment}
      isSubmitting={isPending}
      initialAttachments={assignment.attachments}
      initialValues={{
        title: assignment.title,
        content: assignment.content,
        submissionMethod: assignment.submissionMethod,
        closeAt: assignment.closeAt,
        submitterIds: assignment.submitterIds,
        submissionVisibility: assignment.submissionVisibility,
        remindAts: assignment.remindAts,
      }}
      fieldErrors={fieldErrors}
    />
  );
}

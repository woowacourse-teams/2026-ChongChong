import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import useCreateAssignment from '../hooks/useCreateAssignment';
import AssignmentFormFunnel from '../components/AssignmentFormFunnel';

export default function CreateAssignmentPage() {
  const { studyId } = useIntegerParams(['studyId']);
  const { mutate, isPending, fieldErrors } = useCreateAssignment({ studyId });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentFormFunnel
          studyId={studyId}
          mode="create"
          onSubmit={mutate}
          isSubmitting={isPending}
          fieldErrors={fieldErrors}
        />
      </Main>
    </Page>
  );
}

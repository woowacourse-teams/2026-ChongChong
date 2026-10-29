import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import useCreateAssignment from '../hooks/useCreateAssignment';

export default function CreateAssignmentPage() {
  const { studyId } = useIntegerParams(['studyId']);

  const {
    mutate: handleCreateAssignment,
    isPending,
    fieldErrors,
  } = useCreateAssignment({ studyId });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm
          isSubmitting={isPending}
          submitLabel="과제 올리기"
          onSubmit={(body) => handleCreateAssignment(body)}
          fieldErrors={fieldErrors}
        />
      </Main>
      <BottomTab />
    </Page>
  );
}

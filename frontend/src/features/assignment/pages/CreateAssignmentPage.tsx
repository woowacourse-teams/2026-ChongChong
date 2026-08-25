import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';

export default function CreateAssignmentPage() {
  const onSubmit = () => {};

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm submitLabel="과제 올리기" onSubmit={onSubmit} />
      </Main>
      <BottomTab />
    </Page>
  );
}

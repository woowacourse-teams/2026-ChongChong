import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import AssignmentForm from '../components/AssignmentForm';
import Main from '../../../shared/ui/Main';
import { useSuspenseQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import assignmentQueries from '../queries';
import { AssignmentValue } from '../types';

export default function EditAssignmentPage() {
  const { studyId, assignmentId } = useParams();

  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(Number(studyId), Number(assignmentId)),
  );

  const onSubmit = (values: AssignmentValue) => {
    // 수정 API
    console.log(values);
  };

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <AssignmentForm
          submitLabel="과제 수정하기"
          onSubmit={onSubmit}
          initialValues={{
            title: assignment.title,
            content: assignment.content,
            submissionType: assignment.submissionType,
          }}
        />
      </Main>
      <BottomTab />
    </Page>
  );
}

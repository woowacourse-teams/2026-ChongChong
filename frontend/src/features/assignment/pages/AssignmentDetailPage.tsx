import { Suspense } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import LeaderAssignmentDetailContent from '../components/LeaderAssignmentDetailContent';
import MemberAssignmentDetailContent from '../components/MemberAssignmentDetailContent';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Main from '../../../shared/ui/Main';
import Loading from '../../../shared/ui/Loading';
import BottomTab from '../../../shared/widgets/BottomTab';
import ErrorContent from '../../../shared/ui/ErrorContent';
import studyQueries from '../../study/queries';

export default function AssignmentDetailPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <AssignmentDetailPage.Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
      <BottomTab />
    </Page>
  );
}

AssignmentDetailPage.Content = function Content() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? (
    <LeaderAssignmentDetailContent studyId={studyId} />
  ) : (
    <MemberAssignmentDetailContent studyId={studyId} />
  );
};

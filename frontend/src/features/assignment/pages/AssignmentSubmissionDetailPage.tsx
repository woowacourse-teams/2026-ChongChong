import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Loading from '../../../shared/ui/Loading';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import ErrorContent from '../../../shared/ui/ErrorContent';
import AssignmentSubmissionDetailContent from '../components/AssignmentSubmissionDetailContent';

export default function AssignmentSubmissionDetailPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <AssignmentSubmissionDetailContent />
          </Suspense>
        </ErrorBoundary>
      </Main>
      <BottomTab />
    </Page>
  );
}

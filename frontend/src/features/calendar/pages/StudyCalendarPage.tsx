import { Suspense } from 'react';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import StudyCalendar from '../components/StudyCalendar';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import Main from '../../../shared/ui/Main';

export default function StudyCalendarPage() {
  const { studyId } = useIntegerParams(['studyId']);

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>일정</TopHeader.Title>} />
      <ErrorBoundary
        fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
      >
        <Suspense fallback={<Loading />}>
          <Main>
            <StudyCalendar studyId={studyId} />
          </Main>
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

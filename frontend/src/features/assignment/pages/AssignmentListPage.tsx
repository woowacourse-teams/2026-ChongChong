import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import LeaderAssignmentListContent from '../components/LeaderAssignmentListContent';
import MemberAssignmentListContent from '../components/MemberAssignmentListContent';
import Page from '../../../shared/ui/Page';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import Loading from '../../../shared/ui/Loading';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import ErrorContent from '../../../shared/ui/ErrorContent';
import BottomTab from '../../../shared/widgets/BottomTab';
import { useSuspenseQuery } from '@tanstack/react-query';
import useStudyId from '../../study/hooks/useStudyId';
import studyQueries from '../../study/queries';

export default function AssignmentListPage() {
  return (
    <Page>
      <ErrorBoundary
        fallbackRender={({ error }) => (
          <>
            <TopHeader left={<PrevButton />} />
            <Main>
              <ErrorContent message={getErrorMessage(error)} />
            </Main>
          </>
        )}
      >
        <Suspense fallback={<Loading />}>
          <AssignmentListPage.Content />
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

AssignmentListPage.Content = function Content() {
  const { studyId } = useStudyId();

  const {
    data: { role, studyName, userName },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <>
      <TopHeader
        left={<PrevButton />}
        middle={
          <>
            <TopHeader.Title>{studyName}</TopHeader.Title>
            <TopHeader.Subtitle>
              {userName} · {role === 'LEADER' ? '리드' : '스터디원'}
            </TopHeader.Subtitle>
          </>
        }
      />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          {role === 'LEADER' ? (
            <LeaderAssignmentListContent studyId={studyId} />
          ) : (
            <MemberAssignmentListContent studyId={studyId} />
          )}
        </ErrorBoundary>
      </Main>
    </>
  );
};

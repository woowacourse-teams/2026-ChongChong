import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import Main from '../../../shared/ui/Main';
import BottomTab from '../../../shared/widgets/BottomTab';
import TopHeader from '../../../shared/ui/TopHeader';
import useStudyId from '../hooks/useStudyId';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import {
  LeaderStudyDetailContent,
  MemberStudyDetailContent,
} from '../components/StudyDetailContent';
import ErrorContent from '../../../shared/ui/ErrorContent';
import { Suspense } from 'react';
import Loading from '../../../shared/ui/Loading';

export default function StudyDetailPage() {
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
          <StudyDetailPage.Content />
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

StudyDetailPage.Content = function Content() {
  const { studyId } = useStudyId();
  const {
    data: { studyName, role, userName },
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
            <LeaderStudyDetailContent username={userName} />
          ) : (
            <MemberStudyDetailContent username={userName} />
          )}
        </ErrorBoundary>
      </Main>
    </>
  );
};

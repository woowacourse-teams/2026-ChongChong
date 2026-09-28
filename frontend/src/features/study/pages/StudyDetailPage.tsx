import { useSuspenseQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import studyQueries from '../queries';
import Main from '../../../shared/ui/Main';
import BottomTab from '../../../shared/widgets/BottomTab';
import TopHeader from '../../../shared/ui/TopHeader';
import useIntegerParam from '../../../shared/hooks/useIntegerParams';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import {
  LeaderStudyDetailContent,
  MemberStudyDetailContent,
} from '../components/StudyDetailContent';
import ErrorContent from '../../../shared/ui/ErrorContent';
import { usePostHog } from '@posthog/react';
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
  const { studyId } = useIntegerParam(['studyId']);
  const posthog = usePostHog();
  const {
    data: { studyName, role, userName },
  } = useSuspenseQuery(studyQueries.info(studyId));

  useEffect(() => {
    posthog.capture('study_detail_viewed', {
      study_id: studyId,
      study_role: role,
    });
  }, [posthog, studyId, role]);

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

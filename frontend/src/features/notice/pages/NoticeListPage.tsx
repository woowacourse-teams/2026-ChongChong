import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import LeaderNoticeListContent from '../components/LeaderNoticeListContent';
import MemberNoticeListContent from '../components/MemberNoticeListContent';
import Page from '../../../shared/ui/Page';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import Loading from '../../../shared/ui/Loading';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import ErrorContent from '../../../shared/ui/ErrorContent';
import BottomTab from '../../../shared/widgets/BottomTab';
import studyQueries from '../../study/queries';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeListPage() {
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
          <NoticeListPage.Content />
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

NoticeListPage.Content = function Content() {
  const { studyId } = useIntegerParams(['studyId']);
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
            <LeaderNoticeListContent studyId={studyId} />
          ) : (
            <MemberNoticeListContent studyId={studyId} />
          )}
        </ErrorBoundary>
      </Main>
    </>
  );
};

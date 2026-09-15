import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import LeaderNoticeDetailContent from '../components/LeaderNoticeDetailContent';
import MemberNoticeDetailContent from '../components/MemberNoticeDetailContent';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Main from '../../../shared/ui/Main';
import Loading from '../../../shared/ui/Loading';
import BottomTab from '../../../shared/widgets/BottomTab';
import ErrorContent from '../../../shared/ui/ErrorContent';
import studyQueries from '../../study/queries';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeDetailPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <NoticeDetailPage.Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
      <BottomTab />
    </Page>
  );
}

NoticeDetailPage.Content = function Content() {
  const { studyId, noticeId } = useIntegerParams(['studyId', 'noticeId']);

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? (
    <LeaderNoticeDetailContent studyId={studyId} noticeId={noticeId} />
  ) : (
    <MemberNoticeDetailContent studyId={studyId} noticeId={noticeId} />
  );
};

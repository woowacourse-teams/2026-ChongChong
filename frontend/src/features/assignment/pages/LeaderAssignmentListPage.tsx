import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import LeaderAssignmentListSection from '../components/LeaderAssignmentListSection';
import { Suspense } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import ErrorContent from '../../../shared/ui/ErrorContent';
import BottomTab from '../../../shared/ui/components/BottomTab';

const studyId = 1;

export default function LeaderAssignmentListPage() {
  return (
    <Page>
      <ErrorBoundary fallback={<ErrorContent message="오류가 발생했습니다." />}>
        <TopHeader
          left={<PrevButton />}
          middle={
            <>
              <TopHeader.Title>우테코 8기 FE 스터디</TopHeader.Title>
              <TopHeader.Subtitle>디움 · 리드</TopHeader.Subtitle>
            </>
          }
        />

        <Suspense fallback={<p>로딩중...</p>}>
          <Main>
            <LeaderAssignmentListSection studyId={studyId} />
          </Main>
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

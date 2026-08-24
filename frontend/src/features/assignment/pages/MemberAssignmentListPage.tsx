import Page from '../../../shared/ui/Page';
import { ErrorBoundary } from 'react-error-boundary';
import { Suspense } from 'react';
import ErrorState from '../../../shared/ui/ErrorState';
import TopHeader from '../../../shared/ui/TopHeader';
import MemberAssignmentListSection from '../components/MemberAssignmentListSectiont';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import BottomTab from '../../../shared/ui/components/BottomTab';

const studyId = 1;

export default function MemberAssignmentListPage() {
  return (
    <Page>
      <ErrorBoundary fallback={<ErrorState message="오류가 발생했습니다." />}>
        <TopHeader
          left={<PrevButton />}
          middle={
            <>
              <TopHeader.Title>우테코 8기 FE 스터디</TopHeader.Title>
              <TopHeader.Subtitle>디움 · 스터디원</TopHeader.Subtitle>
            </>
          }
        />
        <Suspense fallback={<p>로딩중입니다...</p>}>
          <Main>
            <MemberAssignmentListSection studyId={studyId} />
          </Main>
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

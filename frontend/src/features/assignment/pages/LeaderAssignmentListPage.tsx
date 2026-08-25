import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import LeaderAssignmentListSection from '../components/LeaderAssignmentListSection';
import { Suspense } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import ErrorContent from '../../../shared/ui/ErrorContent';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { useParams } from 'react-router';

interface Props {
  studyName: string;
  memberName: string;
}

export default function LeaderAssignmentListPage({ studyName, memberName }: Props) {
  const { studyId } = useParams();

  return (
    <Page>
      <ErrorBoundary fallback={<ErrorContent message="오류가 발생했습니다." />}>
        <TopHeader
          left={<PrevButton />}
          middle={
            <>
              <TopHeader.Title>{studyName}</TopHeader.Title>
              <TopHeader.Subtitle>{memberName} · 리드</TopHeader.Subtitle>
            </>
          }
        />

        <Suspense fallback={<p>로딩중...</p>}>
          <Main>
            <LeaderAssignmentListSection studyId={Number(studyId)} />
          </Main>
        </Suspense>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

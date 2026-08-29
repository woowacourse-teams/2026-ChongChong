import Page from '../../../shared/ui/Page';
import { ErrorBoundary } from 'react-error-boundary';
import { Suspense } from 'react';
import ErrorContent from '../../../shared/ui/ErrorContent';
import TopHeader from '../../../shared/ui/TopHeader';
import MemberAssignmentListSection from '../components/MemberAssignmentListSectiont';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import BottomTab from '../../../shared/widgets/BottomTab';
import { useParams } from 'react-router';
import Loading from '../../../shared/ui/Loading';

interface Props {
  studyName: string;
  userName: string;
}

export default function MemberAssignmentListPage({ studyName, userName }: Props) {
  const { studyId } = useParams();

  return (
    <Page>
      <ErrorBoundary fallback={<ErrorContent message="오류가 발생했습니다." />}>
        <TopHeader
          left={<PrevButton />}
          middle={
            <>
              <TopHeader.Title>{studyName}</TopHeader.Title>
              <TopHeader.Subtitle>{userName} · 스터디원</TopHeader.Subtitle>
            </>
          }
        />
        <Main>
          <Suspense fallback={<Loading />}>
            <MemberAssignmentListSection studyId={Number(studyId)} />
          </Suspense>
        </Main>
      </ErrorBoundary>
      <BottomTab />
    </Page>
  );
}

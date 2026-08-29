import { useParams } from 'react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import Main from '../../../shared/ui/Main';
import BottomTab from '../../../shared/ui/components/BottomTab';
import TopHeader from '../../../shared/ui/TopHeader';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import {
  LeaderStudyDetailContent,
  MemberStudyDetailContent,
} from '../components/StudyDetailContent';
import { Suspense } from 'react';
import Loading from '../../../shared/ui/Loading';

export default function StudyDetailPage() {
  const { studyId } = useParams();
  const {
    data: { studyName, role, userName },
  } = useSuspenseQuery(studyQueries.info(Number(studyId)));

  return (
    <Page>
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
        <Suspense fallback={<Loading />}>
          {role === 'LEADER' ? (
            <LeaderStudyDetailContent username={userName} />
          ) : (
            <MemberStudyDetailContent username={userName} />
          )}
        </Suspense>
      </Main>
      <BottomTab />
    </Page>
  );
}

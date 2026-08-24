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

export default function StudyDetailPage() {
  const { studyId } = useParams();
  const {
    data: { studyName, role, memberName },
  } = useSuspenseQuery(studyQueries.info(studyId as string));

  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={
          <>
            <TopHeader.Title>{studyName}</TopHeader.Title>
            <TopHeader.Subtitle>
              {memberName} · {role === 'LEADER' ? '리드' : '스터디원'}
            </TopHeader.Subtitle>
          </>
        }
      />
      <Main>
        {role === 'LEADER' ? (
          <LeaderStudyDetailContent username={memberName} />
        ) : (
          <MemberStudyDetailContent username={memberName} />
        )}
      </Main>
      <BottomTab />
    </Page>
  );
}

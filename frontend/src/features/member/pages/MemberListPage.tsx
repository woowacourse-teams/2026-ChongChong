import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import MemberListContent from '../components/MemberListContent';
import { StudyRole } from '../types';
import BottomTab from '../../../shared/ui/components/BottomTab';

export default function MemberListPage() {
  const role: StudyRole = 'LEADER';

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>멤버</TopHeader.Title>} />
      <Main>{role === 'LEADER' ? <MemberListContent.Leader /> : <MemberListContent.Member />}</Main>
      <BottomTab />
    </Page>
  );
}

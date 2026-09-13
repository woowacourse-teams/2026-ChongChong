import TopHeader from '../../../shared/ui/TopHeader';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AccountMenuSection from '../components/AccountMenuSection';
import ProfileSection from '../components/ProfileSection';
import Main from '../../../shared/ui/Main';

export default function MyPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>마이페이지</TopHeader.Title>} />

      <Main>
        <ProfileSection />
        <AccountMenuSection />
      </Main>
    </Page>
  );
}

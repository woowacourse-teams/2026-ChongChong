import TopHeader from '../../../shared/ui/TopHeader';
import { tokens } from '../../../styles/global';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import Page from '../../../shared/ui/Page';
import headerIcon from '../../../shared/assets/icons/header-icon.svg';
import StudyForm from '../components/StudyForm';

export default function NewStudyPage() {
  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<div>스터디 만들기</div>}
        right={<a href="#">My</a>}
      />
      <Main
        css={{ padding: `0 ${tokens.spacing[4]} ${tokens.layout.gutter} ${tokens.spacing[5]}` }}
      >
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: `56px 0`,
          }}
        >
          <img src={headerIcon} alt="" css={{ width: '70px', height: '70px' }} />
        </div>
        <StudyForm />
      </Main>
    </Page>
  );
}

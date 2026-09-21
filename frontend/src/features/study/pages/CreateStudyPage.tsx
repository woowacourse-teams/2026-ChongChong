import { tokens } from '../../../styles/global';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Page from '../../../shared/ui/Page';
import headerIcon from '../../../shared/assets/icons/header-icon.svg';
import useCreateStudy from '../hooks/useCreateStudy';

import StudyForm from '../components/StudyForm';

export default function NewStudyPage() {
  const { mutate: createStudy, isPending, fieldErrors } = useCreateStudy();

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<div>스터디 만들기</div>} />
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
        <StudyForm onSubmit={createStudy} isSubmitting={isPending} fieldErrors={fieldErrors} />
      </Main>
    </Page>
  );
}

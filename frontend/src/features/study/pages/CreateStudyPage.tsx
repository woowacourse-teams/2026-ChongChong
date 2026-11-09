import { tokens } from '../../../styles/global';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Page from '../../../shared/ui/Page';
import useCreateStudy from '../hooks/useCreateStudy';

import StudyForm from '../components/StudyForm';

export default function NewStudyPage() {
  const { mutate: createStudy, isPending, fieldErrors } = useCreateStudy();

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<div>스터디 만들기</div>} />
      <Main css={{ padding: `0 ${tokens.layout.gutter} ${tokens.spacing[5]}` }}>
        <StudyForm
          showVisibility
          submitLabel={'스터디 만들기'}
          onSubmit={createStudy}
          isSubmitting={isPending}
          fieldErrors={fieldErrors}
        />
      </Main>
    </Page>
  );
}

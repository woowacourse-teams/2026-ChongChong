import { useSuspenseQuery } from '@tanstack/react-query';
import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import ErrorContent from '../../../shared/ui/ErrorContent';
import StudyForm from '../components/StudyForm';
import useEditStudy from '../hooks/useEditStudy';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import headerIcon from '../../../shared/assets/icons/header-icon.svg';
import studyQueries from '../queries';

export default function StudyEditPage() {
  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<TopHeader.Title>스터디 정보 수정</TopHeader.Title>}
      />
      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense>
            <Content />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}

function Content() {
  const { studyId } = useIntegerParams(['studyId']);
  const { mutate: editStudy, isPending, fieldErrors } = useEditStudy(studyId);
  const {
    data: { studyName, description },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <>
      {/* // TODO: 스터디 프로필 이미지 필드입니다, 프로필 이미지가 추가되면 Form 내부 필드로 정의합니다 */}
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
      <StudyForm
        submitLabel={'스터디 수정하기'}
        onSubmit={editStudy}
        isSubmitting={isPending}
        fieldErrors={fieldErrors}
        initialValues={{ name: studyName, description: description }}
      />
    </>
  );
}

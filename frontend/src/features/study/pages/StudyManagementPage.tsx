import { useSuspenseQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { CSSProperties, Suspense } from 'react';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import StudyIcon from '../../../shared/assets/icons/header-icon.svg';
import { tokens, typography } from '../../../styles/global';
import studyQueries from '../queries';
import { StudyLeaderManagementList, StudyMemberManagementList } from '../components/ManagementList';
import useIntegerParam from '../../../shared/hooks/useIntegerParams';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';

const SectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  padding: `${tokens.spacing[5]} 0`,
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const StudyProfileTitleStyle = {
  ...typography.title,
  margin: tokens.spacing[3],
} satisfies CSSProperties;

const StudyProfileSubTitleStyle = {
  ...typography.paragraph,
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function StudyManagementPage() {
  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<TopHeader.Title>스터디 참여하기</TopHeader.Title>}
      />
      <ErrorBoundary
        fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
      >
        <Suspense fallback={<Loading />}>
          <Main>
            <StudyManagementPage.Content />
          </Main>
        </Suspense>
      </ErrorBoundary>
    </Page>
  );
}

StudyManagementPage.Content = function Content() {
  const { studyId } = useIntegerParam(['studyId']);

  const {
    data: { studyName, role, description },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <>
      <section aria-labelledby="study-name" css={SectionStyle}>
        <img src={StudyIcon} alt="" width={68} height={68} />
        <h2 id="study-name" css={StudyProfileTitleStyle}>
          {studyName}
        </h2>
        <p css={StudyProfileSubTitleStyle}>{description}</p>
      </section>
      <section>
        {role === 'LEADER' ? (
          <StudyLeaderManagementList studyId={studyId} />
        ) : (
          <StudyMemberManagementList studyId={studyId} />
        )}
      </section>
    </>
  );
};

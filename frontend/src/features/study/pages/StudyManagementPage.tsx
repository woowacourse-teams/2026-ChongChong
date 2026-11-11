import StudyAvatar from '../components/StudyAvatar';
import { useSuspenseQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { CSSProperties, Suspense } from 'react';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import Main from '../../../shared/ui/Main';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import { tokens } from '../../../styles/global';
import studyQueries from '../queries';
import { Heading, Paragraph } from '../../../shared/ui/Typography';
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
  minHeight: 170,
  padding: `19px ${tokens.spacing[5]} 18px`,
  textAlign: 'center',
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

const StudyProfileTitleStyle = {
  margin: `${tokens.spacing[3]} 0 ${tokens.spacing[1]}`,
} satisfies CSSProperties;

const StudyProfileSubTitleStyle = {
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function StudyManagementPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>스터디 관리</TopHeader.Title>} />
      <ErrorBoundary
        fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
      >
        <Suspense fallback={<Loading />}>
          <Content />
        </Suspense>
      </ErrorBoundary>
    </Page>
  );
}

function Content() {
  const { studyId } = useIntegerParam(['studyId']);

  const {
    data: { studyName, role, description, imageUrl },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <Main css={{ paddingTop: 0 }}>
      <section aria-labelledby="study-name" css={SectionStyle}>
        <StudyAvatar
          imageUrl={imageUrl}
          size={68}
          variant="management"
          alt={`${studyName} 프로필`}
        />
        <Heading variant="title" level={2} id="study-name" css={StudyProfileTitleStyle}>
          {studyName}
        </Heading>
        <Paragraph variant="paragraph" css={StudyProfileSubTitleStyle}>
          {description}
        </Paragraph>
      </section>
      <section>
        {role === 'LEADER' ? (
          <StudyLeaderManagementList studyId={studyId} />
        ) : (
          <StudyMemberManagementList studyId={studyId} />
        )}
      </section>
    </Main>
  );
}

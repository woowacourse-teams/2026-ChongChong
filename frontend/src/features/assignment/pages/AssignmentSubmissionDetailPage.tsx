import type { CSSProperties } from 'react';
import { useParams } from 'react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import linkIcon from '../../../shared/assets/link-green.svg';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { formatSubmittedAt } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import InfoCard from '../components/InfoCard';
import assignmentQueries from '../queries';
import BottomTab from '../../../shared/ui/components/BottomTab';

const articleStyle = {
  display: 'flex',
  flexDirection: 'column',
  paddingTop: tokens.spacing[8],
} satisfies CSSProperties;

const submitterStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

const profileStyle = {
  width: '28px',
  height: '28px',
  flex: '0 0 28px',
} satisfies CSSProperties;

const submitterInfoStyle = {
  display: 'flex',
  minWidth: 0,
  flexDirection: 'column',
} satisfies CSSProperties;

const submitterNameStyle = {
  ...typography.body,
  margin: 0,
  overflow: 'hidden',
  color: tokens.text.primary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const submittedAtStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const cardListStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[2],
} satisfies CSSProperties;

const linkStyle = {
  display: 'block',
  color: 'inherit',
  overflowWrap: 'anywhere',
} satisfies CSSProperties;

export default function AssignmentSubmissionDetailPage() {
  const { studyId, assignmentId, submissionId } = useParams();

  const { data: submission } = useSuspenseQuery(
    assignmentQueries.submissionDetail(Number(studyId), Number(assignmentId), Number(submissionId)),
  );

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />

      <Main>
        <article css={articleStyle} aria-labelledby="submission-member-name">
          <header css={submitterStyle}>
            <img src={profileIcon} alt="" aria-hidden="true" css={profileStyle} />

            <div css={submitterInfoStyle}>
              <h2 id="submission-member-name" css={submitterNameStyle}>
                {submission.name}
              </h2>
              <time css={submittedAtStyle} dateTime={submission.createdAt}>
                {formatSubmittedAt(submission.createdAt)}
              </time>
            </div>
          </header>

          <div css={cardListStyle}>
            <InfoCard icon={assignmentIcon} title="내용">
              {submission.content}
            </InfoCard>

            {submission.link && (
              <InfoCard icon={linkIcon} title="링크">
                <a css={linkStyle} href={submission.link} target="_blank" rel="noreferrer">
                  {submission.link}
                </a>
              </InfoCard>
            )}
          </div>
        </article>
      </Main>
      <BottomTab />
    </Page>
  );
}

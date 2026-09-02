import type { CSSProperties } from 'react';
import { Link, useParams } from 'react-router';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import List from '../../../shared/ui/List';
import { tokens, typography } from '../../../styles/global';
import type { Submission } from '../types';
import { formatSubmittedAt } from '../../../shared/utils/formatDate';
import { usePostHog } from '@posthog/react';

interface Props {
  submissions: Submission[];
}

const sectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const titleStyle = {
  ...typography.title,
  margin: `0 0 ${tokens.spacing[3]}`,
  color: tokens.text.primary,
} satisfies CSSProperties;

const itemStyle = {
  display: 'flex',
  minHeight: '77px',
  alignItems: 'center',
  padding: tokens.spacing[5],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
} satisfies CSSProperties;

const profileStyle = {
  width: '28px',
  height: '28px',
  flex: '0 0 28px',
} satisfies CSSProperties;

const memberStyle = {
  display: 'flex',
  minWidth: 0,
  flexDirection: 'column',
  marginLeft: tokens.spacing[3],
} satisfies CSSProperties;

const nameStyle = {
  ...typography.body,
  overflow: 'hidden',
  color: tokens.text.primary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const dateStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const detailLinkStyle = {
  ...typography.body,
  flex: '0 0 auto',
  marginLeft: 'auto',
  color: tokens.text.brand,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

export default function SubmissionList({ submissions }: Props) {
  const { studyId, assignmentId } = useParams();
  const posthog = usePostHog();

  const handleShowDetail = () => {
    posthog?.capture('submission_detail', {
      location: 'assignment_detail_page',
    });
  };

  return (
    <section css={sectionStyle} aria-labelledby="submission-list-title">
      <h2 id="submission-list-title" css={titleStyle}>
        제출 내역
      </h2>

      <List>
        {submissions.map((submission) => (
          <List.Item key={submission.id} css={itemStyle} onClick={handleShowDetail}>
            <img src={profileIcon} alt="" aria-hidden="true" css={profileStyle} />

            <div css={memberStyle}>
              <span css={nameStyle}>{submission.name}</span>
              <time css={dateStyle} dateTime={submission.createdAt}>
                {formatSubmittedAt(submission.createdAt)}
              </time>
            </div>

            <Link
              css={detailLinkStyle}
              to={`/studies/${studyId}/assignments/${assignmentId}/submissions/${submission.id}`}
              aria-label={`${submission.name} 제출 내역 상세 보기`}
            >
              상세 보기
            </Link>
          </List.Item>
        ))}
      </List>
    </section>
  );
}

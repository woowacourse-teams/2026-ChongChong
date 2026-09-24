import type { CSSProperties } from 'react';
import { Link } from 'react-router';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import { formatSubmittedAt } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import type { Submission, UserAssignmentSubmitDetail } from '../types';

interface Props {
  studyId: number;
  assignmentId: number;
  submission: UserAssignmentSubmitDetail;
  member?: Submission;
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

const emptyStyle = {
  ...typography.footnote,
  margin: `${tokens.spacing[5]} 0`,
  color: tokens.text.muted,
  textAlign: 'center',
} satisfies CSSProperties;

const cardStyle = {
  display: 'flex',
  minHeight: 72,
  alignItems: 'center',
  padding: tokens.spacing[4],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
} satisfies CSSProperties;

const profileStyle = {
  width: 28,
  height: 28,
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
  color: tokens.text.primary,
} satisfies CSSProperties;

const dateStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const detailLinkStyle = {
  ...typography.body,
  flex: '0 0 auto',
  marginLeft: 'auto',
  color: tokens.text.muted,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

export default function MySubmissionStatus({ studyId, assignmentId, submission, member }: Props) {
  if (submission.submissionStatus === 'NOT_ASSIGNED') return null;

  return (
    <section css={sectionStyle} aria-labelledby="my-submission-status-title">
      <h2 id="my-submission-status-title" css={titleStyle}>
        내 제출
      </h2>

      {submission.submissionStatus === 'NOT_SUBMITTED' ? (
        <p css={emptyStyle}>아직 과제를 제출하지 않았어요</p>
      ) : (
        <div css={cardStyle}>
          <img
            src={member?.profileImage ?? profileIcon}
            alt=""
            aria-hidden="true"
            css={profileStyle}
          />
          <div css={memberStyle}>
            <span css={nameStyle}>{member ? `${member.name} (나)` : '나'}</span>
            <time css={dateStyle} dateTime={submission.createdAt}>
              {formatSubmittedAt(submission.createdAt)}
            </time>
          </div>
          <Link
            css={detailLinkStyle}
            to={`/studies/${studyId}/assignments/${assignmentId}/submissions/${submission.submissionId}`}
          >
            상세 보기
          </Link>
        </div>
      )}
    </section>
  );
}

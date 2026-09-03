import type { CSSProperties } from 'react';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import { formatDateToString } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import type { AssignmentDetail } from '../types';
import InfoCard from './InfoCard';

interface Props {
  assignment: AssignmentDetail;
}

const articleStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[3],
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const headerStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

const titleStyle = {
  ...typography.headline,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const deadlineStyle = {
  ...typography.footnote,
  margin: 0,
  color: tokens.text.brand,
} satisfies CSSProperties;

export default function AssignmentArticle({ assignment }: Props) {
  return (
    <article css={articleStyle} aria-labelledby="assignment-title">
      <header css={headerStyle}>
        <h2 id="assignment-title" css={titleStyle}>
          {assignment.title}
        </h2>
        <time css={deadlineStyle} dateTime={assignment.closeAt}>
          {formatDateToString(assignment.closeAt)} 마감
        </time>
      </header>

      <InfoCard icon={assignmentIcon} title="과제 내용">
        {assignment.content}
      </InfoCard>

      <InfoCard icon={assignmentIcon} title="제출 방법">
        {assignment.submissionMethod}
      </InfoCard>
    </article>
  );
}

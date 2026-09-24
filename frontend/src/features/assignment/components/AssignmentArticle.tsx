import type { CSSProperties } from 'react';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import { tokens } from '../../../styles/global';
import type { AssignmentDetail } from '../types';
import InfoCard from './InfoCard';

interface Props {
  assignment: AssignmentDetail;
}

const articleStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

const detailsStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

export default function AssignmentArticle({ assignment }: Props) {
  return (
    <article css={articleStyle} aria-label="과제 상세">
      <div css={detailsStyle}>
        <InfoCard icon={assignmentIcon} title="과제 내용">
          {assignment.content}
        </InfoCard>

        <InfoCard icon={assignmentIcon} title="제출 방법">
          {assignment.submissionMethod}
        </InfoCard>
      </div>
    </article>
  );
}

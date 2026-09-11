import type { CSSProperties } from 'react';
import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import linkIcon from '../../../shared/assets/link-green.svg';
import Button from '../../../shared/ui/Button';
import { formatSubmittedAt } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import { updateAssignmentSubmission } from '../api';
import assignmentQueries from '../queries';
import type { AssignmentSubmissionValue, UserAssignmentSubmitDetail } from '../types';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';
import InfoCard from './InfoCard';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

type SubmittedAssignment = Extract<UserAssignmentSubmitDetail, { submitted: true }>;

interface Props {
  assignmentId: number;
  studyId: number;
  submission: SubmittedAssignment;
}

const sectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const sectionTitleStyle = {
  ...typography.title,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const submittedAtStyle = {
  ...typography.footnote,
  marginTop: tokens.spacing[1],
  color: tokens.text.placeholder,
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

const editButtonStyle = {
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

export default function CompletedAssignmentSubmission({
  assignmentId,
  studyId,
  submission,
}: Props) {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      updateAssignmentSubmission(studyId, assignmentId, submission.submissionId, values),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: assignmentQueries.mySubmission(studyId, assignmentId).queryKey,
        exact: true,
      });

      setIsEditing(false);
    },

    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return isEditing ? (
    <AssignmentSubmissionForm
      key={`${studyId}-${assignmentId}-${submission.submissionId}`}
      initialValues={{ content: submission.content, link: submission.link }}
      isSubmitting={isPending}
      submitLabel="수정하기"
      onSubmit={mutate}
      fieldErrors={fieldErrors}
    />
  ) : (
    <section css={sectionStyle} aria-labelledby="my-submission-title">
      <h2 id="my-submission-title" css={sectionTitleStyle}>
        내 제출
      </h2>
      <time css={submittedAtStyle} dateTime={submission.createdAt}>
        {formatSubmittedAt(submission.createdAt)}
      </time>

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

      <Button
        type="button"
        variant="brandSolid"
        size="large"
        css={editButtonStyle}
        onClick={() => setIsEditing(true)}
      >
        편집하기
      </Button>
    </section>
  );
}

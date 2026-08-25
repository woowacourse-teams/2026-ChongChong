import type { CSSProperties } from 'react';
import { useState } from 'react';
import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import linkIcon from '../../../shared/assets/link-green.svg';
import Button from '../../../shared/ui/Button';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import { formatSubmittedAt } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import { createAssignmentSubmission, updateAssignmentSubmission } from '../api';
import AssignmentArticle from '../components/AssignmentArticle';
import AssignmentSubmissionForm from '../components/AssignmentSubmissionForm';
import InfoCard from '../components/InfoCard';
import assignmentQueries from '../queries';
import type { AssignmentDetail, AssignmentSubmissionValue, SubmissionDetail } from '../types';

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

interface CompletedSubmissionProps {
  assignment: AssignmentDetail;
  submissionId: number;
}

function CompletedSubmission({ assignment, submissionId }: CompletedSubmissionProps) {
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const numericStudyId = Number(studyId);
  const numericAssignmentId = Number(assignmentId);

  const { data: submission } = useSuspenseQuery(
    assignmentQueries.submissionDetail(numericStudyId, numericAssignmentId, submissionId),
  );

  const updateMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      updateAssignmentSubmission(numericStudyId, numericAssignmentId, submissionId, values),
    onSuccess: (updatedSubmission) => {
      queryClient.setQueryData(
        assignmentQueries.submissionDetail(numericStudyId, numericAssignmentId, submissionId)
          .queryKey,
        updatedSubmission,
      );
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(numericStudyId) });
      setIsEditing(false);
    },
  });

  return (
    <>
      <AssignmentArticle assignment={assignment} />

      {isEditing ? (
        <AssignmentSubmissionForm
          initialValues={{ content: submission.content, link: submission.link }}
          isSubmitting={updateMutation.isPending}
          submitLabel="수정하기"
          onSubmit={updateMutation.mutate}
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
      )}
    </>
  );
}

export default function MemberAssignmentDetailPage() {
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();
  const numericStudyId = Number(studyId);
  const numericAssignmentId = Number(assignmentId);

  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(numericStudyId, numericAssignmentId),
  );

  const createMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(numericStudyId, numericAssignmentId, values),
    onSuccess: (submission: SubmissionDetail) => {
      queryClient.setQueryData(
        assignmentQueries.detail(numericStudyId, numericAssignmentId).queryKey,
        { ...assignment, submissionId: submission.id },
      );
      queryClient.setQueryData(
        assignmentQueries.submissionDetail(numericStudyId, numericAssignmentId, submission.id)
          .queryKey,
        submission,
      );
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(numericStudyId) });
    },
  });

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />

      <Main css={{ paddingTop: 0 }}>
        {assignment.submissionId ? (
          <CompletedSubmission assignment={assignment} submissionId={assignment.submissionId} />
        ) : (
          <>
            <AssignmentArticle assignment={assignment} />
            <AssignmentSubmissionForm
              isSubmitting={createMutation.isPending}
              onSubmit={createMutation.mutate}
            />
          </>
        )}
      </Main>
    </Page>
  );
}

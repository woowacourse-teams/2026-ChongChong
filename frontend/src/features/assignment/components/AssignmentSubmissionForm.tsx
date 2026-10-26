import { useState } from 'react';
import AttachmentField from '../../../shared/widgets/AttachmentField';
import { useAttachments } from '../../../shared/modules/files/useAttachments';
import type { Attachment } from '../../../shared/modules/files/types';
import type { CSSProperties, SubmitEventHandler } from 'react';
import { useInputState } from '../../../shared/hooks/useInputState';
import Button from '../../../shared/ui/Button';
import InputField from '../../../shared/widgets/InputField';
import TextAreaField from '../../../shared/widgets/TextAreaField';
import { tokens, typography } from '../../../styles/global';
import { ASSIGNMENT_SUBMISSION_CONTENT, ASSIGNMENT_SUBMISSION_LINK } from '../constants';
import type { AssignmentSubmissionValue } from '../types';
import { usePostHog } from '@posthog/react';

interface Props {
  initialValues?: AssignmentSubmissionValue;
  initialAttachments?: Attachment[];
  isSubmitting?: boolean;
  submitLabel?: string;
  onSubmit: (values: AssignmentSubmissionValue) => void;
  fieldErrors?: Partial<Record<'content' | 'link' | 'attachmentFileIds', string>>;
}

const emptyValues: AssignmentSubmissionValue = {
  content: '',
  link: '',
};

const sectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const titleStyle = {
  ...typography.title,
  margin: `0 0 ${tokens.spacing[4]}`,
  color: tokens.text.primary,
} satisfies CSSProperties;

const formStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

const submitButtonStyle = {
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

export default function AssignmentSubmissionForm({
  initialValues = emptyValues,
  initialAttachments,
  isSubmitting = false,
  submitLabel = '제출하기',
  fieldErrors = {},
  onSubmit,
}: Props) {
  const attachments = useAttachments({
    initialAttachments,
    kind: 'file',
    contentType: 'submission',
    mode: initialValues === emptyValues ? 'create' : 'edit',
  });
  const [submissionError, setSubmissionError] = useState<string>();
  const [content, handleContentChange] = useInputState(
    initialValues.content ?? '',
    (value, prevValue) => {
      if (value.length > ASSIGNMENT_SUBMISSION_CONTENT.length) return prevValue;
      return value;
    },
  );
  const [link, handleLinkChange] = useInputState(initialValues.link ?? '', (value, prevValue) => {
    if (value.length > ASSIGNMENT_SUBMISSION_LINK.length) return prevValue;
    return value;
  });
  const posthog = usePostHog();

  const submitAssignment: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    if (isSubmitting || !attachments.isReady) return;
    if (!content.trim() && !link.trim() && !attachments.attachmentFileIds.length) {
      setSubmissionError('내용, 링크, 파일 중 하나 이상을 입력해주세요.');
      return;
    }
    setSubmissionError(undefined);

    posthog?.capture('assignment_submission_submitted', {
      location: 'assignment_submission_page',
    });

    onSubmit({
      content,
      attachmentFileIds: attachments.attachmentFileIds,
      link: link.trim(),
    });
  };

  return (
    <section css={sectionStyle} aria-labelledby="assignment-submission-title">
      <h2 id="assignment-submission-title" css={titleStyle}>
        내 제출
      </h2>

      <form css={formStyle} onSubmit={submitAssignment}>
        <TextAreaField
          id="assignment-submission-content"
          name="content"
          label="내용"
          value={content}
          onChange={handleContentChange}
          maxLength={ASSIGNMENT_SUBMISSION_CONTENT.length}
          placeholder="과제 내용을 입력해주세요"
          errorText={fieldErrors.content}
          testId="assignment-submission-content-field"
        />
        <InputField
          id="assignment-submission-link"
          name="link"
          label="링크"
          type="text"
          value={link}
          onChange={handleLinkChange}
          maxLength={ASSIGNMENT_SUBMISSION_LINK.length}
          placeholder="https://"
          errorText={fieldErrors.link}
        />

        <AttachmentField
          showImagePreviews
          id="assignment-submission-files"
          label="파일 업로드"
          items={attachments.items}
          onSelect={attachments.addFiles}
          onRemove={attachments.removeFile}
          disabled={isSubmitting}
          errorText={attachments.error ?? fieldErrors.attachmentFileIds}
        />
        {submissionError && (
          <p role="alert" css={{ color: tokens.text.critical }}>
            {submissionError}
          </p>
        )}
        <Button
          type="submit"
          variant="brandSolid"
          size="large"
          disabled={isSubmitting || !attachments.isReady}
          css={submitButtonStyle}
        >
          {isSubmitting ? '저장 중...' : submitLabel}
        </Button>
      </form>
    </section>
  );
}

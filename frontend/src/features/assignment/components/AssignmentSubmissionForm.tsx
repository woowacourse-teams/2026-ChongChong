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
  isSubmitting?: boolean;
  submitLabel?: string;
  onSubmit: (values: AssignmentSubmissionValue) => void;
  fieldErrors?: Partial<Record<'content' | 'link', string>>;
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
  isSubmitting = false,
  submitLabel = '제출하기',
  fieldErrors = {},
  onSubmit,
}: Props) {
  const [content, handleContentChange] = useInputState(initialValues.content, (value) =>
    value.slice(0, ASSIGNMENT_SUBMISSION_CONTENT.length),
  );
  const [link, handleLinkChange] = useInputState(initialValues.link ?? '', (value) =>
    value.slice(0, ASSIGNMENT_SUBMISSION_LINK.length),
  );
  const posthog = usePostHog();

  const submitAssignment: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    if (isSubmitting) return;

    posthog?.capture('assignment_submission_submitted', {
      location: 'assignment_submission_page',
    });

    onSubmit({
      content,
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
          isRequired
        />
        <InputField
          id="assignment-submission-link"
          name="link"
          label="링크"
          type="url"
          value={link}
          onChange={handleLinkChange}
          maxLength={ASSIGNMENT_SUBMISSION_LINK.length}
          placeholder="https://"
          errorText={fieldErrors.link}
        />

        <Button
          type="submit"
          variant="brandSolid"
          size="large"
          disabled={isSubmitting}
          css={submitButtonStyle}
        >
          {isSubmitting ? '저장 중...' : submitLabel}
        </Button>
      </form>
    </section>
  );
}

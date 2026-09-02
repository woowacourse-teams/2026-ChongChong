import type { CSSProperties, SubmitEventHandler } from 'react';
import { useState } from 'react';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import { tokens, typography } from '../../../styles/global';
import type { AssignmentSubmissionValue } from '../types';
import { usePostHog } from '@posthog/react';

interface Props {
  initialValues?: AssignmentSubmissionValue;
  isSubmitting?: boolean;
  submitLabel?: string;
  onSubmit: (values: AssignmentSubmissionValue) => void;
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
  onSubmit,
}: Props) {
  const [content, setContent] = useState(initialValues.content);
  const [link, setLink] = useState(initialValues.link ?? '');
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
        <Field id="assignment-submission-content" label="내용" isRequired>
          <TextArea
            id="assignment-submission-content"
            name="content"
            value={content}
            required
            onChange={(event) => setContent(event.target.value)}
            placeholder="과제 내용을 입력해주세요"
          />
        </Field>

        <Field id="assignment-submission-link" label="링크">
          <Input
            id="assignment-submission-link"
            name="link"
            type="url"
            value={link}
            onChange={(event) => setLink(event.target.value)}
            placeholder="https://"
          />
        </Field>

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

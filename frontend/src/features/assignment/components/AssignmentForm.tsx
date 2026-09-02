import { CSSProperties, useState, useMemo } from 'react';
import { ValidationError } from '../../../shared/api/error';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import { tokens } from '../../../styles/global';
import { AssignmentValue } from '../types';
import { formatDeadline, toLocalDateTime } from '../../../shared/utils/formatDate';

const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

interface AssignmentFormProps {
  initialValues?: AssignmentValue;
  submitLabel: string;
  isSubmitting?: boolean;
  onSubmit: (values: AssignmentValue) => void;
  error?: Error | null;
}

const emptyValues = {
  title: '',
  content: '',
  submissionMethod: '',
  closeAt: '',
};

export default function AssignmentForm({
  initialValues = emptyValues,
  submitLabel,
  isSubmitting = false,
  onSubmit,
  error,
}: AssignmentFormProps) {
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [submissionMethod, setsubmissionMethod] = useState(initialValues.submissionMethod);
  const [closeAt, setCloseAt] = useState(initialValues.closeAt);

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting) return;

    onSubmit({ title, content, submissionMethod, closeAt });
  }

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return (
    <form css={formStyle} onSubmit={handleSubmit}>
      <Field
        id="assignment-title"
        label="제목"
        isRequired
        isError={Boolean(fieldErrors.title)}
        errorText={fieldErrors.title}
      >
        <Input
          id="assignment-title"
          name="title"
          value={title}
          autoFocus
          onChange={(event) => setTitle(event.target.value)}
          maxLength={20}
          placeholder="제목을 입력해주세요"
        />
      </Field>

      <Field
        id="assignment-content"
        label="내용"
        isRequired
        isError={Boolean(fieldErrors.content)}
        errorText={fieldErrors.content}
      >
        <TextArea
          id="assignment-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          maxLength={10000}
          onChange={(event) => setContent(event.target.value)}
        />
      </Field>

      <Field
        id="submit-method"
        label="제출 방법"
        isRequired
        isError={Boolean(fieldErrors.submissionMethod)}
        errorText={fieldErrors.submissionMethod}
      >
        <Input
          id="submit-method"
          name="method"
          value={submissionMethod}
          autoFocus
          onChange={(event) => setsubmissionMethod(event.target.value)}
          placeholder="제출 방법을 입력해주세요"
        />
      </Field>

      <Field
        id="assignment-close-at"
        label="마감 시각"
        isRequired
        isError={Boolean(fieldErrors.closeAt)}
        errorText={fieldErrors.closeAt}
      >
        <DateTimePicker
          id="assignment-close-at"
          title="마감 시각 설정"
          value={closeAt ? new Date(closeAt) : undefined}
          triggerLabel={closeAt ? formatDeadline(closeAt) : '마감 시각 설정'}
          triggerVariant="neutralOutline"
          onChange={(value) => {
            setCloseAt(toLocalDateTime(value));
          }}
        />
      </Field>

      <Button
        type="submit"
        variant="brandSolid"
        size="large"
        disabled={isSubmitting}
        css={{ marginTop: tokens.spacing[8] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}

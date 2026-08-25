import { CSSProperties, useState } from 'react';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import { tokens } from '../../../styles/global';
import { AssignmentValue } from '../types';

const formStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

interface AssignmentFormProps {
  initialValues?: AssignmentValue;
  submitLabel: string;
  onSubmit: (values: AssignmentValue) => void;
}

const emptyValues = {
  title: '',
  content: '',
  submissionType: '',
  closeAt: '',
};

function formatDateTime(value: Date) {
  return `${value.getFullYear()}년 ${value.getMonth() + 1}월 ${value.getDate()}일 ${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}`;
}

function toLocalDateTime(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const date = String(value.getDate()).padStart(2, '0');
  const hours = String(value.getHours()).padStart(2, '0');
  const minutes = String(value.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${date}T${hours}:${minutes}:00`;
}

export default function AssignmentForm({
  initialValues = emptyValues,
  submitLabel,
  onSubmit,
}: AssignmentFormProps) {
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [submissionType, setSubmissionType] = useState(initialValues.submissionType);
  const [closeAt, setCloseAt] = useState(initialValues.closeAt);
  const [isCloseAtError, setIsCloseAtError] = useState(false);

  return (
    <form
      css={formStyle}
      onSubmit={(event) => {
        event.preventDefault();
        if (!closeAt) {
          setIsCloseAtError(true);
          return;
        }

        onSubmit({ title, content, submissionType, closeAt });
      }}
    >
      <Field id="assignment-title" label="제목" isRequired>
        <Input
          id="assignment-title"
          name="title"
          value={title}
          autoFocus
          required
          onChange={(event) => setTitle(event.target.value)}
          placeholder="제목을 입력해주세요"
        />
      </Field>

      <Field id="assignment-content" label="내용" isRequired>
        <TextArea
          id="assignment-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          required
          onChange={(event) => setContent(event.target.value)}
        />
      </Field>

      <Field id="submit-method" label="제출 방법" isRequired>
        <Input
          id="submit-method"
          name="method"
          value={submissionType}
          autoFocus
          required
          onChange={(event) => setSubmissionType(event.target.value)}
          placeholder="제출 방법을 입력해주세요"
        />
      </Field>

      <Field
        id="assignment-close-at"
        label="마감 시각"
        isRequired
        isError={isCloseAtError}
        errorText="마감 시각을 설정해주세요"
      >
        <DateTimePicker
          id="assignment-close-at"
          title="마감 시각 설정"
          value={closeAt ? new Date(closeAt) : undefined}
          triggerLabel={closeAt ? formatDateTime(new Date(closeAt)) : '마감 시각 설정'}
          triggerVariant="neutralOutline"
          onChange={(value) => {
            setCloseAt(toLocalDateTime(value));
            setIsCloseAtError(false);
          }}
        />
      </Field>

      <Button
        type="submit"
        variant="brandSolid"
        size="large"
        css={{ marginTop: tokens.spacing[8] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}

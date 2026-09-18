import { CSSProperties, useState } from 'react';
import Button from '../../../shared/ui/Button';
import { InputField } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import { tokens } from '../../../styles/global';
import { AssignmentValue } from '../types';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT } from '../constants';
import { formatDateToString, toLocalDateTime } from '../../../shared/utils/formatDate';
import { usePostHog } from '@posthog/react';

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
  fieldErrors?: Partial<Record<'title' | 'content' | 'submissionMethod' | 'closeAt', string>>;
}

const emptyValues = {
  title: '',
  content: '',
  submissionMethod: '',
  closeAt: '',
};

export default function AssignmentForm({
  initialValues = emptyValues,
  isSubmitting = false,
  fieldErrors = {},
  submitLabel,
  onSubmit,
}: AssignmentFormProps) {
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [submissionMethod, setsubmissionMethod] = useState(initialValues.submissionMethod);
  const [closeAt, setCloseAt] = useState(initialValues.closeAt);
  const posthog = usePostHog();

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();

    if (isSubmitting) return;

    posthog?.capture('assignment_form_submitted', {
      location: 'assignment_create_page',
    });

    onSubmit({ title, content, submissionMethod, closeAt });
  }

  return (
    <form css={formStyle} onSubmit={handleSubmit}>
      <InputField data-testid="assignment-title-field">
        <InputField.Label htmlFor={'assignment-title'} isRequired={true}>
          제목
        </InputField.Label>
        <Input
          id="assignment-title"
          name="title"
          value={title}
          autoFocus
          onChange={(event) => setTitle(event.target.value)}
          maxLength={ASSIGNMENT_TITLE.length}
          placeholder="제목을 입력해주세요"
        />
        <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
          <InputField.SubText errorText={fieldErrors.title} />
          <InputField.CurrentLength
            currentLength={title.length}
            maxLength={ASSIGNMENT_TITLE.length}
          />
        </div>
      </InputField>
      <InputField data-testid="assignment-content-field">
        <InputField.Label htmlFor={'assignment-content'} isRequired={true}>
          내용
        </InputField.Label>
        <TextArea
          id="assignment-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          maxLength={ASSIGNMENT_CONTENT.length}
          onChange={(event) => setContent(event.target.value)}
        />
        <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
          <InputField.SubText errorText={fieldErrors.content} />
          <InputField.CurrentLength
            currentLength={content.length}
            maxLength={ASSIGNMENT_CONTENT.length}
          />
        </div>
      </InputField>
      <InputField>
        <InputField.Label htmlFor={'submit-method'} isRequired={true}>
          제출 방법
        </InputField.Label>
        <Input
          id="submit-method"
          name="method"
          value={submissionMethod}
          autoFocus
          onChange={(event) => setsubmissionMethod(event.target.value)}
          placeholder="제출 방법을 입력해주세요"
          required={true}
        />
        <InputField.SubText errorText={fieldErrors.submissionMethod} />
      </InputField>
      <InputField>
        <InputField.Label htmlFor={'assignment-close-at'} isRequired={true}>
          마감 시각
        </InputField.Label>
        <DateTimePicker
          id="assignment-close-at"
          title="마감 시각 설정"
          value={closeAt ? new Date(closeAt) : undefined}
          triggerLabel={closeAt ? formatDateToString(closeAt) : '마감 시각 설정'}
          triggerVariant="neutralOutline"
          onChange={(value) => {
            setCloseAt(toLocalDateTime(value));
          }}
        />
        <InputField.SubText errorText={fieldErrors.closeAt} />
      </InputField>

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

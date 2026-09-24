import { CSSProperties, useState } from 'react';
import Button from '../../../shared/ui/Button';
import { Field } from '../../../shared/ui/inputs/Field';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import InputField from '../../../shared/widgets/InputField';
import TextAreaField from '../../../shared/widgets/TextAreaField';
import CheckField from '../../../shared/widgets/CheckField';
import { tokens } from '../../../styles/global';
import { AssignmentValue } from '../types';
import { ASSIGNMENT_TITLE, ASSIGNMENT_CONTENT } from '../constants';
import { formatDateToString, toLocalDateTime } from '../../../shared/utils/formatDate';
import { usePostHog } from '@posthog/react';
import { useInputState } from '../../../shared/hooks/useInputState';

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
  fieldErrors?: Partial<
    Record<'title' | 'content' | 'submissionMethod' | 'closeAt' | 'submissionTarget', string>
  >;
}

const emptyValues = {
  title: '',
  content: '',
  submissionMethod: '',
  closeAt: '',
  submissionTarget: 'MEMBERS_AND_LEADER',
} satisfies AssignmentValue;

export default function AssignmentForm({
  initialValues = emptyValues,
  isSubmitting = false,
  fieldErrors = {},
  submitLabel,
  onSubmit,
}: AssignmentFormProps) {
  const [title, handleTitleChange] = useInputState(initialValues.title, (value) =>
    value.slice(0, ASSIGNMENT_TITLE.length),
  );
  const [content, handleContentChange] = useInputState(initialValues.content, (value) =>
    value.slice(0, ASSIGNMENT_CONTENT.length),
  );
  const [submissionMethod, handleSubmissionMethodChange] = useInputState(
    initialValues.submissionMethod,
    (value) => value.slice(0, ASSIGNMENT_CONTENT.length),
  );
  const [closeAt, setCloseAt] = useState(initialValues.closeAt);
  const [submissionTarget, setSubmissionTarget] = useState(initialValues.submissionTarget);
  const posthog = usePostHog();

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();

    if (isSubmitting) return;

    posthog?.capture('assignment_form_submitted', {
      location: 'assignment_create_page',
    });

    onSubmit({ title, content, submissionMethod, closeAt, submissionTarget });
  }

  return (
    <form css={formStyle} onSubmit={handleSubmit}>
      <InputField
        id="assignment-title"
        name="title"
        label="제목"
        value={title}
        autoFocus
        onChange={handleTitleChange}
        maxLength={ASSIGNMENT_TITLE.length}
        placeholder="제목을 입력해주세요"
        errorText={fieldErrors.title}
        isRequired
        testId="assignment-title-field"
      />
      <TextAreaField
        id="assignment-content"
        name="content"
        label="내용"
        value={content}
        placeholder="내용을 입력해주세요"
        maxLength={ASSIGNMENT_CONTENT.length}
        onChange={handleContentChange}
        errorText={fieldErrors.content}
        isRequired
        testId="assignment-content-field"
      />
      <InputField
        id="submit-method"
        name="method"
        label="제출 방법"
        value={submissionMethod}
        maxLength={ASSIGNMENT_CONTENT.length}
        onChange={handleSubmissionMethodChange}
        placeholder="제출 방법을 입력해주세요"
        errorText={fieldErrors.submissionMethod}
        isRequired
      />
      <Field>
        <Field.Label htmlFor={'assignment-close-at'} isRequired={true}>
          마감 시각
        </Field.Label>
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
        <Field.SubText errorText={fieldErrors.closeAt} />
      </Field>

      <CheckField
        id="leader-submission"
        name="leaderSubmission"
        label="리드 제출 여부"
        checkLabel="나도 과제를 제출할게요"
        checked={submissionTarget === 'MEMBERS_AND_LEADER'}
        onChange={(event) =>
          setSubmissionTarget(event.target.checked ? 'MEMBERS_AND_LEADER' : 'MEMBERS_ONLY')
        }
        helpText="체크하지 않으면 리드는 과제 제출 대상에서 제외돼요"
        errorText={fieldErrors.submissionTarget}
        isRequired
      />

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

import { CSSProperties, useState } from 'react';
import type { CSSObject } from '@emotion/react';
import Button from '../../../shared/ui/Button';
import { Field } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import DateTimePicker from '../../../shared/ui/date-time-picker/DateTimePicker';
import { tokens, typography } from '../../../styles/global';
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

const checkboxLabelStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  width: 'fit-content',
  cursor: 'pointer',
  color: tokens.text.default,
  ...typography.body,
} satisfies CSSProperties;

const checkboxStyle = {
  appearance: 'none',
  width: 20,
  height: 20,
  margin: 0,
  cursor: 'pointer',
  border: tokens.border.default,
  borderRadius: tokens.spacing[1],
  '&:checked': {
    backgroundColor: tokens.bg.brand,
    borderColor: tokens.bg.brand,
  },
  '&:checked::after': {
    content: '""',
    display: 'block',
    width: 5,
    height: 9,
    margin: '4px 0 0 6px',
    border: `solid ${tokens.text.onBrand}`,
    borderWidth: '0 2px 2px 0',
    transform: 'rotate(45deg)',
  },
} satisfies CSSObject;

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
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [submissionMethod, setsubmissionMethod] = useState(initialValues.submissionMethod);
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
      <Field data-testid="assignment-title-field">
        <Field.Label htmlFor={'assignment-title'} isRequired={true}>
          제목
        </Field.Label>
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
          <Field.SubText errorText={fieldErrors.title} />
          <Field.CurrentLength
            currentLength={title.length}
            maxLength={ASSIGNMENT_TITLE.length}
          />
        </div>
      </Field>
      <Field data-testid="assignment-content-field">
        <Field.Label htmlFor={'assignment-content'} isRequired={true}>
          내용
        </Field.Label>
        <TextArea
          id="assignment-content"
          name="content"
          value={content}
          placeholder="내용을 입력해주세요"
          maxLength={ASSIGNMENT_CONTENT.length}
          onChange={(event) => setContent(event.target.value)}
        />
        <div css={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Field.SubText errorText={fieldErrors.content} />
          <Field.CurrentLength
            currentLength={content.length}
            maxLength={ASSIGNMENT_CONTENT.length}
          />
        </div>
      </Field>
      <Field>
        <Field.Label htmlFor={'submit-method'} isRequired={true}>
          제출 방법
        </Field.Label>
        <Input
          id="submit-method"
          name="method"
          value={submissionMethod}
          autoFocus
          onChange={(event) => setsubmissionMethod(event.target.value)}
          placeholder="제출 방법을 입력해주세요"
          required={true}
        />
        <Field.SubText errorText={fieldErrors.submissionMethod} />
      </Field>
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

      <Field>
        <Field.Label htmlFor={'leader-submission'} isRequired={true}>
          리드 제출 여부
        </Field.Label>
        <label css={checkboxLabelStyle}>
          <input
            id="leader-submission"
            name="leaderSubmission"
            type="checkbox"
            css={checkboxStyle}
            checked={submissionTarget === 'MEMBERS_AND_LEADER'}
            onChange={(event) =>
              setSubmissionTarget(event.target.checked ? 'MEMBERS_AND_LEADER' : 'MEMBERS_ONLY')
            }
            required={true}
          />
          나도 과제를 제출할게요
        </label>
        <Field.SubText
          errorText={fieldErrors.submissionTarget}
          helpText={'체크하지 않으면 리드는 과제 제출 대상에서 제외돼요'}
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

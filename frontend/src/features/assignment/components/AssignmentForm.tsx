import { CSSProperties, useState } from 'react';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
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
};

export default function AssignmentForm({
  initialValues = emptyValues,
  submitLabel,
  onSubmit,
}: AssignmentFormProps) {
  const [title, setTitle] = useState(initialValues.title);
  const [content, setContent] = useState(initialValues.content);
  const [submissionType, setSubmissionType] = useState(initialValues.submissionType);

  return (
    <form
      css={formStyle}
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({ title, content, submissionType });
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

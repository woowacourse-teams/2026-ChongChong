import { useState, type CSSProperties, type FormEvent } from 'react';
import Button from '../../../shared/ui/Button';
import { Field } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import { tokens } from '../../../styles/global';

const MAX_PROFILE_NAME_LENGTH = 8;

interface Props {
  initialName: string;
  isSubmitting: boolean;
  errorText?: string;
  onSubmit: (name: string) => void;
  onCancel: () => void;
  onChange: () => void;
}

const formStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

const actionsStyle = {
  display: 'flex',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

export default function ProfileNameForm({
  initialName,
  isSubmitting,
  errorText,
  onSubmit,
  onCancel,
  onChange,
}: Props) {
  const [name, setName] = useState(initialName);
  const [validationError, setValidationError] = useState<string>();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedName = name.trim();

    if (!trimmedName) {
      setValidationError('이름을 입력해 주세요.');
      return;
    }

    setValidationError(undefined);
    onSubmit(trimmedName);
  }

  return (
    <form css={formStyle} aria-label="프로필 이름 수정" onSubmit={handleSubmit}>
      <Field>
        <Field.Label htmlFor="profile-name">이름</Field.Label>
        <Input
          id="profile-name"
          value={name}
          disabled={isSubmitting}
          maxLength={MAX_PROFILE_NAME_LENGTH}
          aria-invalid={Boolean(validationError || errorText)}
          onChange={(event) => {
            setName(event.target.value);
            setValidationError(undefined);
            onChange();
          }}
        />
        <Field.SubText
          errorText={validationError ?? errorText}
          helpText={`${MAX_PROFILE_NAME_LENGTH}자 이내로 입력해 주세요`}
        />
      </Field>

      <div css={actionsStyle}>
        <Button
          type="button"
          variant="neutralOutline"
          size="small"
          disabled={isSubmitting}
          onClick={onCancel}
        >
          취소
        </Button>
        <Button
          type="submit"
          variant="brandSolid"
          size="small"
          disabled={isSubmitting || name.trim() === initialName}
        >
          {isSubmitting ? '저장 중...' : '저장'}
        </Button>
      </div>
    </form>
  );
}

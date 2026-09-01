import { useNavigate } from 'react-router';
import { CSSProperties, useMemo } from 'react';
import { useMutation } from '@tanstack/react-query';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import Button from '../../../shared/ui/Button';
import { useInputState } from '../../../shared/hooks/useInputState';
import { tokens } from '../../../styles/global';
import { createStudy } from '../api';
import isBlank from '../../../shared/utils/isBlank';
import { ValidationError } from '../../../shared/api/error';

const StudyFormStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

export default function StudyForm() {
  const navigate = useNavigate();
  const [nameValue, handleNameValue] = useInputState('');
  const [descriptionValue, handleDescriptionValue] = useInputState('');

  const { mutate, error } = useMutation({
    mutationFn: createStudy,
    onSuccess: (data) => navigate(`/studies/${data.studyId}`),
  });

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    const body = { name: nameValue, description: descriptionValue };
    mutate(body);
  }

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return (
    <form css={StudyFormStyle} onSubmit={handleSubmit}>
      <Field
        id="study-name"
        isRequired={true}
        label="스터디 이름"
        helpText="스터디원에게 그대로 보여요"
        errorText={fieldErrors.name}
        isError={Boolean(fieldErrors.name)}
      >
        <Input id="study-name" value={nameValue} onChange={handleNameValue} maxLength={15} />
      </Field>
      <Field
        id="study-description"
        label="어떤 스터디인가요?"
        helpText="모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요"
        errorText={fieldErrors.description}
        isError={Boolean(fieldErrors.description)}
      >
        <TextArea
          id="study-description"
          value={descriptionValue}
          onChange={handleDescriptionValue}
          maxLength={30}
        />
      </Field>
      <Button
        variant="brandSolid"
        size="large"
        type="submit"
        disabled={isBlank(nameValue)}
        css={{ marginTop: tokens.spacing[1] }}
      >
        스터디 만들기
      </Button>
    </form>
  );
}

import { useNavigate } from 'react-router';
import { CSSProperties } from 'react';
import { useMutation } from '@tanstack/react-query';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import TextArea from '../../../shared/ui/inputs/TextArea';
import Button from '../../../shared/ui/Button';
import { useInputState } from '../../../shared/hooks/useInputState';
import { tokens } from '../../../styles/global';
import { createStudy } from '../api';

const StudyFormStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

export default function StudyForm() {
  const navigate = useNavigate();
  const [nameValue, handleNameValue] = useInputState('');
  const [descriptionValue, handleDescriptionValue] = useInputState('');

  const mutation = useMutation({
    mutationFn: createStudy,
    onSuccess: (data) => navigate(`/studies/${data.studyId}`),
  });

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    const body = { name: nameValue, description: descriptionValue };
    mutation.mutate(body);
  }

  function isInValidInput() {
    if (nameValue.length > 0) return false;
    return true;
  }

  return (
    <form css={StudyFormStyle} onSubmit={handleSubmit}>
      <Field
        id="study-name"
        isRequired={true}
        label="스터디 이름"
        helpText="스터디원에게 그대로 보여요"
        errorText="스터디 이름은 15자 이하로 입력할 수 있어요"
      >
        <Input id="study-name" value={nameValue} onChange={handleNameValue} maxLength={15} />
      </Field>
      <Field
        id="study-description"
        label="어떤 스터디인가요?"
        helpText="모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요"
        errorText="설명은 30자 이하로 입력할 수 있어요"
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
        disabled={isInValidInput()}
        css={{ marginTop: tokens.spacing[1] }}
      >
        스터디 만들기
      </Button>
    </form>
  );
}

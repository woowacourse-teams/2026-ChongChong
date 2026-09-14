import { CSSProperties } from 'react';
import Input from '../../../shared/ui/inputs/Input';
import { InputField } from '../../../shared/ui/inputs/Field';
import TextArea from '../../../shared/ui/inputs/TextArea';
import Button from '../../../shared/ui/Button';
import { useInputState } from '../../../shared/hooks/useInputState';
import { STUDY_NAME, STUDY_DESCRIPTION } from '../constants';
import { tokens } from '../../../styles/global';
import isBlank from '../../../shared/utils/isBlank';
import { usePostHog } from '@posthog/react';

const StudyFormStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

interface Props {
  onSubmit: ({ name, description }: { name: string; description: string }) => void;
  isSubmitting: boolean;
  fieldErrors: Partial<Record<'name' | 'description', string>>;
}

export default function StudyForm({ onSubmit, isSubmitting, fieldErrors }: Props) {
  const [nameValue, handleNameValue] = useInputState('');
  const [descriptionValue, handleDescriptionValue] = useInputState('');
  const posthog = usePostHog();

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    posthog?.capture('study_form_submitted', {
      location: 'study_create_page',
    });
    const body = { name: nameValue, description: descriptionValue };
    onSubmit(body);
  }

  return (
    <form css={StudyFormStyle} onSubmit={handleSubmit}>
      <InputField data-testid="study-name-field">
        <InputField.Label htmlFor="study-name" isRequired={true}>
          스터디 이름
        </InputField.Label>
        <Input
          id="study-name"
          value={nameValue}
          onChange={handleNameValue}
          maxLength={STUDY_NAME.length}
          required={true}
        />
        <div css={{ display: 'flex', justifyContent: 'space-between' }}>
          <InputField.SubText
            errorText={fieldErrors.name}
            helpText={'스터디원에게 그대로 보여요'}
          />
          <InputField.CurrentLength
            currentLength={nameValue.length}
            maxLength={STUDY_NAME.length}
          />
        </div>
      </InputField>
      <InputField data-testid="study-description-field">
        <InputField.Label htmlFor="study-description">어떤 스터디인가요?</InputField.Label>
        <TextArea
          id="study-description"
          value={descriptionValue}
          onChange={handleDescriptionValue}
          maxLength={STUDY_DESCRIPTION.length}
        />
        <div css={{ display: 'flex', justifyContent: 'space-between' }}>
          <InputField.SubText
            errorText={fieldErrors.description}
            helpText={'모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요'}
          />
          <InputField.CurrentLength
            currentLength={descriptionValue.length}
            maxLength={STUDY_DESCRIPTION.length}
          />
        </div>
      </InputField>
      <Button
        variant="brandSolid"
        size="large"
        type="submit"
        disabled={isBlank(nameValue) || isSubmitting}
        css={{ marginTop: tokens.spacing[1] }}
      >
        스터디 만들기
      </Button>
    </form>
  );
}

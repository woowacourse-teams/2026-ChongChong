import { CSSProperties } from 'react';
import Button from '../../../shared/ui/Button';
import InputField from '../../../shared/widgets/InputField';
import TextAreaField from '../../../shared/widgets/TextAreaField';
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
  const [nameValue, handleNameValue] = useInputState('', (value) =>
    value.slice(0, STUDY_NAME.length),
  );
  const [descriptionValue, handleDescriptionValue] = useInputState('', (value) =>
    value.slice(0, STUDY_DESCRIPTION.length),
  );
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
      <InputField
        id="study-name"
        label="스터디 이름"
        value={nameValue}
        onChange={handleNameValue}
        maxLength={STUDY_NAME.length}
        helpText="스터디원에게 그대로 보여요"
        errorText={fieldErrors.name}
        isRequired
        testId="study-name-field"
      />
      <TextAreaField
        id="study-description"
        label="어떤 스터디인가요?"
        value={descriptionValue}
        onChange={handleDescriptionValue}
        maxLength={STUDY_DESCRIPTION.length}
        helpText="모이는 요일과 시간을 적어두면 초대할 때 설명이 줄어들어요"
        errorText={fieldErrors.description}
        testId="study-description-field"
      />
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

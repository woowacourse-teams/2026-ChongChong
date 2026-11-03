import StudyProfileField from './StudyProfileField';
import useStudyProfileImage from '../hooks/useStudyProfileImage';
import type { StudyFormValues } from '../types';
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
  submitLabel: string;
  onSubmit: (body: StudyFormValues) => void;
  isSubmitting: boolean;
  fieldErrors: Partial<Record<'name' | 'description' | 'imageFileId', string>>;
  initialValues?: { name: string; description: string | null; imageUrl?: string | null };
}

const defaultInitialValues: NonNullable<Props['initialValues']> = {
  name: '',
  description: '',
};

export default function StudyForm({
  submitLabel,
  onSubmit,
  isSubmitting,
  fieldErrors,
  initialValues = defaultInitialValues,
}: Props) {
  const profile = useStudyProfileImage({ initialImageUrl: initialValues.imageUrl, fieldErrors });
  const [nameValue, handleNameValue] = useInputState(initialValues.name, (value, prevValue) => {
    if (value.length > STUDY_NAME.length) return prevValue;
    return value;
  });
  const [descriptionValue, handleDescriptionValue] = useInputState(
    initialValues.description ?? '',
    (value, prevValue) => {
      if (value.length > STUDY_DESCRIPTION.length) return prevValue;
      return value;
    },
  );
  const posthog = usePostHog();

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting || profile.isUploading || isBlank(nameValue)) return;
    posthog?.capture('study_form_submitted', {
      location: 'study_create_page',
    });
    const body: StudyFormValues = { name: nameValue, description: descriptionValue };
    if (profile.imageFileId !== undefined) body.imageFileId = profile.imageFileId;
    onSubmit(body);
  }

  return (
    <form css={StudyFormStyle} onSubmit={handleSubmit}>
      <StudyProfileField
        imageUrl={profile.imageUrl}
        disabled={isSubmitting || profile.isUploading}
        errorText={profile.error}
        onSelect={profile.select}
        onReset={profile.reset}
      />
      <InputField
        id="study-name"
        label="스터디 이름"
        value={nameValue}
        onChange={handleNameValue}
        maxLength={STUDY_NAME.length}
        helpText="스터디 이름을 입력해 주세요"
        errorText={fieldErrors.name}
        isRequired
        testId="study-name-field"
        placeholder="영어 회화 스터디"
      />
      <TextAreaField
        id="study-description"
        label="스터디 설명"
        value={descriptionValue}
        onChange={handleDescriptionValue}
        maxLength={STUDY_DESCRIPTION.length}
        helpText="스터디를 소개해 주세요"
        errorText={fieldErrors.description}
        testId="study-description-field"
        placeholder="매주 수요일 저녁 7시, 영어로 대화해요"
      />
      <Button
        variant="brandSolid"
        size="large"
        type="submit"
        disabled={isBlank(nameValue) || isSubmitting || profile.isUploading}
        css={{ marginTop: tokens.spacing[1] }}
      >
        {submitLabel}
      </Button>
    </form>
  );
}

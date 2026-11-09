import StudyProfileField from './StudyProfileField';
import useStudyProfileImage from '../hooks/useStudyProfileImage';
import type { StudyFormValues } from '../types';
import { CSSProperties, useState } from 'react';
import SwitchField from '../../../shared/widgets/SwitchField';
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
  showVisibility?: boolean;
  onSubmit: (body: StudyFormValues) => void;
  isSubmitting: boolean;
  fieldErrors: Partial<Record<'name' | 'description' | 'imageFileId' | 'isPublic', string>>;
  initialValues?: { name: string; description: string | null; imageUrl?: string | null };
}

const defaultInitialValues: NonNullable<Props['initialValues']> = {
  name: '',
  description: '',
};

export default function StudyForm({
  submitLabel,
  showVisibility = false,
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
  const [isPublic, setIsPublic] = useState(false);

  function handleSubmit(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    if (isSubmitting || profile.isUploading || isBlank(nameValue)) return;
    posthog?.capture('study_form_submitted', {
      location: 'study_create_page',
    });
    const body: StudyFormValues = { name: nameValue, description: descriptionValue };
    if (profile.imageFileId !== undefined) body.imageFileId = profile.imageFileId;
    if (showVisibility) body.isPublic = isPublic;
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
      {showVisibility && (
        <SwitchField
          id="study-is-public"
          label="스터디 공개 여부"
          helpText="공개하면 누구나 스터디를 찾고 참여할 수 있어요."
          errorText={fieldErrors.isPublic}
          checked={isPublic}
          onChange={(event) => setIsPublic(event.target.checked)}
          disabled={isSubmitting}
        />
      )}
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

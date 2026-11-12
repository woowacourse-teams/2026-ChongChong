import type { CSSProperties } from 'react';
import { useState } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Field } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';
import myPageQueries from '../queries';
import UserProfileField from './UserProfileField';
import useUpdateProfileImage from '../hooks/useUpdateProfileImage';
import ProfileNameForm from './ProfileNameForm';
import useUpdateProfileName from '../hooks/useUpdateProfileName';

const profileSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

export default function ProfileSection() {
  const { data: profile } = useSuspenseQuery(myPageQueries.profile());
  const image = useUpdateProfileImage();
  const [isEditing, setIsEditing] = useState(false);
  const { mutate, isPending, errorText, reset } = useUpdateProfileName({
    onSuccess: () => setIsEditing(false),
  });

  function startEditing() {
    reset();
    setIsEditing(true);
  }

  function cancelEditing() {
    reset();
    setIsEditing(false);
  }

  return (
    <section css={profileSectionStyle} aria-label="프로필">
      <UserProfileField
        name={profile.name}
        imageUrl={profile.profileImageUrl}
        disabled={image.isPending || isEditing}
        errorText={image.error?.message}
        onSelect={(file) => image.mutate(file)}
        onReset={() => image.mutate(null)}
      />

      {isEditing ? (
        <ProfileNameForm
          initialName={profile.name}
          isSubmitting={isPending}
          errorText={errorText}
          onSubmit={mutate}
          onCancel={cancelEditing}
          onChange={reset}
        />
      ) : (
        <>
          <Field>
            <Field.Label htmlFor="profile-name">이름</Field.Label>
            <Input id="profile-name" value={profile.name} disabled />
            <Field.SubText helpText="다른 사람에게도 표시되는 이름이에요" />
          </Field>
          <Button
            type="button"
            variant="brandSolid"
            size="large"
            css={{ marginTop: tokens.spacing[5] }}
            disabled={image.isPending}
            onClick={startEditing}
          >
            프로필 수정하기
          </Button>
        </>
      )}
    </section>
  );
}

import type { CSSProperties } from 'react';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import Button from '../../../shared/ui/Button';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import { tokens } from '../../../styles/global';

interface ProfileSectionProps {
  name?: string;
}

const profileSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

const profileImageStyle = {
  width: '70px',
  height: '70px',
  alignSelf: 'center',
  marginBottom: '28px',
} satisfies CSSProperties;

export default function ProfileSection({ name = '바니' }: ProfileSectionProps) {
  return (
    <section css={profileSectionStyle} aria-label="프로필">
      <img src={profileIcon} alt="" aria-hidden="true" css={profileImageStyle} />

      <Field id="profile-name" label="이름" helpText="다른 사람에게도 표시되는 이름이에요">
        <Input
          id="profile-name"
          defaultValue={name}
          maxLength={15}
          style={{ border: tokens.border.brand }}
        />
      </Field>

      <Button
        variant="brandSolid"
        size="large"
        style={{ marginTop: tokens.spacing[6] }}
      >
        프로필 수정하기
      </Button>
    </section>
  );
}

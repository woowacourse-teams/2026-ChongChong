import type { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import { Field } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import { tokens } from '../../../styles/global';
import myPageQueries from '../queries';

const profileSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
} satisfies CSSProperties;

const profileImageStyle = {
  width: '70px',
  height: '70px',
  alignSelf: 'center',
  marginBottom: '28px',
  borderRadius: tokens.radius.full,
  objectFit: 'cover',
} satisfies CSSProperties;

export default function ProfileSection() {
  const { data: profile } = useSuspenseQuery(myPageQueries.profile());

  return (
    <section css={profileSectionStyle} aria-label="프로필">
      <img
        src={profile.profileImageUrl ?? profileIcon}
        alt={`${profile.name}님의 프로필 사진`}
        css={profileImageStyle}
      />
      <Field>
        <Field.Label htmlFor={'profile-name'}>이름</Field.Label>
        <Input id="profile-name" value={profile.name} disabled />
        <Field.SubText helpText={'다른 사람에게도 표시되는 이름이에요'} />
      </Field>
    </section>
  );
}

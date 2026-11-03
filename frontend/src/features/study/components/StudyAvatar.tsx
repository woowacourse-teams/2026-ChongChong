import type { CSSObject } from '@emotion/react';
import Avatar from '../../../shared/ui/Avatar';
import rabbit from '../../../shared/assets/icons/header-icon.svg?width=210&as=webp';
import { tokens } from '../../../styles/global';

interface StudyAvatarProps {
  imageUrl?: string | null;
  size?: number;
  alt?: string;
  variant?: 'circle' | 'list' | 'management';
}

const StudyAvatarStyle = {
  background: tokens.bg.brandSubtle,
} satisfies CSSObject;

const DefaultImageStyle = {
  display: 'block',
  objectFit: 'cover',
} satisfies CSSObject;

export default function StudyAvatar({
  imageUrl,
  size = 88,
  alt = '',
  variant = 'circle',
}: StudyAvatarProps) {
  const height = variant === 'management' ? 67 : size;

  return (
    <Avatar
      src={imageUrl}
      alt={alt}
      size={{ width: size, height }}
      shape={variant === 'circle' ? 'circle' : 'rounded'}
      css={StudyAvatarStyle}
      fallback={
        <img
          src={rabbit}
          alt={alt}
          css={[DefaultImageStyle, { width: size * 0.68, height: height * 0.68 }]}
        />
      }
    />
  );
}

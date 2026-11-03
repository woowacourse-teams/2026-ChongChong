import type { ReactNode } from 'react';
import type { CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';

export interface AvatarProps {
  src?: string | null;
  alt: string;
  size?: number | { width: number; height: number };
  shape?: 'circle' | 'rounded';
  fallback: ReactNode;
  className?: string;
}

const AvatarStyle = {
  display: 'grid',
  placeItems: 'center',
  flexShrink: 0,
  overflow: 'hidden',
  background: tokens.bg.neutral,
} satisfies CSSObject;

const ImageStyle = {
  display: 'block',
  objectFit: 'cover',
} satisfies CSSObject;

export default function Avatar({
  src,
  alt,
  size = 40,
  shape = 'circle',
  fallback,
  className,
}: AvatarProps) {
  const dimensions = typeof size === 'number' ? { width: size, height: size } : size;

  return (
    <span
      css={[
        AvatarStyle,
        dimensions,
        { borderRadius: shape === 'circle' ? '50%' : tokens.radius.lg },
      ]}
      className={className}
    >
      {src ? <img src={src} alt={alt} css={[ImageStyle, dimensions]} /> : fallback}
    </span>
  );
}

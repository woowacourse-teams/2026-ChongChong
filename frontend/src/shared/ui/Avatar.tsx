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
  // TODO: 객체가 전달되었을때 해당 사이즈 사용, 숫자가 전달되었을때는 해당 값을 width, heigth으로 사용.
  // 이 부분은 사용과 직접적으로 관련되어 문서화가 필요해 보임.
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

import { useState } from 'react';
import rabbit from '../../../shared/assets/icons/header-icon.svg?width=210&as=webp';
import { tokens } from '../../../styles/global';
import listMask from '../assets/list-profile-mask.svg';
import managementMask from '../assets/management-profile-mask.svg';

export default function StudyAvatar({
  imageUrl,
  size = 88,
  alt = '',
  variant = 'circle',
}: {
  imageUrl?: string | null;
  size?: number;
  alt?: string;
  variant?: 'circle' | 'list' | 'management';
}) {
  // 관리 페이지에서는 67사용 외에는 size 사용 ->
  const height = variant === 'management' ? 67 : size;
  const mask = variant === 'list' ? listMask : managementMask;
  const [failedUrl, setFailedUrl] = useState<string>();
  const custom = Boolean(imageUrl && imageUrl !== failedUrl);
  return (
    <span
      css={{
        display: 'grid',
        placeItems: 'center',
        width: size,
        height,
        flexShrink: 0,
        overflow: 'hidden',
        borderRadius: variant === 'circle' ? '50%' : undefined,
        maskImage: variant === 'circle' ? undefined : `url(${mask})`,
        maskSize: '100% 100%',
        maskRepeat: 'no-repeat',
        background: tokens.bg.brandSubtle,
      }}
    >
      <img
        src={custom ? imageUrl! : rabbit}
        alt={alt}
        onError={() => {
          if (custom) setFailedUrl(imageUrl!);
        }}
        css={{
          display: 'block',
          width: custom ? size : size * 0.68,
          height: custom ? height : height * 0.68,
          objectFit: 'cover',
        }}
      />
    </span>
  );
}

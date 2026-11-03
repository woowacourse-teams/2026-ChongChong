import { useRef, useState } from 'react';
import BottomSheet from '../../../shared/ui/date-time-picker/BottomSheet';
import { IMAGE_ACCEPT } from '../../../shared/modules/files/validation';
import { tokens } from '../../../styles/global';
import camera from '../assets/profile-camera.svg';
import StudyAvatar from './StudyAvatar';

interface Props {
  imageUrl?: string | null;
  disabled: boolean;
  onSelect: (file: File) => void;
  onReset: () => void;
}

export default function StudyProfileField({ imageUrl, disabled, onSelect, onReset }: Props) {
  const [open, setOpen] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const rowStyle = {
    display: 'block',
    width: '100%',
    height: 56,
    padding: '0 24px',
    border: 0,
    background: tokens.bg.default,
    color: tokens.text.default,
    font: 'inherit',
    fontSize: 16,
    textAlign: 'left' as const,
    cursor: 'pointer',
    '&:hover': { background: tokens.bg.brandSubtle },
    '&:focus-visible': { outline: `2px solid ${tokens.bg.brand}`, outlineOffset: -2 },
  };

  return (
    <div css={{ display: 'flex', justifyContent: 'center', margin: '24px 0 52px' }}>
      <button
        type="button"
        aria-label="스터디 프로필 이미지 변경"
        aria-haspopup="dialog"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => setOpen(true)}
        css={{
          position: 'relative',
          width: 88,
          height: 88,
          padding: 0,
          border: 0,
          borderRadius: '50%',
          background: 'transparent',
          cursor: 'pointer',
          '&:focus-visible': { outline: `2px solid ${tokens.bg.brand}`, outlineOffset: 4 },
        }}
      >
        <StudyAvatar imageUrl={imageUrl} alt="스터디 프로필" />
        <span
          css={{
            position: 'absolute',
            right: -2,
            bottom: -2,
            width: 28,
            height: 28,
            boxSizing: 'border-box',
            border: '2px solid white',
            borderRadius: '50%',
            background: tokens.bg.brand,
            display: 'grid',
            placeItems: 'center',
          }}
        >
          <img src={camera} alt="" width={16} height={16} />
        </span>
      </button>
      <input
        ref={input}
        type="file"
        hidden
        aria-label="프로필 이미지 파일"
        accept={IMAGE_ACCEPT}
        disabled={disabled}
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = '';
          if (file) onSelect(file);
        }}
      />
      <BottomSheet open={open} title="프로필 이미지" variant="menu" onClose={() => setOpen(false)}>
        <button
          type="button"
          css={rowStyle}
          onClick={() => {
            setOpen(false);
            input.current?.click();
          }}
        >
          프로필 이미지 선택
        </button>
        {imageUrl && (
          <button
            type="button"
            css={rowStyle}
            onClick={() => {
              setOpen(false);
              onReset();
            }}
          >
            기본 프로필 이미지 사용
          </button>
        )}
        <button type="button" css={rowStyle} onClick={() => setOpen(false)}>
          취소
        </button>
      </BottomSheet>
    </div>
  );
}

import { useRef } from 'react';
import type { ChangeEvent, CSSProperties } from 'react';
import { Field } from '../ui/inputs/Field';
import { tokens, typography } from '../../styles/global';
import imageIcon from '../assets/image-upload.svg';
import deleteIcon from '../assets/image-upload-delete.svg';
import addIcon from '../assets/add.svg';

interface ImageFieldProps {
  id: string;
  label: string;
  images: File[];
  onChange: (images: File[]) => void;
  helpText?: string;
  testId?: string;
}

const imageStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  minHeight: 52,
  padding: `0 ${tokens.spacing[3]}`,
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  color: tokens.text.primary,
  ...typography.subtitle,
} satisfies CSSProperties;

const addButtonStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[2],
  width: '100%',
  height: 48,
  border: 0,
  borderRadius: tokens.radius.md,
  background: tokens.bg.subtle,
  color: tokens.text.primary,
  cursor: 'pointer',
  ...typography.body,
} satisfies CSSProperties;

export default function ImageField({
  id,
  label,
  images,
  onChange,
  helpText,
  testId,
}: ImageFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selectedImages = Array.from(event.target.files ?? []).filter((image) =>
      image.type.startsWith('image/'),
    );
    if (selectedImages.length) onChange([...images, ...selectedImages]);
    event.target.value = '';
  };

  return (
    <Field data-testid={testId}>
      <Field.Label htmlFor={id}>{label}</Field.Label>
      <input
        ref={inputRef}
        id={id}
        type="file"
        accept="image/*"
        multiple
        onChange={handleImageChange}
        css={{ position: 'absolute', width: 1, height: 1, opacity: 0, overflow: 'hidden' }}
      />

      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[2] }}>
        {images.map((image, index) => (
          <div key={`${image.name}-${index}`} css={imageStyle}>
            <img src={imageIcon} alt="" width={24} height={24} />
            <span
              css={{
                flex: 1,
                minWidth: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {image.name}
            </span>
            <button
              type="button"
              aria-label={`${image.name} 삭제`}
              onClick={() => onChange(images.filter((_, imageIndex) => imageIndex !== index))}
              css={{
                display: 'grid',
                placeItems: 'center',
                padding: 0,
                border: 0,
                background: 'none',
                cursor: 'pointer',
              }}
            >
              <img src={deleteIcon} alt="" width={20} height={20} />
            </button>
          </div>
        ))}

        <button type="button" onClick={() => inputRef.current?.click()} css={addButtonStyle}>
          <img src={addIcon} alt="" width={24} height={24} />
          이미지 불러오기
        </button>
      </div>
      {helpText && <Field.HelpText>{helpText}</Field.HelpText>}
    </Field>
  );
}

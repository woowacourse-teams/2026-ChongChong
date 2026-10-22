import { useRef } from 'react';
import type { ChangeEvent, CSSProperties } from 'react';
import { Field } from '../ui/inputs/Field';
import { tokens, typography } from '../../styles/global';
import imageIcon from '../assets/image-upload.svg';
import deleteIcon from '../assets/image-upload-delete.svg';
import addIcon from '../assets/add.svg';
import type { AttachmentItem } from '../modules/files/types';

interface AttachmentFieldProps {
  id: string;
  label: string;
  items: AttachmentItem[];
  onSelect: (files: File[]) => void;
  onRemove: (id: string) => void;
  accept?: string;
  disabled?: boolean;
  errorText?: string;
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

export default function AttachmentField({
  id,
  label,
  items,
  onSelect,
  onRemove,
  accept,
  disabled = false,
  errorText,
  helpText,
  testId,
}: AttachmentFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  const handleImageChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selectedImages = Array.from(event.target.files ?? []);
    if (!disabled && selectedImages.length) onSelect(selectedImages);
    event.target.value = '';
  };

  return (
    <Field data-testid={testId}>
      <Field.Label htmlFor={id}>{label}</Field.Label>
      <input
        ref={inputRef}
        id={id}
        type="file"
        accept={accept}
        disabled={disabled}
        multiple
        onChange={handleImageChange}
        css={{ position: 'absolute', width: 1, height: 1, opacity: 0, overflow: 'hidden' }}
      />

      <div
        role="list"
        aria-label="첨부 파일"
        css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[2] }}
      >
        {items.map((image) => (
          <div role="listitem" key={image.id} css={imageStyle}>
            <img src={imageIcon} alt="" width={24} height={24} />
            <div
              css={{
                flex: 1,
                minWidth: 0,
                overflowWrap: 'anywhere',
              }}
            >
              <div
                title={image.name}
                css={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {image.name}
              </div>
              {image.status === 'error' ? (
                <Field.ErrorText role="alert">{image.message}</Field.ErrorText>
              ) : (
                <div aria-live="polite" css={{ ...typography.footnote, color: tokens.text.muted }}>
                  {image.status === 'uploading' ? '업로드 중...' : '업로드 완료'}
                </div>
              )}
            </div>
            <button
              type="button"
              aria-label={`${image.name} 삭제`}
              disabled={disabled}
              onClick={() => onRemove(image.id)}
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
      </div>
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        css={addButtonStyle}
      >
        <img src={addIcon} alt="" width={24} height={24} />
        파일 불러오기
      </button>
      <Field.SubText errorText={errorText} helpText={helpText} />
    </Field>
  );
}

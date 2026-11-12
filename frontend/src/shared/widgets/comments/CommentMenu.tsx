import { useEffect, useRef, useState, type CSSProperties, type KeyboardEvent } from 'react';
import type { Comment } from '../../modules/comments/responseSchemas';
import { tokens, typography } from '../../../styles/global';
import moreIcon from '../../assets/comment-more.svg';

interface Props {
  comment: Comment;
  onEdit: (button: HTMLButtonElement | null) => void;
  onDelete: (button: HTMLButtonElement | null) => void;
}

const triggerStyle = {
  display: 'grid',
  placeItems: 'center',
  width: '28px',
  height: '28px',
  margin: '-4px',
  cursor: 'pointer',
  flexShrink: 0,
} satisfies CSSProperties;

const menuStyle = {
  position: 'absolute',
  margin: 0,
  zIndex: 1,
  width: '100px',
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
  boxShadow: tokens.shadow[2],
  overflow: 'hidden',
} satisfies CSSProperties;

const itemStyle = {
  ...typography.body,
  display: 'block',
  width: '100%',
  minHeight: '40px',
  color: tokens.text.primary,
  cursor: 'pointer',
} satisfies CSSProperties;

export default function CommentMenu({ comment, onEdit, onDelete }: Props) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const menu = menuRef.current;
    const trigger = triggerRef.current;
    const parent = menu?.offsetParent;
    if (menu && trigger && parent instanceof HTMLElement) {
      const triggerRect = trigger.getBoundingClientRect();
      const parentRect = parent.getBoundingClientRect();
      // 스크롤 목록 밖의 BottomSheet를 기준으로 배치해 입력창 위에도 표시한다.
      menu.style.top = `${triggerRect.bottom - parentRect.top + 4}px`;
      menu.style.right = `${parentRect.right - triggerRect.right}px`;
    }
    menu?.querySelector('button')?.focus({ preventScroll: true });
    const closeOutside = (event: PointerEvent) => {
      if (event.target instanceof Node && !containerRef.current?.contains(event.target)) {
        setOpen(false);
      }
    };
    const closeOnMove = () => setOpen(false);
    document.addEventListener('pointerdown', closeOutside);
    document.addEventListener('scroll', closeOnMove, true);
    window.addEventListener('resize', closeOnMove);
    return () => {
      document.removeEventListener('pointerdown', closeOutside);
      document.removeEventListener('scroll', closeOnMove, true);
      window.removeEventListener('resize', closeOnMove);
    };
  }, [open]);

  function close() {
    setOpen(false);
    triggerRef.current?.focus();
  }
  function toggle() {
    if (open) {
      close();
      return;
    }
    setOpen(true);
  }

  function navigate(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape' || event.key === 'Tab') {
      event.preventDefault();
      event.stopPropagation();
      close();
      return;
    }
    if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    const items = Array.from(menuRef.current?.querySelectorAll('button') ?? []);
    const current = items.findIndex((item) => item === document.activeElement);
    const index =
      event.key === 'Home'
        ? 0
        : event.key === 'End'
          ? items.length - 1
          : (current + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
    items[index]?.focus();
  }

  return (
    <div ref={containerRef} css={{ flexShrink: 0 }}>
      <button
        ref={triggerRef}
        type="button"
        aria-label={`${comment.author.name} 댓글 더보기`}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={toggle}
        css={triggerStyle}
      >
        <img src={moreIcon} alt="" />
      </button>
      {open && (
        <div ref={menuRef} role="menu" aria-label="댓글 관리" onKeyDown={navigate} css={menuStyle}>
          {comment.canEdit && (
            <button
              type="button"
              role="menuitem"
              css={itemStyle}
              onClick={() => {
                setOpen(false);
                onEdit(triggerRef.current);
              }}
            >
              수정
            </button>
          )}
          {comment.canDelete && (
            <button
              type="button"
              role="menuitem"
              css={[
                itemStyle,
                comment.canEdit && { borderTop: tokens.border.default },
                { color: tokens.text.critical },
              ]}
              onClick={() => {
                setOpen(false);
                onDelete(triggerRef.current);
              }}
            >
              삭제
            </button>
          )}
          <button
            type="button"
            role="menuitem"
            css={[itemStyle, { borderTop: tokens.border.default, color: tokens.text.secondary }]}
            onClick={close}
          >
            취소
          </button>
        </div>
      )}
    </div>
  );
}

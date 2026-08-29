import { useEffect, useEffectEvent, useId, useRef, useState } from 'react';
import type { PointerEvent as ReactPointerEvent, ReactNode } from 'react';
import {
  actionStyle,
  backdropStyle,
  handleAreaStyle,
  handleStyle,
  headerStyle,
  overlayStyle,
  sheetStyle,
  titleStyle,
} from './dateTimePicker.styles';

const ANIMATION_DURATION_MS = 280;
const CLOSE_VELOCITY = 0.6;

interface DragState {
  pointerId: number;
  startY: number;
  lastY: number;
  lastTime: number;
  velocity: number;
}

export interface BottomSheetProps {
  open: boolean;
  title: ReactNode;
  actionLabel: ReactNode;
  children: ReactNode;
  onAction: () => void;
  onClose: () => void;
}

export default function BottomSheet({
  open,
  title,
  actionLabel,
  children,
  onAction,
  onClose,
}: BottomSheetProps) {
  const titleId = useId();
  const sheetRef = useRef<HTMLDivElement>(null);
  const dragStateRef = useRef<DragState | null>(null);
  const dragOffsetRef = useRef(0);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState(0);

  const updateDragOffset = (offset: number) => {
    dragOffsetRef.current = offset;
    setDragOffset(offset);
  };

  const handleKeyDown = useEffectEvent((event: KeyboardEvent) => {
    if (event.key === 'Escape') onClose();
  });

  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    sheetRef.current?.focus();

    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open]);

  const handleDragStart = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (!open || event.button !== 0) return;

    dragStateRef.current = {
      pointerId: event.pointerId,
      startY: event.clientY,
      lastY: event.clientY,
      lastTime: event.timeStamp,
      velocity: 0,
    };
    event.currentTarget.setPointerCapture?.(event.pointerId);
    setIsDragging(true);
  };

  const handleDragMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const dragState = dragStateRef.current;
    if (!dragState || dragState.pointerId !== event.pointerId) return;

    const elapsed = Math.max(event.timeStamp - dragState.lastTime, 1);
    dragState.velocity = (event.clientY - dragState.lastY) / elapsed;
    dragState.lastY = event.clientY;
    dragState.lastTime = event.timeStamp;
    updateDragOffset(Math.max(0, event.clientY - dragState.startY));
  };

  const finishDrag = (event: ReactPointerEvent<HTMLDivElement>, allowClose: boolean) => {
    const dragState = dragStateRef.current;
    if (!dragState || dragState.pointerId !== event.pointerId) return;

    const sheetHeight = sheetRef.current?.offsetHeight ?? 0;
    const closeThreshold = Math.min(160, Math.max(80, sheetHeight * 0.3));
    const shouldClose =
      allowClose &&
      (dragOffsetRef.current >= closeThreshold || dragState.velocity >= CLOSE_VELOCITY);

    event.currentTarget.releasePointerCapture?.(event.pointerId);
    dragStateRef.current = null;
    setIsDragging(false);

    if (shouldClose) {
      onClose();
    } else {
      updateDragOffset(0);
    }
  };

  const sheetTransform = open ? `translate3d(0, ${dragOffset}px, 0)` : 'translate3d(0, 100%, 0)';
  const backdropOpacity = open ? Math.max(0, 1 - dragOffset / 320) : 0;
  const transition = isDragging
    ? 'none'
    : `transform ${ANIMATION_DURATION_MS}ms cubic-bezier(0.22, 1, 0.36, 1)`;

  return (
    <div
      css={[
        overlayStyle,
        {
          visibility: open ? 'visible' : 'hidden',
          transition: open ? 'visibility 0s' : `visibility 0s linear ${ANIMATION_DURATION_MS}ms`,
        },
      ]}
      aria-hidden={!open}
    >
      <div
        css={[
          backdropStyle,
          {
            opacity: backdropOpacity,
            transition: isDragging ? 'none' : `opacity ${ANIMATION_DURATION_MS}ms ease`,
          },
        ]}
        data-testid="bottom-sheet-backdrop"
        onClick={onClose}
      />
      <div
        ref={sheetRef}
        css={[sheetStyle, { transform: sheetTransform, transition }]}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onTransitionEnd={(event) => {
          if (!open && event.propertyName === 'transform') updateDragOffset(0);
        }}
      >
        <div
          css={[handleAreaStyle, { cursor: isDragging ? 'grabbing' : 'grab' }]}
          aria-hidden="true"
          data-testid="bottom-sheet-handle"
          onPointerDown={handleDragStart}
          onPointerMove={handleDragMove}
          onPointerUp={(event) => finishDrag(event, true)}
          onPointerCancel={(event) => finishDrag(event, false)}
        >
          <div css={handleStyle} />
        </div>

        <header css={headerStyle}>
          <h2 id={titleId} css={titleStyle}>
            {title}
          </h2>
          <button type="button" css={actionStyle} onClick={onAction}>
            {actionLabel}
          </button>
        </header>

        {children}
      </div>
    </div>
  );
}

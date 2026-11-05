import {
  ComponentPropsWithRef,
  CSSProperties,
  ReactNode,
  RefObject,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
} from 'react';
import { CSSObject } from '@emotion/react';
import { tokens } from '../../styles/global';
import { useBodyScrollLock } from '../hooks/useBodyScrollLock';
import { FLOATING_SIZE, floatingMotion, floatingPositionStyle } from './floatingStyles';

type TriggerProps = Pick<
  ComponentPropsWithRef<'button'>,
  | 'ref'
  | 'onClick'
  | 'inert'
  | 'style'
  | 'aria-hidden'
  | 'aria-haspopup'
  | 'aria-expanded'
  | 'aria-controls'
> & { bottom?: CSSProperties['bottom'] };

interface FloatingPanelProps {
  'aria-label': string;
  trigger: (props: TriggerProps) => ReactNode;
  children: ReactNode | ((actions: { close: () => void }) => ReactNode);
  closeButton?: (actions: { close: () => void; expanded: boolean }) => ReactNode;
  bottom?: CSSProperties['bottom'];
}

const dialogStyle = {
  position: 'fixed',
  inset: 0,
  width: '100%',
  height: '100%',
  maxWidth: 'none',
  maxHeight: 'none',
  margin: 0,
  padding: 0,
  border: 0,
  background: 'transparent',
  color: tokens.text.default,
  '&::backdrop': { background: 'transparent' },
} satisfies CSSObject;

const panelPositionStyle = {
  ...floatingPositionStyle,
  width: `min(326px, calc(100% - ${tokens.layout.gutter} * 2))`,
  // 그림자를 clipping 밖에 두어 원형 버튼으로 접힐 때도 윤곽을 따라가게 한다.
  filter: 'drop-shadow(0 12px 16px rgba(15, 23, 42, 0.16))',
  pointerEvents: 'none',
} satisfies CSSObject;

const panelStyle = {
  display: 'flex',
  flexDirection: 'column',
  border: tokens.border.neutral,
  borderRadius: tokens.spacing[6],
  background: tokens.bg.default,
  pointerEvents: 'auto',
} satisfies CSSObject;

/** trigger와 임의의 콘텐츠를 선언적으로 연결한다. 메뉴 항목이나 이동 경로는 알지 못한다. */
export default function FloatingPanel({
  trigger,
  children,
  closeButton,
  bottom,
  'aria-label': label,
}: FloatingPanelProps) {
  const [phase, setPhase] = useState<'closed' | 'open' | 'closing'>('closed');
  const open = phase !== 'closed';
  const triggerRef = useRef<HTMLButtonElement>(null);
  const id = useId();
  const finishClose = useCallback(() => setPhase('closed'), []);
  const close = () => {
    setPhase((current) => (current === 'closed' ? current : 'closing'));
  };

  return (
    <>
      {trigger({
        ref: triggerRef,
        onClick: () => setPhase('open'),
        inert: open,
        'aria-hidden': open || undefined,
        style: {
          opacity: phase === 'open' ? 0 : 1,
          transition: `opacity ${phase === 'open' ? 100 : 180}ms ${floatingMotion.open.easing} ${phase === 'closing' ? 60 : 0}ms`,
        },
        'aria-haspopup': 'dialog',
        'aria-expanded': open,
        'aria-controls': open ? id : undefined,
        bottom,
      })}
      {open && (
        <PanelDialog
          id={id}
          label={label}
          bottom={bottom}
          closing={phase === 'closing'}
          onClose={close}
          onExited={finishClose}
          triggerRef={triggerRef}
          closeButton={closeButton}
        >
          {typeof children === 'function' ? children({ close }) : children}
        </PanelDialog>
      )}
    </>
  );
}

function PanelDialog({
  id,
  label,
  bottom,
  closing,
  onClose,
  onExited,
  triggerRef,
  children,
  closeButton,
}: {
  id: string;
  label: string;
  bottom?: CSSProperties['bottom'];
  closing: boolean;
  onClose: () => void;
  onExited: () => void;
  triggerRef: RefObject<HTMLButtonElement | null>;
  children: ReactNode;
  closeButton?: FloatingPanelProps['closeButton'];
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const surfaceRef = useRef<HTMLDivElement>(null);
  const [entered, setEntered] = useState(false);
  const expanded = entered && !closing;
  const { durationMs: duration, easing } = floatingMotion[closing ? 'close' : 'open'];
  useBodyScrollLock();

  useEffect(() => {
    if (!closing) return;
    // 전환이 취소되거나 transitionend가 누락되어도 모달이 남지 않도록 한다.
    const timeout = setTimeout(onExited, floatingMotion.close.durationMs + 80);
    return () => clearTimeout(timeout);
  }, [closing, onExited]);

  useEffect(() => {
    const dialog = dialogRef.current;
    const trigger = triggerRef.current;
    dialog?.showModal();
    // 시작 윤곽을 계산한 다음 전환한다. 도중에 닫아도 현재 모습에서 이어진다.
    surfaceRef.current?.getBoundingClientRect();
    const frame = requestAnimationFrame(() => setEntered(true));
    return () => {
      cancelAnimationFrame(frame);
      dialog?.close();
      if (trigger?.isConnected) trigger.focus({ preventScroll: true });
    };
  }, [triggerRef]);

  return (
    <dialog
      ref={dialogRef}
      id={id}
      aria-label={label}
      aria-modal="true"
      css={dialogStyle}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClose={(event) => {
        if (!event.currentTarget.open) onExited();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div
        css={[
          panelPositionStyle,
          {
            opacity: expanded ? 1 : 0,
            transition: `opacity ${expanded ? 120 : 100}ms ease ${closing ? 140 : 0}ms`,
          },
          bottom !== undefined && { bottom },
        ]}
      >
        <div
          ref={surfaceRef}
          inert={closing}
          css={{
            ...panelStyle,
            maxHeight: `calc(100dvh - ${tokens.layout.safeTop} - ${tokens.layout.safeBottom} - ${tokens.spacing[8]})`,
            clipPath: expanded
              ? `inset(0 0 0 0 round ${tokens.spacing[6]})`
              : `inset(calc(100% - ${FLOATING_SIZE}px) 0 0 calc(100% - ${FLOATING_SIZE}px) round ${FLOATING_SIZE / 2}px)`,
            transition: `clip-path ${duration}ms ${easing}`,
          }}
          onTransitionEnd={(event) => {
            if (
              closing &&
              event.target === event.currentTarget &&
              event.propertyName === 'clip-path'
            ) {
              onExited();
            }
          }}
        >
          <div
            css={{
              minHeight: 0,
              overflowY: 'auto',
              overscrollBehavior: 'contain',
              padding: tokens.spacing[3],
              paddingBottom: closeButton ? tokens.spacing[1] : tokens.spacing[3],
            }}
          >
            <div
              css={{
                opacity: expanded ? 1 : 0,
                transform: expanded ? 'translateY(0)' : 'translateY(6px)',
                transition: `opacity ${expanded ? 160 : 90}ms ease ${expanded ? 65 : 0}ms, transform ${expanded ? 240 : 140}ms ${easing}`,
              }}
            >
              {children}
            </div>
          </div>
          {closeButton && (
            <div
              css={{
                alignSelf: 'stretch',
                flexShrink: 0,
                // 접힐 때 여백도 줄여 brand 버튼이 원래 Floating 윤곽까지 이어지게 한다.
                padding: expanded ? `0 ${tokens.spacing[3]} ${tokens.spacing[3]}` : 0,
                transition: `padding ${duration}ms ${easing}`,
              }}
            >
              {closeButton({ close: onClose, expanded })}
            </div>
          )}
        </div>
      </div>
    </dialog>
  );
}

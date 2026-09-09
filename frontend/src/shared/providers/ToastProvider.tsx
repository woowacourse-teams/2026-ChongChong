/* eslint-disable react-refresh/only-export-components */

import { useState, useEffect, useContext, createContext, ReactNode } from 'react';
import { tokens } from '../../styles/global';

interface ToastProviderProps {
  children: ReactNode;
}

interface ToastContextValue {
  open: (ui: ReactNode, options?: ToastOptions) => void;
}

interface ToastOptions {
  duration: number;
}

const defaultOptions = {
  duration: 3000,
};

export const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function useToast() {
  const toast = useContext(ToastContext);

  if (!toast) {
    throw new Error('useToast는 ToastProvider 내부에서만 사용할 수 있습니다.');
  }

  return toast;
}

export function ToastProvider({ children }: ToastProviderProps) {
  const [toastData, setToastData] = useState<{
    ui: ReactNode;
    options: ToastOptions;
  } | null>(null);
  const [isVisible, setIsVisible] = useState(false);

  function onExited() {
    setToastData(null);
  }

  function open(ui: ReactNode, options: ToastOptions = defaultOptions) {
    setIsVisible(true);
    setToastData({ ui: ui, options: options });
  }

  useEffect(() => {
    if (toastData === null) return;

    const timerId = setTimeout(() => {
      setIsVisible(false);
    }, toastData?.options.duration);

    return () => clearTimeout(timerId);
  }, [toastData]);

  return (
    <ToastContext.Provider value={{ open }}>
      {children}
      <ToastWrapper isVisible={isVisible} onExited={onExited}>
        {toastData?.ui}
      </ToastWrapper>
    </ToastContext.Provider>
  );
}

function ToastWrapper({
  isVisible,
  onExited,
  children,
}: {
  isVisible: boolean;
  onExited: () => void;
  children: ReactNode;
}) {
  return (
    <div
      css={{
        transform: `translateX(-50%) translateY(${isVisible ? '-12px' : '48px'})`,
        opacity: isVisible ? 1 : 0,
        transition: `transform 400ms ease, opacity 400ms ease`,
        position: 'fixed',
        zIndex: 1000,
        bottom: `calc(${tokens.spacing[6]} + ${tokens.layout.safeBottom})`,
        left: '50%',
      }}
      onTransitionEnd={(event: React.TransitionEvent<HTMLDivElement>) => {
        if (event.propertyName !== 'opacity') return;
        if (isVisible) return;
        onExited();
      }}
    >
      {children}
    </div>
  );
}

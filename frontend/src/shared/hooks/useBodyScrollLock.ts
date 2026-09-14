import { useEffect } from 'react';

const SCROLL_POSITION_ATTR = 'data-scroll-y';

export function useBodyScrollLock(): void {
  useEffect(() => {
    enableBodyScrollLock();

    return () => {
      disableBodyScrollLock();
    };
  }, []);
}

export function enableBodyScrollLock(): void {
  if (isBodyScrollLocked()) {
    return;
  }

  const scrollY = window.scrollY;
  saveScrollPosition(scrollY);
  applyScrollLockStyles(scrollY);
}

function isBodyScrollLocked(): boolean {
  return document.body.getAttribute(SCROLL_POSITION_ATTR) != null;
}

function saveScrollPosition(scrollY: number): void {
  document.body.setAttribute(SCROLL_POSITION_ATTR, scrollY.toString());
}

function applyScrollLockStyles(scrollY: number): void {
  const { body } = document;
  body.style.overflow = 'hidden';
  body.style.position = 'fixed';
  body.style.top = `-${scrollY}px`;
  body.style.left = '0px';
  body.style.right = '0px';
  body.style.bottom = '0px';
}

export function disableBodyScrollLock(): void {
  const savedScrollY = document.body.getAttribute(SCROLL_POSITION_ATTR);

  if (savedScrollY == null) {
    return;
  }

  removeScrollLockStyles();
  restoreScrollPosition(savedScrollY);
  clearSavedScrollPosition();
}

function removeScrollLockStyles(): void {
  const { body } = document;
  body.style.removeProperty('overflow');
  body.style.removeProperty('position');
  body.style.removeProperty('top');
  body.style.removeProperty('left');
  body.style.removeProperty('right');
  body.style.removeProperty('bottom');
}

function restoreScrollPosition(savedScrollY: string): void {
  const scrollY = Number(savedScrollY);

  window.scrollTo(0, scrollY);
}

function clearSavedScrollPosition(): void {
  document.body.removeAttribute(SCROLL_POSITION_ATTR);
}

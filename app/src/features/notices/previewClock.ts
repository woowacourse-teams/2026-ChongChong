// API 연결 전 날짜 선택과 목록이 공유하는 Figma 시연 시계.
export const previewNow = new Date('2026-08-05T17:59:00').getTime();
export const previewDate = '2026-08-05';
export const isFutureReminder = (value: string): boolean =>
  new Date(value).getTime() > previewNow;

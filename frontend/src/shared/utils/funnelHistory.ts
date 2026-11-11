export function getHistoryIndex(): number {
  const index: unknown = window.history.state?.idx;
  return typeof index === 'number' && Number.isInteger(index) && index >= 0 ? index : 0;
}

export function getFunnelStartIndex(savedIndex: unknown): number {
  const currentIndex = getHistoryIndex();
  return typeof savedIndex === 'number' &&
    Number.isInteger(savedIndex) &&
    savedIndex >= 0 &&
    savedIndex <= currentIndex
    ? savedIndex
    : currentIndex;
}

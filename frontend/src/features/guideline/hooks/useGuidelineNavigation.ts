export function useGuidelineNavigation(
  page: number,
  pageCount: number,
  setPage: (page: number) => void,
) {
  const hasPrevious = page > 1;
  const hasNext = page < pageCount;

  const goPrevious = () => {
    if (hasPrevious) setPage(page - 1);
  };

  const goNext = () => {
    if (hasNext) setPage(page + 1);
  };

  return {
    hasPrevious,
    hasNext,
    goPrevious,
    goNext,
  };
}

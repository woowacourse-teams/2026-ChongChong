const PAGE_SIZE = 4;

interface CursorPage<T> {
  page: T[];
  nextCursor: number;
  hasNext: boolean;
}

export function paginateByCursor<T extends { id: number }>(
  items: T[],
  searchParams: URLSearchParams,
  size: number = PAGE_SIZE,
): CursorPage<T> {
  const cursor = Number(searchParams.get('cursor'));

  const sorted = [...items].sort((a, b) => b.id - a.id);
  // 시작할 아이템의 위치(cursor)를 찾습니다.
  const cursorIndex = cursor ? sorted.findIndex((item) => item.id === cursor) : -1;
  const startIndex = cursorIndex >= 0 ? cursorIndex + 1 : 0;
  // 현재 페이지에 보여줄 데이터를 결정합니다.
  const page = sorted.slice(startIndex, startIndex + size);
  const pageLastItem = page[page.length - 1];

  return {
    page,
    nextCursor: pageLastItem ? pageLastItem.id : cursor,
    hasNext: startIndex + page.length < sorted.length,
  };
}

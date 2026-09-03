export function formatDateToString(value: string) {
  const date = new Date(value);

  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ${String(
    date.getHours(),
  ).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

export function formatReminder(value: string) {
  const now = new Date();
  const remindDate = new Date(value);

  const diffMs = remindDate.getTime() - now.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));

  if (diffMinutes < 60) {
    return `${diffMinutes}분 뒤`;
  }

  const diffHours = Math.floor(diffMinutes / 60);

  if (diffHours < 24) {
    return `${diffHours}시간 뒤`;
  }

  const diffDays = Math.floor(diffHours / 24);

  return `${diffDays}일 뒤`;
}

export function formatSubmittedAt(value: string) {
  const date = new Date(value.replace(' ', 'T'));

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${hours}:${minutes} 제출`;
}

export function formatRelativeTime(value: string, now = new Date()) {
  const date = new Date(value.replace(' ', 'T'));

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const diffSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffSeconds < 60) {
    return '방금 전';
  }

  const diffMinutes = Math.floor(diffSeconds / 60);
  if (diffMinutes < 60) {
    return `${diffMinutes}분 전`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}시간 전`;
  }

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) {
    return `${diffDays}일 전`;
  }

  if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}주 전`;
  }

  if (diffDays < 365) {
    return `${Math.floor(diffDays / 30)}개월 전`;
  }

  return `${Math.floor(diffDays / 365)}년 전`;
}

export function toLocalDateTime(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const date = String(value.getDate()).padStart(2, '0');
  const hours = String(value.getHours()).padStart(2, '0');
  const minutes = String(value.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${date}T${hours}:${minutes}:00`;
}

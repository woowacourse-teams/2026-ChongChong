import type { NoticeReadState } from './types';
import type { Variant } from '../../shared/ui/Badge';

export const NOTICE_TITLE = {
  length: 100,
};

export const NOTICE_CONTENT = {
  length: 10000,
};

export const readStatusBadge = {
  READ: { variant: 'brandSolid', label: '읽음' },
  UNREAD: { variant: 'brandOutline', label: '읽지 않음' },
  NOT_ASSIGNED: { variant: 'neutralSolid', label: '확인 대상 아님' },
} satisfies Record<NoticeReadState, { variant: Variant; label: string }>;

export interface NoticeFormValues {
  title: string;
  content: string;
  remindAts?: string[];
}

export interface CreateNoticeResponse {
  noticeId: number;
}

export interface Notice {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  recipientCount?: number;
  readRecipientCount?: number;
  remindAt?: string;
  isComplete: boolean;
}

export interface NoticeListResponse {
  nextCursor: number | null;
  hasNext: boolean;
  notices: Notice[];
}

export interface Member {
  id: number;
  name: string;
  profileImage: string;
  lastRemindAt?: string;
}

export interface NoticeReadStatus {
  id: number;
  memberCount: number;
  readCount: number;
  unreadCount: number;
  remindAt?: string;
  readMembers: Member[];
  unreadMembers: Member[];
}

export interface NoticeDetail {
  id: number;
  title: string;
  writer: string;
  profileImageUrl: string;
  content: string;
  createdAt: string;
}

export interface MemberReadStatus {
  isRead: boolean;
  readAt: string | null;
}

export type UpdateNoticeValue = Partial<NoticeFormValues>;

export interface UpdateNoticeReadResponse {
  readAt: string;
}

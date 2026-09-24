export interface NoticeFormValues {
  title: string;
  content: string;
  remindAts?: string[] | null;
}

export interface CreateNoticeResponse {
  noticeId: number;
}

export type NoticeReadState = 'NOT_ASSIGNED' | 'UNREAD' | 'READ';

interface NoticeSummaryBase {
  id: number;
  title: string;
  content: string;
  createdAt: string;
}

export interface LeaderNoticeSummary extends NoticeSummaryBase {
  recipientCount: number;
  readRecipientCount: number;
  remindAt?: string;
  isComplete: boolean;
}

export interface MemberNoticeSummary extends NoticeSummaryBase {
  readStatus: NoticeReadState;
}

export type Notice = LeaderNoticeSummary | MemberNoticeSummary;

export interface NoticeListResponse {
  nextCursor: number | null;
  hasNext: boolean;
  notices: Notice[];
}

export interface Member {
  id: number;
  name: string;
  profileImage: string | null;
}

export interface UnreadNoticeMember extends Member {
  lastRemindAt: string | null;
}

export interface NoticeReadStatus {
  id: number;
  memberCount: number;
  readCount: number;
  unreadCount: number;
  remindAt: string | null;
  readMembers: Member[];
  unreadMembers: UnreadNoticeMember[];
}

export interface NoticeDetail {
  id: number;
  title: string;
  content: string;
  createdAt: string;
}

export type MemberReadStatus =
  | {
      readStatus: 'READ';
      readAt: string;
    }
  | {
      readStatus: 'UNREAD';
    }
  | {
      readStatus: 'NOT_ASSIGNED';
    };

export type UpdateNoticeValue = Partial<NoticeFormValues>;

export interface UpdateNoticeReadResponse {
  readAt: string;
}

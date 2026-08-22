export interface NoticeFormValues {
  title: string;
  content: string;
  reminders: Date[];
}

export interface Notice {
  id: number;
  title: string;
  description: string;
  createdAt: string;
  isRead?: boolean;
  readCount?: number;
  totalCount?: number;
  reminderText?: string;
}

export interface Assignment {
  id: number;
  title: string;
  content: string;
  submissionType: string;
  closeAt: string;
  memberCount: number;
  completeCount: number;
  remindAt?: string;
  isComplete: boolean;
}

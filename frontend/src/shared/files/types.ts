export interface Attachment {
  fileId: number;
  originalFileName: string;
  contentType: string;
  size: number;
  sortOrder: number;
  imageUrl?: string | null;
}

export type AttachmentItem = { id: string; name: string } & (
  | { status: 'uploading' }
  | { status: 'completed'; fileId: number }
  | { status: 'error'; message: string }
);

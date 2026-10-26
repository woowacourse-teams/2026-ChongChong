export interface Attachment {
  fileId: number;
  originalFileName: string;
  contentType: string;
  size: number;
  sortOrder: number;
  imageUrl: string | null;
  downloadUrl?: string;
}

export type AttachmentItem = { id: string; name: string; previewFile?: File; imageUrl?: string } & (
  | { status: 'uploading' }
  | { status: 'completed'; fileId: number }
  | { status: 'error'; message: string }
);

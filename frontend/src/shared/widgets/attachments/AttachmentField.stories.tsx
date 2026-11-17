import { useId, useState } from 'react';
import type { ComponentProps } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';
import type { AttachmentItem } from '../../modules/files/types';
import sampleImage from '../../assets/search-empty.png';
import secondImage from '../../assets/notification-green.png';
import AttachmentField from './AttachmentField';

const emptyItems: AttachmentItem[] = [];
const completed: AttachmentItem = {
  id: 'completed',
  name: '스터디 자료.pdf',
  status: 'completed',
  fileId: 1,
};
const uploading: AttachmentItem = { id: 'uploading', name: '추가 자료.pdf', status: 'uploading' };
const failed: AttachmentItem = {
  id: 'failed',
  name: '실패한 파일.pdf',
  status: 'error',
  message: '파일을 업로드하지 못했어요.',
};
const images: AttachmentItem[] = [
  {
    id: 'image-1',
    name: '스터디 이미지.png',
    status: 'completed',
    fileId: 2,
    imageUrl: sampleImage,
  },
  { id: 'image-2', name: '알림 이미지.png', status: 'completed', fileId: 3, imageUrl: secondImage },
];

function InteractiveAttachmentField({
  items: initialItems,
  ...args
}: ComponentProps<typeof AttachmentField>) {
  const [items, setItems] = useState(initialItems);
  const uniqueId = useId();

  return (
    <AttachmentField
      {...args}
      id={`${args.id}-${uniqueId}`}
      items={items}
      onSelect={(files) => {
        args.onSelect(files);
        setItems((current) => [
          ...current,
          ...files.map((file): AttachmentItem => ({
            id: crypto.randomUUID(),
            name: file.name,
            previewFile: file,
            status: 'completed',
            fileId: 0,
          })),
        ]);
      }}
      onRemove={(id) => {
        args.onRemove(id);
        setItems((current) => current.filter((item) => item.id !== id));
      }}
    />
  );
}

const meta = {
  title: 'Widgets/Attachments/AttachmentField',
  component: AttachmentField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '파일 목록과 선택·삭제·이미지 미리보기를 확인하는 예시입니다. 선택한 파일은 브라우저 메모리에만 보관하고 즉시 완료 상태로 표시하며 서버 업로드·용량 검증은 하지 않습니다. 업로드 중·실패는 고정 샘플입니다. disabled는 선택·삭제를 막으며 기존 이미지 확대는 허용합니다.',
      },
    },
  },
  args: {
    id: 'storybook-attachment',
    label: '첨부 파일',
    items: emptyItems,
    accept: '',
    disabled: false,
    showImagePreviews: false,
    helpText: '파일을 선택해 목록과 삭제 동작을 확인해주세요.',
    errorText: '',
    onSelect: fn(),
    onRemove: fn(),
  },
  argTypes: {
    id: { description: 'Docs에서 고유 접미사를 붙이는 파일 입력 식별자입니다.', control: false },
    label: { description: '필드 라벨입니다.', control: 'text' },
    items: {
      description:
        '예시의 초기 목록입니다. 목록 구성은 각 스토리에서 확인하며 선택·삭제 상태는 로컬로 관리합니다. File 객체는 Controls로 전달하지 않습니다.',
      control: false,
    },
    accept: {
      description: '네이티브 파일 선택창의 형식 필터입니다. 파일 검증 규칙은 아닙니다.',
      control: 'text',
    },
    disabled: { description: '파일 불러오기와 삭제를 막습니다.', control: 'boolean' },
    showImagePreviews: { description: '이미지 미리보기·확대를 표시합니다.', control: 'boolean' },
    helpText: { description: '오류가 없을 때 표시하는 도움말입니다.', control: 'text' },
    errorText: { description: '도움말보다 우선 표시하는 필드 오류입니다.', control: 'text' },
    onSelect: {
      description: '선택한 파일을 로컬 목록에 추가하고 Actions에 기록합니다.',
      control: false,
    },
    onRemove: {
      description: '해당 항목을 로컬 목록에서 제거하고 Actions에 기록합니다.',
      control: false,
    },
  },
  render: (args, context) => <InteractiveAttachmentField key={context.id} {...args} />,
} satisfies Meta<typeof AttachmentField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const Completed: Story = { args: { items: [completed] } };
export const Uploading: Story = { args: { items: [uploading] } };
export const UploadError: Story = { args: { items: [failed] } };
export const MixedStates: Story = { args: { items: [completed, uploading, failed] } };
export const ImagePreviews: Story = {
  args: {
    items: images,
    showImagePreviews: true,
    accept: 'image/*',
    helpText: '이미지를 눌러 확대하고 이전·다음 이미지 이동을 확인해주세요.',
  },
};
export const LongFileName: Story = {
  args: {
    items: [
      { ...completed, name: '여러_주제와_활동을_정리한_아주_긴_스터디_자료_파일명입니다.pdf' },
    ],
  },
};
export const ErrorOverHelp: Story = {
  args: {
    helpText: '오류가 있으면 숨겨지는 도움말입니다.',
    errorText: '첨부 파일을 확인해주세요.',
  },
};
export const Disabled: Story = { args: { disabled: true } };
export const DisabledWithFiles: Story = {
  args: { items: images, disabled: true, showImagePreviews: true },
};
export const WithoutHelp: Story = { args: { helpText: '' } };

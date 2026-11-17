import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';
import TextArea from './TextArea';

const meta = {
  title: 'UI/Inputs/TextArea',
  component: TextArea,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '기본 여러 줄 입력입니다. 최소 높이는 96px이며 직접 크기 조절은 제공하지 않습니다. 클릭하거나 Tab으로 포커스를 확인할 수 있습니다.',
      },
    },
  },
  args: {
    'aria-label': '여러 줄 입력',
    defaultValue: '',
    placeholder: '내용을 입력해주세요',
    disabled: false,
    readOnly: false,
    'aria-invalid': false,
    onChange: fn(),
  },
  argTypes: {
    defaultValue: {
      description: '초기 입력값입니다. Controls에서 바꾸면 입력을 새 값으로 초기화합니다.',
      control: 'text',
    },
    placeholder: { description: '값이 없을 때 표시할 안내 문구입니다.', control: 'text' },
    disabled: { description: '입력과 포커스를 막습니다.', control: 'boolean' },
    readOnly: { description: '포커스·선택은 허용하고 값 수정은 막습니다.', control: 'boolean' },
    'aria-invalid': {
      description: '오류 상태를 알리고 오류색 테두리를 표시합니다.',
      control: 'boolean',
    },
    onChange: { description: '입력 변경을 Actions에 기록합니다.', control: false },
  },
  render: (args) => <TextArea key={String(args.defaultValue)} {...args} />,
} satisfies Meta<typeof TextArea>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const WithValue: Story = { args: { defaultValue: '이번 주 스터디 내용을 정리합니다.' } };
export const Multiline: Story = {
  args: { defaultValue: '첫 번째 줄입니다.\n두 번째 줄입니다.\n세 번째 줄입니다.' },
};
export const Disabled: Story = { args: { defaultValue: '비활성 입력', disabled: true } };
export const ReadOnly: Story = { args: { defaultValue: '읽기 전용 입력', readOnly: true } };
export const Invalid: Story = {
  args: { defaultValue: '입력 내용을 확인해주세요', 'aria-invalid': true },
};

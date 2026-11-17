import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';
import Input from './Input';

const meta = {
  title: 'UI/Inputs/Input',
  component: Input,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '기본 한 줄 입력입니다. 클릭하거나 Tab으로 포커스하면 브랜드색 테두리가 표시됩니다. 오류 표시는 aria-invalid로 지정합니다.',
      },
    },
  },
  args: {
    'aria-label': '기본 입력',
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
  render: (args) => <Input key={String(args.defaultValue)} {...args} />,
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const WithValue: Story = { args: { defaultValue: '리액트 스터디' } };
export const Disabled: Story = { args: { defaultValue: '비활성 입력', disabled: true } };
export const ReadOnly: Story = { args: { defaultValue: '읽기 전용 입력', readOnly: true } };
export const Invalid: Story = {
  args: { defaultValue: '입력 내용을 확인해주세요', 'aria-invalid': true },
};

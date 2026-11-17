import { useId } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';
import { fn } from 'storybook/test';
import CheckField from './CheckField';

const meta = {
  title: 'Widgets/Fields/CheckField',
  component: CheckField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '라벨과 체크박스, 보조 문구를 조합한 필드입니다. isRequired는 필수 표시만 제공하며 네이티브 required를 설정하지 않습니다. 오류 문구가 도움말보다 우선합니다.',
      },
    },
  },
  args: {
    id: 'storybook-CheckField',
    label: '참여 확인',
    checkLabel: '참여 여부를 확인했습니다.',
    isRequired: false,
    checked: false,
    disabled: false,
    helpText: '',
    errorText: '',
    onChange: fn(),
  },
  argTypes: {
    id: {
      description: 'Docs에서 중복되지 않게 고유 접미사를 붙이는 식별자입니다.',
      control: false,
    },
    label: { description: '필드 라벨입니다.', control: 'text' },
    checkLabel: { description: '체크박스 옆 문구입니다.', control: 'text' },
    isRequired: { description: '라벨에 필수 표시만 추가합니다.', control: 'boolean' },
    checked: {
      description: '선택 상태입니다. 조작과 Controls가 함께 갱신됩니다.',
      control: 'boolean',
    },
    disabled: { description: '선택 변경을 막습니다.', control: 'boolean' },
    helpText: { description: '오류가 없을 때 표시하는 도움말입니다.', control: 'text' },
    errorText: { description: '도움말보다 우선 표시하는 오류 문구입니다.', control: 'text' },
    onChange: { description: '선택 상태를 갱신하고 Actions에 기록합니다.', control: false },
  },
  render: function Render(args) {
    const [, updateArgs] = useArgs();
    const uniqueId = useId();
    return (
      <CheckField
        {...args}
        id={`${args.id}-${uniqueId}`}
        onChange={(event) => {
          updateArgs({ checked: event.currentTarget.checked });
          args.onChange?.(event);
        }}
      />
    );
  },
} satisfies Meta<typeof CheckField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Unchecked: Story = {};
export const Checked: Story = { args: { checked: true } };
export const Required: Story = { args: { isRequired: true } };
export const Help: Story = { args: { helpText: '선택 여부를 확인해주세요.' } };
export const Error: Story = { args: { errorText: '설정 내용을 확인해주세요.' } };
export const ErrorOverHelp: Story = {
  args: {
    helpText: '오류가 있으면 숨겨지는 도움말입니다.',
    errorText: '오류 문구를 먼저 표시합니다.',
  },
};
export const Disabled: Story = { args: { disabled: true } };
export const DisabledChecked: Story = { args: { checked: true, disabled: true } };

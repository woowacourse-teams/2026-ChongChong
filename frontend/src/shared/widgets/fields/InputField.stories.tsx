import { useId } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';
import { fn } from 'storybook/test';
import InputField from './InputField';

const meta = {
  title: 'Widgets/Fields/InputField',
  component: InputField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '라벨·입력·도움말 또는 오류·글자 수를 조합한 제어 입력입니다. 입력과 Controls의 value는 동기화됩니다. errorText는 문구만 표시하므로 오류 테두리는 aria-invalid를 함께 지정합니다. maxLength는 사용자 입력을 제한하며, Controls로 직접 전달하는 value를 잘라내지는 않습니다.',
      },
    },
  },
  args: {
    id: 'storybook-InputField',
    label: '제목',
    value: '',
    maxLength: 100,
    isRequired: false,
    helpText: '',
    errorText: '',
    placeholder: '제목을 입력해주세요',
    disabled: false,
    'aria-invalid': false,
    onChange: fn(),
  },
  argTypes: {
    id: {
      description:
        '입력 식별자입니다. Docs 예시 중복 방지를 위해 스토리에서 고유 접미사를 붙입니다.',
      control: false,
    },
    label: { description: '입력과 연결된 라벨 문구입니다.', control: 'text' },
    value: {
      description: '현재 입력값입니다. 글자 수와 Controls에 함께 반영됩니다.',
      control: 'text',
    },
    maxLength: {
      description: '사용자가 입력할 수 있는 최대 길이와 글자 수 표시의 기준입니다.',
      control: { type: 'number', min: 0 },
    },
    isRequired: { description: '필수 표시와 네이티브 required를 설정합니다.', control: 'boolean' },
    helpText: {
      description: '도움말입니다. 오류 문구가 있으면 표시하지 않습니다.',
      control: 'text',
    },
    errorText: {
      description: '오류 문구입니다. 오류 테두리가 필요하면 aria-invalid도 지정합니다.',
      control: 'text',
    },
    placeholder: { description: '값이 없을 때 표시할 안내입니다.', control: 'text' },
    disabled: { description: '입력과 포커스를 막습니다.', control: 'boolean' },
    'aria-invalid': {
      description: '입력의 오류 상태와 오류 테두리를 설정합니다.',
      control: 'boolean',
    },
    onChange: {
      description: '입력값을 갱신하고 Actions에 변경 이벤트를 기록합니다.',
      control: false,
    },
  },
  render: function Render(args) {
    const [, updateArgs] = useArgs();
    const uniqueId = useId();

    return (
      <InputField
        {...args}
        id={`${args.id}-${uniqueId}`}
        onChange={(event) => {
          updateArgs({ value: event.currentTarget.value });
          args.onChange?.(event);
        }}
      />
    );
  },
} satisfies Meta<typeof InputField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Empty: Story = {};
export const WithValue: Story = { args: { value: '리액트 스터디' } };

export const Required: Story = { args: { isRequired: true } };
export const Help: Story = { args: { helpText: '100자 이내로 입력해주세요.' } };
export const Error: Story = { args: { errorText: '제목을 입력해주세요.', 'aria-invalid': true } };
export const ErrorOverHelp: Story = {
  args: {
    helpText: '도움말은 오류 문구가 있을 때 숨겨집니다.',
    errorText: '오류 문구를 먼저 표시합니다.',
    'aria-invalid': true,
  },
};
export const Disabled: Story = { args: { value: '비활성 입력', disabled: true } };
export const AtMaxLength: Story = { args: { value: '1234567890', maxLength: 10 } };

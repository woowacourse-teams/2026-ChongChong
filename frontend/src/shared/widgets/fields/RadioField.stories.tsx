import { useId } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';
import { fn } from 'storybook/test';
import RadioField from './RadioField';

const meta = {
  title: 'Widgets/Fields/RadioField',
  component: RadioField,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '선택지 중 하나를 고르는 필드입니다. value와 Controls를 동기화합니다. isRequired는 필수 표시만 제공하고 disabled 속성은 지원하지 않습니다. Docs 예시마다 id와 name을 분리합니다.',
      },
    },
  },
  args: {
    id: 'storybook-radio',
    name: 'visibility',
    label: '공개 범위',
    options: [
      { label: '공개', value: 'public' },
      { label: '비공개', value: 'private' },
    ],
    value: 'public',
    isRequired: false,
    helpText: '',
    errorText: '',
    onChange: fn(),
  },
  argTypes: {
    id: { description: '예시별 고유 접미사가 붙는 라벨·입력 식별자입니다.', control: false },
    name: { description: '예시별 고유 접미사가 붙는 라디오 그룹 이름입니다.', control: false },
    label: { description: '라디오 그룹 라벨입니다.', control: 'text' },
    options: {
      description: '하나 이상의 선택지입니다. 선택지 구성은 각 예시에서 확인합니다.',
      control: false,
    },
    value: { description: '현재 선택값입니다.', control: 'select', options: ['public', 'private'] },
    isRequired: { description: '그룹 라벨에 필수 표시만 추가합니다.', control: 'boolean' },
    helpText: { description: '오류가 없을 때 표시하는 도움말입니다.', control: 'text' },
    errorText: { description: '도움말보다 우선 표시하는 오류 문구입니다.', control: 'text' },
    onChange: {
      description: '선택한 문자열을 받고 Controls와 Actions를 갱신합니다.',
      control: false,
    },
  },
  render: function Render(args) {
    const [, updateArgs] = useArgs();
    const uniqueId = useId();
    return (
      <RadioField
        {...args}
        id={`${args.id}-${uniqueId}`}
        name={`${args.name}-${uniqueId}`}
        onChange={(value) => {
          updateArgs({ value });
          args.onChange(value);
        }}
      />
    );
  },
} satisfies Meta<typeof RadioField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Public: Story = {};
export const Private: Story = { args: { value: 'private' } };
export const Required: Story = { args: { isRequired: true } };
export const Help: Story = { args: { helpText: '스터디원에게 공개할지 선택해주세요.' } };
export const Error: Story = { args: { errorText: '공개 범위를 확인해주세요.' } };
export const ErrorOverHelp: Story = {
  args: {
    helpText: '오류가 있으면 숨겨지는 도움말입니다.',
    errorText: '오류 문구를 먼저 표시합니다.',
  },
};
export const SingleOption: Story = {
  args: { options: [{ label: '공개', value: 'public' }] },
  argTypes: { value: { options: ['public'] } },
};
export const ThreeOptions: Story = {
  args: {
    options: [
      { label: '공개', value: 'public' },
      { label: '비공개', value: 'private' },
      { label: '초대만', value: 'invite' },
    ],
  },
  argTypes: { value: { options: ['public', 'private', 'invite'] } },
};

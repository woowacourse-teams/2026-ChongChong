import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';
import Button from './Button';

const meta = {
  title: 'UI/Button',
  component: Button,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component: '공용 버튼입니다. variant로 색상과 테두리, size로 너비와 높이를 선택합니다.',
      },
    },
  },
  args: {
    variant: 'brandSolid',
    size: 'large',
    disabled: false,
    children: '확인',
    onClick: fn(),
  },
  argTypes: {
    variant: {
      description: '버튼 색상과 테두리입니다. 필수 속성입니다.',
      control: 'select',
      options: ['brandSolid', 'criticalSolid', 'brandOutline', 'criticalOutline', 'neutralOutline'],
    },
    size: {
      description:
        'small은 부모 너비의 50%·높이 44px, large는 100%·높이 52px입니다. 필수 속성입니다.',
      control: 'select',
      options: ['small', 'large'],
    },
    disabled: {
      description: '비활성화 시 클릭할 수 없습니다.',
      control: 'boolean',
    },
    children: {
      description: '버튼에 표시할 내용입니다. 이 예시에서는 문자열로 편집합니다.',
      control: 'text',
    },
    onClick: {
      description:
        '클릭 이벤트입니다. 이 예시는 Actions 패널에 기록하며 전송이나 이동을 하지 않습니다.',
      control: false,
    },
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const BrandSolid: Story = {};
export const CriticalSolid: Story = { args: { variant: 'criticalSolid' } };
export const BrandOutline: Story = { args: { variant: 'brandOutline' } };
export const CriticalOutline: Story = { args: { variant: 'criticalOutline' } };
export const NeutralOutline: Story = { args: { variant: 'neutralOutline' } };
export const Small: Story = { args: { size: 'small' } };
export const Disabled: Story = { args: { disabled: true } };

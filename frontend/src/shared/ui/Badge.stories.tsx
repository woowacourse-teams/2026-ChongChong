import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Badge from './Badge';

const meta = {
  title: 'UI/Badge',
  component: Badge,
  tags: ['autodocs'],
  parameters: {
    docs: { description: { component: '상태나 분류를 표시하는 배지입니다.' } },
  },
  args: { variant: 'brandSolid', size: 'small', children: '진행 중' },
  argTypes: {
    variant: {
      description: '배경·글자색·테두리 조합입니다. 필수 속성입니다.',
      control: 'select',
      options: ['brandSolid', 'neutralSolid', 'brandOutline', 'warningSolid'],
    },
    size: {
      description: '패딩과 글자 크기를 선택합니다. status는 최소 높이 24px입니다. 필수 속성입니다.',
      control: 'select',
      options: ['small', 'large', 'status'],
    },
    children: {
      description: '배지 문구입니다. 긴 문구는 자동 생략하지 않습니다.',
      control: 'text',
    },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

export const BrandSolid: Story = {};
export const NeutralSolid: Story = { args: { variant: 'neutralSolid' } };
export const BrandOutline: Story = { args: { variant: 'brandOutline' } };
export const WarningSolid: Story = { args: { variant: 'warningSolid', children: '지각 제출' } };
export const Large: Story = { args: { size: 'large' } };
export const Status: Story = { args: { size: 'status' } };
export const LongText: Story = {
  args: { children: '다음 모임 전까지 자료를 확인해주세요' },
};

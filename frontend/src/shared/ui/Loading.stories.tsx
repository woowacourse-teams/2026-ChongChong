import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Loading from './Loading';

const meta = {
  title: 'UI/Loading',
  component: Loading,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '데이터를 불러오는 동안 표시하는 토끼 애니메이션입니다. 이미지 크기는 150×150px이며 대체 텍스트는 "로딩 중"입니다. 별도 props는 없으며, 세로 flex 부모의 남은 공간을 채워 가운데 정렬됩니다. 이 예시는 실제 데이터 요청 없이 로딩 상태만 표시합니다.',
      },
    },
  },
} satisfies Meta<typeof Loading>;

export default meta;
type Story = StoryObj<typeof meta>;

export const DataLoading: Story = {
  render: () => (
    <div css={{ display: 'flex', flexDirection: 'column', height: 320 }}>
      <Loading />
    </div>
  ),
};

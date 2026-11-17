import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Spinner from './Spinner';

const meta = {
  title: 'UI/Spinner',
  component: Spinner,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          '데이터를 불러오는 동안 표시하는 회전 표시입니다. 부모 영역의 가운데 정렬되며 위아래 패딩을 포함합니다. role="status"와 ariaLabel로 로딩 상태를 전달합니다. 모션 감소 설정이 켜져 있으면 회전하지 않습니다. 예시는 실제 데이터 요청 없이 로딩 상태만 표시합니다.',
      },
    },
  },
  args: { size: 32, ariaLabel: '로딩 중' },
  argTypes: {
    size: {
      description: '원형 표시의 너비와 높이(px)입니다. 기본값은 32입니다.',
      control: { type: 'number', min: 8, step: 1 },
    },
    ariaLabel: {
      description: '스크린 리더에 전달하는 상태 설명입니다. 화면에 문구로 표시되지는 않습니다.',
      control: 'text',
    },
  },
} satisfies Meta<typeof Spinner>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

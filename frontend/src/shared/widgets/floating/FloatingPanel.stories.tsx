import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Button from '../../ui/Button';
import Floating from '../../ui/Floating';
import plus from '../../assets/icons/plus-white.svg';
import FloatingPanel from './FloatingPanel';

const meta = {
  title: 'Widgets/Floating/FloatingPanel',
  component: FloatingPanel,
  tags: ['autodocs'],
  parameters: {
    docs: {
      story: { inline: false, height: '260px' },
      controls: { disable: true },
      description: {
        component:
          '플로팅 버튼과 임의의 콘텐츠를 연결하는 패널입니다. 버튼을 눌러 열고, 닫기 버튼·패널 바깥·Escape로 닫을 수 있습니다. 닫히면 열기 버튼으로 초점이 돌아옵니다. Docs에서도 열고 닫을 수 있으며, 속성 변경은 Canvas Controls에서 확인합니다.',
      },
    },
  },
  args: {
    'aria-label': '플로팅 패널',
    trigger: ({ bottom, ...props }) => (
      <Floating
        {...props}
        css={{ bottom }}
        aria-label="패널 열기"
        icon={<img src={plus} alt="" width={30} height={30} />}
      />
    ),
    children: <p>메뉴 외에도 안내나 원하는 콘텐츠를 넣을 수 있습니다.</p>,
    closeButton: ({ close }) => (
      <Button variant="brandSolid" size="large" onClick={close}>
        닫기
      </Button>
    ),
  },
  argTypes: {
    'aria-label': { description: '열린 패널의 접근 가능한 이름입니다.', control: 'text' },
    bottom: {
      description:
        '패널의 아래쪽 위치입니다. 숫자는 px이며 calc() 문자열도 가능합니다. trigger에도 전달되므로 버튼에 함께 적용해야 합니다.',
      control: { type: 'number', min: 0 },
    },
    trigger: {
      description: '버튼의 ref·클릭·접근성 속성과 bottom을 받는 렌더 함수입니다.',
      control: false,
    },
    children: {
      description: '패널 콘텐츠입니다. 렌더 함수를 사용하면 close로 패널을 닫을 수 있습니다.',
      control: false,
    },
    closeButton: {
      description: '콘텐츠 아래에 고정할 닫기 버튼입니다. close 함수를 전달받습니다.',
      control: false,
    },
  },
} satisfies Meta<typeof FloatingPanel>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

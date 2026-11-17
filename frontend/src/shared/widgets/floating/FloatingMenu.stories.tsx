import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { MemoryRouter, Route, Routes } from 'react-router';
import Floating from '../../ui/Floating';
import plus from '../../assets/icons/plus-white.svg';
import door from '../../assets/icons/door-open-white.svg';
import FloatingMenu from './FloatingMenu';

const createItem = (
  <FloatingMenu.LinkItem
    to="/create"
    icon={<img src={plus} alt="" width={22} height={22} />}
    label="스터디 만들기"
    description="함께할 새로운 스터디를 열어요"
  />
);
const joinItem = (
  <FloatingMenu.LinkItem
    to="/join"
    icon={<img src={door} alt="" width={22} height={22} />}
    label="링크로 스터디 참여하기"
    description="초대 코드로 스터디에 합류해요"
  />
);

const meta = {
  title: 'Widgets/Floating/FloatingMenu',
  component: FloatingMenu,
  tags: ['autodocs'],
  parameters: {
    docs: {
      controls: { disable: true },
      description: {
        component:
          'FloatingPanel에 링크 항목과 닫기 버튼을 조합한 메뉴입니다. Default에서 메뉴를 열고 닫을 수 있으며, Items 예시에서 항목의 모양을 바로 볼 수 있습니다. 링크를 누르면 선택 결과 문구가 표시됩니다. 속성 변경은 Canvas Controls에서 확인합니다. 링크는 MemoryRouter 안에서만 이동하며 실제 서비스 화면이나 요청을 실행하지 않습니다. 링크 선택 자체가 메뉴를 닫지는 않습니다.',
      },
    },
  },
  args: {
    'aria-label': '스터디 메뉴',
    trigger: ({ bottom, ...props }) => (
      <Floating
        {...props}
        css={{ bottom }}
        aria-label="스터디 메뉴 열기"
        icon={<img src={plus} alt="" width={30} height={30} />}
      />
    ),
    children: (
      <>
        {createItem}
        {joinItem}
      </>
    ),
  },
  argTypes: {
    'aria-label': { description: '열린 메뉴의 접근 가능한 이름입니다.', control: 'text' },
    bottom: {
      description: '버튼과 메뉴의 아래쪽 위치입니다. 숫자는 px이며 calc()도 사용할 수 있습니다.',
      control: { type: 'number', min: 0 },
    },
    trigger: {
      description: 'FloatingPanel의 버튼 속성을 전달받는 렌더 함수입니다.',
      control: false,
    },
    children: {
      description:
        'LinkItem을 조합합니다. icon·label·to는 필수이며 description은 생략할 수 있습니다.',
      control: false,
    },
  },
  decorators: [
    (Story) => (
      <MemoryRouter>
        <Story />
        <Routes>
          <Route path="/" element={null} />
          <Route path="/create" element={<p role="status">스터디 만들기를 선택했습니다.</p>} />
          <Route path="/join" element={<p role="status">스터디 참여하기를 선택했습니다.</p>} />
        </Routes>
      </MemoryRouter>
    ),
  ],
} satisfies Meta<typeof FloatingMenu>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  parameters: { docs: { story: { inline: false, height: '260px' } } },
};
export const Items: Story = {
  render: () => (
    <div css={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      {createItem}
      {joinItem}
    </div>
  ),
  parameters: {
    docs: { description: { story: '아이콘·라벨·설명이 있는 링크 항목의 모습입니다.' } },
  },
};
export const WithoutDescription: Story = {
  render: () => (
    <FloatingMenu.LinkItem
      to="/create"
      icon={<img src={plus} alt="" width={22} height={22} />}
      label="스터디 만들기"
    />
  ),
};

import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import Floating from '../../../shared/ui/Floating';
import FloatingMenu from '../../../shared/ui/FloatingMenu';
import plus from '../../../shared/assets/icons/plus-white.svg';
import door from '../../../shared/assets/icons/door-open-white.svg';

const actions = [
  {
    label: '스터디 만들기',
    description: '함께할 새로운 스터디를 열어요',
    icon: plus,
    destination: URL_POLICY.toCreateStudy(),
  },
  {
    label: '스터디 참여하기',
    description: '초대 링크로 스터디에 합류해요',
    icon: door,
    destination: URL_POLICY.toJoinStudy(),
  },
];

export default function StudyFloatingActions() {
  return (
    <FloatingMenu
      aria-label="스터디 메뉴"
      trigger={(props) => (
        <Floating
          {...props}
          aria-label="스터디 메뉴 열기"
          icon={<img src={plus} alt="" width={30} height={30} />}
        />
      )}
    >
      {actions.map(({ label, description, icon, destination }) => (
        <FloatingMenu.Item
          key={label}
          as={Link}
          {...destination}
          icon={<img src={icon} alt="" width={22} height={22} />}
          label={label}
          description={description}
        />
      ))}
    </FloatingMenu>
  );
}

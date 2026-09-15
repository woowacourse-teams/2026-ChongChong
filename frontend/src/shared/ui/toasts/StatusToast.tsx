import { ToastRoot } from './Toast';
import ErrorIcon from '../../assets/red-x.webp';
import SuccessIcon from '../../assets/green-check.webp';
import { tokens } from '../../../styles/global';

type Status = 'Error' | 'Success';

interface Props {
  status: Status;
  message: string;
}

export default function StatusToast({ status, message }: Props) {
  function decideStatusIcon(status: Status) {
    if (status === 'Success') return SuccessIcon;
    if (status === 'Error') return ErrorIcon;
    return SuccessIcon;
  }
  return (
    <ToastRoot
      content={
        <div css={{ display: 'flex', alignItems: 'center', gap: tokens.spacing[1] }}>
          <img src={decideStatusIcon(status)} width={32} height={32}></img>
          {message}
        </div>
      }
    ></ToastRoot>
  );
}

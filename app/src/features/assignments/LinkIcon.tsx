import { SvgXml } from 'react-native-svg';
import { tokens as t } from '../../ui/tokens';

const normal =
  '<svg width="13" height="13" viewBox="0 0 13 13" fill="none" xmlns="http://www.w3.org/2000/svg">\n<g clip-path="url(#clip0_595_2016)">\n<path d="M5.41675 7.04134C5.92301 7.53758 6.60367 7.81554 7.31258 7.81554C8.0215 7.81554 8.70215 7.53758 9.20841 7.04134L10.8334 5.41634C11.2246 4.8948 11.4145 4.24965 11.3683 3.59935C11.3221 2.94906 11.0428 2.33725 10.5818 1.87626C10.1208 1.41527 9.50904 1.13604 8.85874 1.08982C8.20844 1.04361 7.5633 1.23352 7.04175 1.62468L6.50008 2.16634" stroke="#0F172A" stroke-opacity="0.4" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>\n<path d="M7.58334 5.95877C7.07707 5.46253 6.39642 5.18457 5.6875 5.18457C4.97859 5.18457 4.29794 5.46253 3.79167 5.95877L2.16667 7.58377C1.77551 8.10532 1.5856 8.75046 1.63182 9.40076C1.67803 10.0511 1.95727 10.6629 2.41826 11.1238C2.87925 11.5848 3.49105 11.8641 4.14135 11.9103C4.79164 11.9565 5.43679 11.7666 5.95834 11.3754L6.5 10.8338" stroke="#0F172A" stroke-opacity="0.4" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>\n</g>\n<defs>\n<clipPath id="clip0_595_2016">\n<rect width="13" height="13" fill="white"/>\n</clipPath>\n</defs>\n</svg>\n';
const active =
  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">\n<path d="M8.6665 4.00033L9.99984 2.66699C10.6665 2.00033 11.9998 2.00033 12.6665 2.66699L13.3332 3.33366C13.9998 4.00033 13.9998 5.33366 13.3332 6.00033L9.99984 9.33366C9.33317 10.0003 7.99984 10.0003 7.33317 9.33366M7.33317 12.0003L5.99984 13.3337C5.33317 14.0003 3.99984 14.0003 3.33317 13.3337L2.6665 12.667C1.99984 12.0003 1.99984 10.667 2.6665 10.0003L5.99984 6.66699C6.6665 6.00033 7.99984 6.00033 8.6665 6.66699" stroke="#00C471" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>\n</svg>\n';
export function LinkIcon({
  size = 16,
  green = false,
}: {
  readonly size?: number;
  readonly green?: boolean;
}) {
  return (
    <SvgXml
      xml={(green ? active : normal).replaceAll('#00C471', t.color.brand)}
      width={size}
      height={size}
      aria-hidden
    />
  );
}

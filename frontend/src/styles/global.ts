import { css } from '@emotion/react';
import { modernNormalize } from './normalize';

export const globalStyles = css`
  :root {
    /* Primitive Ink */

    --main-black: #111111;
    --ink-900: #0f172a;

    /* Primitive Green */

    --green-50: #e6f9f0;
    --green-500: #00c471;

    /* Primitive Gray */

    --gray-50: #f9f9f9;
    --gray-200: #ececee;

    /* Primitive Red */

    --red-500: #de5e56;

    /* Primitive Social */

    --social-kakao: #fee500;

    /* Figma Color Tokens */

    --card-line-8: rgba(15, 23, 42, 0.08);
    --option-sub-6: rgba(15, 23, 42, 0.06);
    --option-placeholder-40: rgba(15, 23, 42, 0.4);
    --option-sub-font-color-55: rgba(15, 23, 42, 0.55);
    --option-sub-70: rgba(15, 23, 42, 0.7);
    --option-gray-background-icon: #ececee;
    --option-deactivate: rgba(0, 0, 0, 0.32);
    --google-login-line: rgba(116, 119, 117, 0.7);

    /* Semantic Background */

    --bg-default: #ffffff;
    --bg-subtle: var(--gray-50);
    --bg-brand: var(--green-500);
    --bg-brand-subtle: var(--green-50);
    --bg-chip: var(--option-gray-background-icon);
    --bg-disabled: var(--option-deactivate);
    --bg-dim: rgba(0, 0, 0, 0.42);

    /* Semantic Border */

    --border-default: var(--card-line-8);
    --border-subtle: var(--card-line-8);
    --border-brand: var(--green-500);
    --border-danger: var(--red-500);
    --border-google: var(--google-login-line);

    /* Semantic Text */

    --text-default: #172033;
    --text-secondary: var(--option-sub-70);
    --text-muted: var(--option-sub-font-color-55);
    --text-placeholder: var(--option-placeholder-40);
    --text-brand: var(--green-500);
    --text-danger: var(--red-500);
    --text-on-brand: #ffffff;
    --text-on-brand-strong: var(--main-black);

    /* Layout */

    --layout-gutter: 20px;
    --layout-header-height: 64px;
    --layout-tab-bar-height: 64px;
    --safe-top: env(safe-area-inset-top, 0px);
    --safe-bottom: env(safe-area-inset-bottom, 0px);

    /* Spacing */

    --spacing-0: 0px;
    --spacing-1: 4px;
    --spacing-2: 8px;
    --spacing-3: 12px;
    --spacing-4: 16px;
    --spacing-5: 20px;
    --spacing-6: 24px;
    --spacing-8: 32px;
    --spacing-10: 40px;
    --spacing-20: 80px;

    /* Radius */

    --radius-sm: 8px;
    --radius-md: 12px;
    --radius-lg: 16px;
    --radius-xl: 20px;
    --radius-full: 999px;

    /* Elevation */

    --shadow-1: 0 1px 2px rgba(15, 23, 42, 0.06);
    --shadow-2: 0 1px 2px rgba(29, 41, 57, 0.04), 0 3px 8px rgba(29, 41, 57, 0.06);
    --shadow-3: 0 12px 32px rgba(15, 23, 42, 0.16);

    /* Typography Family */
    --font-family-base:
      'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui,
      'Apple SD Gothic Neo', sans-serif;

    /* Typography Weight */
    --font-weight-regular: 400;
    --font-weight-medium: 500;
    --font-weight-semibold: 600;

    /* Typography Size */
    --font-size-24: 24px;
    --font-size-18: 18px;
    --font-size-16: 16px;
    --font-size-14: 14px;
    --font-size-13: 13px;
    --font-size-12: 12px;

    /* Typography Line height */
    --line-height-34: 34px;
    --line-height-28: 28px;
    --line-height-24: 24px;
    --line-height-20: 20px;
    --line-height-18: 18px;

    /* Typography Letter spacing */
    --letter-spacing-default: -0.025em;
  }
  ${modernNormalize}
`;

export const typography = {
  headline: css`
    font-size: var(--font-size-24);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-34);
    letter-spacing: var(--letter-spacing-default);
  `,

  title: css`
    font-size: var(--font-size-18);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-28);
    letter-spacing: var(--letter-spacing-default);
  `,

  subtitle: css`
    font-size: var(--font-size-16);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-24);
    letter-spacing: var(--letter-spacing-default);
  `,

  sectionLabel: css`
    font-size: var(--font-size-16);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-24);
    letter-spacing: var(--letter-spacing-default);
  `,

  button: css`
    font-size: var(--font-size-16);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-24);
    letter-spacing: var(--letter-spacing-default);
  `,

  body: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-20);
    letter-spacing: var(--letter-spacing-default);
  `,

  bodyStrong: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-semibold);
    line-height: var(--line-height-20);
    letter-spacing: var(--letter-spacing-default);
  `,
  paragraph: css`
    font-size: var(--font-size-14);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-20);
    letter-spacing: var(--letter-spacing-default);
  `,

  caption: css`
    font-size: var(--font-size-13);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-18);
    letter-spacing: var(--letter-spacing-default);
  `,

  label: css`
    font-size: var(--font-size-12);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-18);
    letter-spacing: var(--letter-spacing-default);
  `,

  footnote: css`
    font-size: var(--font-size-12);
    font-weight: var(--font-weight-regular);
    line-height: var(--line-height-18);
    letter-spacing: var(--letter-spacing-default);
  `,
};

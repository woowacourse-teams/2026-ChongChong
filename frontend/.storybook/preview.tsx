import { Global } from '@emotion/react';
import type { Preview } from '@storybook/react-webpack5';
import { globalStyles } from '../src/styles/global';

const preview: Preview = {
  parameters: {
    viewport: {
      options: {
        narrow: { name: '320px', styles: { width: '320px', height: '800px' }, type: 'mobile' },
        standard: { name: '390px', styles: { width: '390px', height: '800px' }, type: 'mobile' },
        wide: { name: '448px', styles: { width: '448px', height: '800px' }, type: 'mobile' },
      },
    },
  },
  decorators: [
    (Story) => (
      <>
        <Global styles={globalStyles} />
        <div css={{ width: '100%', maxWidth: 448, margin: '0 auto' }}>
          <Story />
        </div>
      </>
    ),
  ],
};

export default preview;

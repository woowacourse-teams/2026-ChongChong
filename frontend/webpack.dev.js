import process from 'node:process';
import { merge } from 'webpack-merge';
import { createCommonConfig, createSentryPlugin } from './webpack.common.js';

const commonConfig = createCommonConfig({
  environment: 'dev',
  assetFilename: 'assets/[name][ext]',
});
const sentryAuthToken = process.env.SENTRY_AUTH_TOKEN;

if (process.env.DEPLOY_ENV !== 'dev') {
  throw new Error('DEPLOY_ENV must be dev for a development deployment build.');
}

if (!sentryAuthToken) {
  throw new Error('SENTRY_AUTH_TOKEN is required for a development deployment build.');
}

export default merge(commonConfig, {
  mode: 'production',
  devtool: 'source-map',

  output: {
    filename: 'bundle.js',
    chunkFilename: '[name].js',
  },

  plugins: [createSentryPlugin(sentryAuthToken)],
});

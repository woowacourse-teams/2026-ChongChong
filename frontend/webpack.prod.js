import process from 'node:process';
import { merge } from 'webpack-merge';
import { createCommonConfig, createSentryPlugin } from './webpack.common.js';

const commonConfig = createCommonConfig({
  environment: 'prod',
  assetFilename: 'assets/[name].[contenthash:16][ext]',
});
const sentryAuthToken = process.env.SENTRY_AUTH_TOKEN;

if (process.env.DEPLOY_ENV !== 'prod') {
  throw new Error('DEPLOY_ENV must be prod for a production deployment build.');
}

if (!sentryAuthToken) {
  throw new Error('SENTRY_AUTH_TOKEN is required for a production build.');
}

export default merge(commonConfig, {
  mode: 'production',
  devtool: 'hidden-source-map',

  output: {
    filename: '[name].[contenthash:16].js',
    chunkFilename: '[name].[contenthash:16].js',
  },

  plugins: [createSentryPlugin(sentryAuthToken)],
});

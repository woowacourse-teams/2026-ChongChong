import ky from 'ky';

const api = ky.create({
  baseUrl: 'https://mock.chongchong.com',
  hooks: {},
});

export default api;

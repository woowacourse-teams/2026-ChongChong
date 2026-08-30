import { userTable, userSchemaType } from '../features/user/mocks/db';

function getUserIdentifier(headers: Headers) {
  const token = headers.get('Authorization')?.replace('Bearer ', '');
  if (!token) return undefined;

  const userId = Number(token);
  return Number.isNaN(userId) ? undefined : userId;
}

export function findUserFromHeader(headers: Headers): undefined | userSchemaType {
  const userId = getUserIdentifier(headers);
  if (userId === undefined) return undefined;
  const user = userTable.findFirst((q) => q.where({ id: Number(userId) }));
  return user;
}

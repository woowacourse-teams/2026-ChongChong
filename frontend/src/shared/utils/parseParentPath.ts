export function parseParentPath(pathname: string) {
  let parentPath = pathname;

  // 후행 슬래시 제거
  if (pathname.endsWith('/')) {
    parentPath = pathname.substring(0, pathname.lastIndexOf('/'));
  }

  parentPath = pathname.substring(0, parentPath.lastIndexOf('/'));

  if (parentPath.endsWith('submissions')) {
    return parentPath.substring(0, parentPath.lastIndexOf('/'));
  } else {
    return parentPath;
  }
}

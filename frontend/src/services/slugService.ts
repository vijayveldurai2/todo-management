import { Workspace, Project, Board, Task } from '../types';

export function generateBaseSlug(name: string): string {
  return (name || 'untitled')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-') || 'item';
}

export function getRandomShortSuffix(): string {
  return Math.random().toString(36).substring(2, 7);
}

/**
 * Creates a unique slug guaranteed not to exist in the current parent scope.
 */
export async function createUniqueSlug(
  name: string,
  checkExists: (slugToCheck: string) => Promise<boolean> | boolean,
  isTodo: boolean = false
): Promise<string> {
  const base = generateBaseSlug(name);

  if (isTodo) {
    let slug = `${base}-${getRandomShortSuffix()}`;
    while (await checkExists(slug)) {
      slug = `${base}-${getRandomShortSuffix()}`;
    }
    return slug;
  } else {
    let slug = base;
    let counter = 2;
    while (await checkExists(slug)) {
      slug = `${base}-${counter++}`;
    }
    return slug;
  }
}

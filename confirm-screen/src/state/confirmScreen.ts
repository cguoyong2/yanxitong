import type { ConfirmScreenGiftEvent, ConfirmScreenStatus } from '../api/client';

const BINDING_KEY = 'yanxitong.confirmScreen.binding';
const EVENT_KEY = 'yanxitong.confirmScreen.currentGiftEvent';

export function saveBinding(binding: ConfirmScreenStatus): void {
  localStorage.setItem(BINDING_KEY, JSON.stringify(binding));
}

export function readBinding(): ConfirmScreenStatus | null {
  return readJson<ConfirmScreenStatus>(BINDING_KEY, localStorage);
}

export function clearBinding(): void {
  localStorage.removeItem(BINDING_KEY);
}

export function saveGiftEvent(event: ConfirmScreenGiftEvent): void {
  sessionStorage.setItem(EVENT_KEY, JSON.stringify(event));
}

export function readGiftEvent(): ConfirmScreenGiftEvent | null {
  return readJson<ConfirmScreenGiftEvent>(EVENT_KEY, sessionStorage);
}

function readJson<T>(key: string, storage: Storage): T | null {
  const raw = storage.getItem(key);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    storage.removeItem(key);
    return null;
  }
}

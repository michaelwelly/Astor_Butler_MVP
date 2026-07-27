/**
 * One-shot UI hints that should stop nagging once the user has shown they got
 * the message.
 *
 * Deliberately sessionStorage, not localStorage: nothing outlives the tab, so
 * these never become tracking state and stay clear of the consent gate. The
 * cost is that a returning visitor is taught once more, which for a hint is
 * the right trade.
 */

export const HINT_REELS_SWIPE = "c3flex.reels.swipe";
export const HINT_REELS_SOUND = "c3flex.reels.sound";
export const HINT_CHAT_SEEN = "c3flex.chat.seen";

export function learned(key: string): boolean {
  try {
    return sessionStorage.getItem(key) === "1";
  } catch {
    return false; // storage blocked → show the hint, it is harmless
  }
}

export function markLearned(key: string): void {
  try {
    sessionStorage.setItem(key, "1");
  } catch {
    /* storage blocked — the hint just shows again next time */
  }
}

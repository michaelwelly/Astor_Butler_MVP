/**
 * Every КП ends with a keyword — «ПАКЕТ», «РЕПОРТАЖ», «Хочу кино». A CTA button
 * anywhere on the site sends that word straight into the Butler chat instead of
 * opening an empty input and making the visitor retype it.
 *
 * A window event rather than a context: the chat widget is mounted once per
 * page, the buttons live several sections away, and nothing else needs to share
 * state with it.
 */
const ASK_EVENT = "butler:ask";

export function askButler(text: string) {
  window.dispatchEvent(new CustomEvent<string>(ASK_EVENT, { detail: text }));
}

export function onButlerAsk(handler: (text: string) => void) {
  const listener = (e: Event) => handler((e as CustomEvent<string>).detail);
  window.addEventListener(ASK_EVENT, listener);
  return () => window.removeEventListener(ASK_EVENT, listener);
}

/* ============================================================
   Astor Butler — presentation interactions.
   Vanilla JS, no build step. Graceful degradation everywhere:
   - no fine pointer  -> native cursor, no ripples;
   - reduced motion   -> everything visible, no animation;
   - no JS            -> content still readable (CSS keeps
                         .reveal visible via media query fallback
                         and hero opens on scroll).
   ============================================================ */
(function () {
  "use strict";

  const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(pointer: fine)").matches;

  /* ---------- 1. Key cursor + ripple trail ---------- */
  if (finePointer && !prefersReduced) {
    document.body.classList.add("key-cursor");
    const key = document.getElementById("cursorKey");
    const rippleLayer = document.getElementById("rippleLayer");
    let lastRipple = 0;

    document.addEventListener("mousemove", (e) => {
      key.style.transform =
        "translate(" + (e.clientX - 6) + "px," + (e.clientY - 6) + "px) rotate(-35deg)";
      const now = performance.now();
      if (now - lastRipple > 90) {
        lastRipple = now;
        const r = document.createElement("span");
        r.className = "ripple";
        const size = 40 + Math.random() * 50;
        r.style.width = r.style.height = size + "px";
        r.style.left = e.clientX + "px";
        r.style.top = e.clientY + "px";
        rippleLayer.appendChild(r);
        setTimeout(() => r.remove(), 1500);
      }
    }, { passive: true });

    document.addEventListener("mouseleave", () => { key.style.opacity = "0"; });
    document.addEventListener("mouseenter", () => { key.style.opacity = "1"; });
  }

  /* ---------- 2. Hero door opening ---------- */
  const hero = document.getElementById("hero");

  function openDoor() {
    if (hero.classList.contains("open")) return;
    hero.classList.add("opening");
    const delay = prefersReduced ? 0 : 550;
    setTimeout(() => hero.classList.add("open"), delay);
  }

  if (prefersReduced) {
    hero.classList.add("open");
  } else {
    // The key "turns" on first interaction: click, scroll or short timeout.
    hero.addEventListener("click", openDoor, { once: true });
    window.addEventListener("scroll", openDoor, { once: true, passive: true });
    setTimeout(openDoor, 1600);
  }

  const scrollHint = document.getElementById("scrollHint");
  if (scrollHint) {
    scrollHint.addEventListener("click", () => {
      document.getElementById("what").scrollIntoView({ behavior: prefersReduced ? "auto" : "smooth" });
    });
  }

  /* ---------- 3. Scroll reveal ---------- */
  const revealEls = document.querySelectorAll(".reveal");
  if ("IntersectionObserver" in window && !prefersReduced) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.15, rootMargin: "0px 0px -8% 0px" });
    revealEls.forEach((el) => io.observe(el));
  } else {
    revealEls.forEach((el) => el.classList.add("in"));
  }

  /* ---------- 4. Butler mascot: appears and changes by chapter ---------- */
  const butler = document.getElementById("butler");
  const butlerCaption = document.getElementById("butlerCaption");
  const moods = [
    { id: "what",     mood: "greet",  caption: "К вашим услугам" },
    { id: "journey",  mood: "listen", caption: "Слушаю вас" },
    { id: "brain",    mood: "listen", caption: "Понимаю" },
    { id: "voice",    mood: "listen", caption: "Слышу и вижу" },
    { id: "roles",    mood: "serve",  caption: "Передаю команде" },
    { id: "features", mood: "serve",  caption: "Держу границы" },
    { id: "compare",  mood: "greet",  caption: "Сравните сами" },
    { id: "business", mood: "serve",  caption: "К делу" },
    { id: "story",    mood: "greet",  caption: "История живая" },
    { id: "final",    mood: "bow",    caption: "Ваш ход" },
  ];

  if ("IntersectionObserver" in window) {
    const sectionIO = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        const conf = moods.find((m) => m.id === entry.target.id);
        if (!conf) return;
        butler.classList.add("visible");
        butler.setAttribute("data-mood", conf.mood);
        if (butlerCaption) butlerCaption.textContent = conf.caption;
      });
    }, { threshold: 0.3 });

    moods.forEach((m) => {
      const el = document.getElementById(m.id);
      if (el) sectionIO.observe(el);
    });

    // Hide butler while hero fills the screen.
    const heroIO = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && entry.intersectionRatio > 0.5) {
          butler.classList.remove("visible");
        }
      });
    }, { threshold: [0.5] });
    heroIO.observe(hero);
  }

  /* ---------- 5. Chat widget reveal + logic ---------- */
  const chatWidget = document.getElementById("chatWidget");
  const chatFab = document.getElementById("chatFab");
  const chatCloseBtn = document.getElementById("chatCloseBtn");
  const chatForm = document.getElementById("chatForm");
  const chatInput = document.getElementById("chatInput");
  const chatLog = document.getElementById("chatLog");
  const openChatBtn = document.getElementById("openChatBtn");
  let greeted = false;

  function showWidget() {
    chatWidget.setAttribute("aria-hidden", "false");
  }

  // Reveal FAB when the final section approaches.
  if ("IntersectionObserver" in window) {
    const finalIO = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) showWidget();
      });
    }, { threshold: 0.2 });
    const finalSection = document.getElementById("final");
    if (finalSection) finalIO.observe(finalSection);
  } else {
    showWidget();
  }

  function addBubble(text, who) {
    const b = document.createElement("div");
    b.className = "bubble " + (who === "guest" ? "guest" : "butler-msg");
    b.textContent = text;
    chatLog.appendChild(b);
    chatLog.scrollTop = chatLog.scrollHeight;
    return b;
  }

  function addTyping() {
    const t = document.createElement("div");
    t.className = "bubble butler-msg chat-typing";
    t.innerHTML = "<span></span><span></span><span></span>";
    chatLog.appendChild(t);
    chatLog.scrollTop = chatLog.scrollHeight;
    return t;
  }

  function openChat() {
    showWidget();
    chatWidget.classList.add("open");
    if (!greeted) {
      greeted = true;
      addBubble("Добрый вечер. Чем могу быть полезен: стол, меню или особая просьба?", "butler");
    }
    chatInput.focus();
  }

  function closeChat() {
    chatWidget.classList.remove("open");
    chatFab.focus();
  }

  chatFab.addEventListener("click", openChat);
  chatCloseBtn.addEventListener("click", closeChat);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && chatWidget.classList.contains("open")) closeChat();
  });

  if (openChatBtn) openChatBtn.addEventListener("click", openChat);

  chatForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const text = chatInput.value.trim();
    if (!text) return;
    chatInput.value = "";
    addBubble(text, "guest");
    const typing = addTyping();
    try {
      const reply = await window.AstorChat.submitMessage({ text });
      typing.remove();
      addBubble(reply.text, "butler");
    } catch (err) {
      typing.remove();
      addBubble("Прошу прощения, связь с рестораном сейчас недоступна. Попробуйте Telegram — там я всегда на месте.", "butler");
    }
  });
})();

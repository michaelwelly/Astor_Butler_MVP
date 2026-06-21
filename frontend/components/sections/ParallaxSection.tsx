"use client";

import { useEffect, useRef } from "react";
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

gsap.registerPlugin(ScrollTrigger);

const BG  = "/parallax/bg.png";
const MID = "/parallax/mid.png";
const FG  = "/parallax/fg.png";

export function ParallaxSection() {
  const sectionRef = useRef<HTMLElement>(null);
  const bgRef      = useRef<HTMLDivElement>(null);
  const midRef     = useRef<HTMLDivElement>(null);
  const fgRef      = useRef<HTMLDivElement>(null);
  const labelRef   = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = sectionRef.current;
    if (!el) return;

    const ctx = gsap.context(() => {
      // Start: layers exploded — bg up-left, fg down-right, mid stays
      gsap.set(bgRef.current,    { xPercent: -12, yPercent: -22, scale: 1.2, opacity: 0.55 });
      gsap.set(midRef.current,   { xPercent: 0,   yPercent: 0,   scale: 1.05, opacity: 0.6 });
      gsap.set(fgRef.current,    { xPercent: 10,  yPercent: 20,  scale: 1.18, opacity: 0.5 });
      gsap.set(labelRef.current, { opacity: 0, yPercent: 22 });

      const tl = gsap.timeline({
        scrollTrigger: {
          trigger: el,
          start: "top top",
          end: "+=220%",
          scrub: 2.5,
          pin: true,
        },
      });

      // Phase 1 (0→0.7): layers converge and assemble
      tl.to(bgRef.current,  { xPercent: 0, yPercent: 0, scale: 1.04, opacity: 1, ease: "power2.inOut" }, 0)
        .to(midRef.current, { scale: 1.0, opacity: 1, ease: "power2.inOut" }, 0)
        .to(fgRef.current,  { xPercent: 0, yPercent: 0, scale: 1.04, opacity: 1, ease: "power2.inOut" }, 0)
        // Phase 2 (0.45→0.72): label fades in once assembled
        .to(labelRef.current, { opacity: 1, yPercent: 0, ease: "power2.out" }, 0.45)
        // Phase 3 (0.76→1): section fades to reveal catalog
        .to(el, { opacity: 0, ease: "power1.in", duration: 0.26 }, 0.76);
    }, el);

    return () => ctx.revert();
  }, []);

  return (
    <section ref={sectionRef} className="parallax-section">
      <div className="grain" />

      <div ref={bgRef} className="parallax-layer parallax-bg">
        <img src={BG} alt="" aria-hidden="true" />
      </div>
      <div ref={midRef} className="parallax-layer parallax-mid">
        <img src={MID} alt="" aria-hidden="true" />
      </div>
      <div ref={fgRef} className="parallax-layer parallax-fg">
        <img src={FG} alt="" aria-hidden="true" />
      </div>

      <div ref={labelRef} className="parallax-label">
        <span>C3FLEX</span>
        <p>Кинематографическая площадка</p>
      </div>
    </section>
  );
}

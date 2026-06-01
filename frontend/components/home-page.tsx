"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import Lenis from "lenis";
import {
  ArrowDown,
  ArrowUpRight,
  Check,
  Menu,
  Pause,
  Play,
  Plus,
  Send,
  Volume2,
  VolumeX,
  X,
} from "lucide-react";
import { directions, portfolioCases, servicePackages, type PortfolioCase } from "@/lib/portfolio";
import { submitLead, type LeadRequest } from "@/lib/lead-api";

const emptyLead: LeadRequest = {
  projectType: "",
  description: "",
  services: "",
  format: "",
  deadline: "",
  budget: "",
  contact: "",
};

export function HomePage() {
  const heroVideo = useRef<HTMLVideoElement>(null);
  const [soundOn, setSoundOn] = useState(false);
  const [playing, setPlaying] = useState(true);
  const [menuOpen, setMenuOpen] = useState(false);
  const [selectedCase, setSelectedCase] = useState<PortfolioCase | null>(null);
  const [lead, setLead] = useState(emptyLead);
  const [leadStatus, setLeadStatus] = useState<"idle" | "sending" | "sent" | "error">("idle");

  const heroCase = portfolioCases[0];
  const currentYear = useMemo(() => new Date().getFullYear(), []);

  useEffect(() => {
    const lenis = new Lenis({ lerp: 0.08, smoothWheel: true });
    let frame = 0;
    const raf = (time: number) => {
      lenis.raf(time);
      frame = requestAnimationFrame(raf);
    };
    frame = requestAnimationFrame(raf);
    return () => {
      cancelAnimationFrame(frame);
      lenis.destroy();
    };
  }, []);

  const toggleSound = () => {
    if (!heroVideo.current) return;
    heroVideo.current.muted = soundOn;
    setSoundOn(!soundOn);
  };

  const togglePlayback = () => {
    if (!heroVideo.current) return;
    if (playing) {
      heroVideo.current.pause();
    } else {
      void heroVideo.current.play();
    }
    setPlaying(!playing);
  };

  const handleLeadChange = (field: keyof LeadRequest, value: string) => {
    setLead((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLeadStatus("sending");
    try {
      await submitLead(lead);
      setLeadStatus("sent");
      setLead(emptyLead);
    } catch {
      setLeadStatus("error");
    }
  };

  return (
    <main>
      <header className="site-header">
        <a className="brand" href="#top" aria-label="C3FLEX home">
          C3<span>FLEX</span><sup>.com</sup>
        </a>
        <nav className="desktop-nav" aria-label="Main navigation">
          <a href="#work">Work</a>
          <a href="#directions">Directions</a>
          <a href="#services">Services</a>
          <a href="#contact">Brief us</a>
        </nav>
        <div className="header-actions">
          <a className="circle-link" href="#contact" aria-label="Start a project">
            <ArrowUpRight size={18} />
          </a>
          <button className="menu-button" type="button" onClick={() => setMenuOpen(true)} aria-label="Open menu">
            <Menu size={20} />
          </button>
        </div>
      </header>

      <section className="hero" id="top">
        <video
          className="hero-video"
          ref={heroVideo}
          src={heroCase.video}
          poster={heroCase.image}
          autoPlay
          loop
          muted
          playsInline
        />
        <div className="hero-overlay" />
        <div className="grain" />
        <div className="hero-copy">
          <p className="eyebrow"><span /> Independent production studio</p>
          <h1>Stories that<br /><i>stay</i> with you.</h1>
          <p className="hero-description">
            C3FLEX turns fleeting moments, tactile products and ambitious campaigns into films with a pulse.
          </p>
          <div className="hero-links">
            <a className="primary-link" href="#work">Explore the work <ArrowDown size={16} /></a>
            <a className="text-link" href="#contact">Start a project <ArrowUpRight size={15} /></a>
          </div>
        </div>
        <div className="hero-meta">
          <span>Featured / {heroCase.title}</span>
          <span>{heroCase.duration}</span>
        </div>
        <div className="video-controls">
          <button type="button" onClick={togglePlayback} aria-label={playing ? "Pause video" : "Play video"}>
            {playing ? <Pause size={15} /> : <Play size={15} />}
          </button>
          <button type="button" onClick={toggleSound} aria-label={soundOn ? "Mute video" : "Unmute video"}>
            {soundOn ? <Volume2 size={16} /> : <VolumeX size={16} />}
          </button>
        </div>
        <div className="hero-index">
          <span>01</span>
          <div><b /><b /><b /></div>
          <span>03</span>
        </div>
      </section>

      <section className="manifesto section-pad">
        <p className="section-label">C3FLEX / Production with intent</p>
        <h2>We capture the energy<br />before it <i>disappears.</i></h2>
        <div className="manifesto-grid">
          <p>From the rush of a live room to the smallest movement of light across a product, every detail is a reason to look closer.</p>
          <p>Our films feel editorial, move with purpose and leave enough space for the viewer to step inside.</p>
        </div>
      </section>

      <section className="work section-pad" id="work">
        <div className="section-heading">
          <p className="section-label">Selected work / {currentYear}</p>
          <h2>Move through<br /><i>the feeling.</i></h2>
          <p>Three directions. One restless eye.</p>
        </div>
        <div className="case-grid">
          {portfolioCases.map((item, index) => (
            <motion.button
              className={`case-card case-card-${index + 1}`}
              key={item.id}
              type="button"
              onClick={() => setSelectedCase(item)}
              initial={{ opacity: 0, y: 36 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.18 }}
              transition={{ duration: 0.7, delay: index * 0.09 }}
            >
              <img src={item.image} alt="" />
              <span className="case-shade" />
              <span className="case-top"><em>{item.category}</em><em>{item.year}</em></span>
              <span className="case-bottom">
                <strong>{item.title}</strong>
                <span>{item.kicker}</span>
              </span>
              <span className="case-open"><Plus size={18} /></span>
            </motion.button>
          ))}
        </div>
      </section>

      <section className="directions section-pad" id="directions">
        <div className="section-heading compact">
          <p className="section-label">Choose your direction</p>
          <h2>Every story has<br />its own <i>temperature.</i></h2>
        </div>
        <div className="direction-list">
          {directions.map((direction) => (
            <a className="direction-row" href="#work" key={direction.id}>
              <span className="direction-number">{direction.index}</span>
              <span className="direction-title">{direction.title}</span>
              <span className="direction-description">{direction.description}</span>
              <span className="direction-arrow"><ArrowUpRight size={21} /></span>
            </a>
          ))}
        </div>
      </section>

      <section className="services section-pad" id="services">
        <div className="section-heading compact">
          <p className="section-label">Ways to work together</p>
          <h2>Production,<br /><i>made clear.</i></h2>
        </div>
        <div className="service-grid">
          {servicePackages.map((item) => (
            <article className="service-card" key={item.number}>
              <span>{item.number}</span>
              <h3>{item.title}</h3>
              <p>{item.description}</p>
              <div><strong>{item.price}</strong><ArrowUpRight size={17} /></div>
            </article>
          ))}
        </div>
      </section>

      <section className="contact" id="contact">
        <div className="contact-copy">
          <p className="section-label">Start a project</p>
          <h2>Tell us what<br />you want people<br /><i>to feel.</i></h2>
          <p>Share a few essentials. We will come back with the right production shape.</p>
          <a href="https://t.me/" target="_blank" rel="noreferrer">Prefer Telegram? Talk to us directly <ArrowUpRight size={15} /></a>
        </div>
        <form className="brief-form" onSubmit={handleSubmit}>
          <label>
            <span>Project type</span>
            <input required value={lead.projectType} onChange={(event) => handleLeadChange("projectType", event.target.value)} placeholder="Campaign, event, reels..." />
          </label>
          <label className="wide">
            <span>What are we making?</span>
            <textarea required value={lead.description} onChange={(event) => handleLeadChange("description", event.target.value)} placeholder="A few lines about the task, mood and audience." />
          </label>
          <label>
            <span>Needed services</span>
            <input value={lead.services} onChange={(event) => handleLeadChange("services", event.target.value)} placeholder="Concept, shoot, edit..." />
          </label>
          <label>
            <span>Format</span>
            <input value={lead.format} onChange={(event) => handleLeadChange("format", event.target.value)} placeholder="Reels, film, podcast..." />
          </label>
          <label>
            <span>Deadline</span>
            <input value={lead.deadline} onChange={(event) => handleLeadChange("deadline", event.target.value)} placeholder="When should it go live?" />
          </label>
          <label>
            <span>Budget</span>
            <input value={lead.budget} onChange={(event) => handleLeadChange("budget", event.target.value)} placeholder="Your comfortable range" />
          </label>
          <label className="wide">
            <span>Name and contact</span>
            <input required value={lead.contact} onChange={(event) => handleLeadChange("contact", event.target.value)} placeholder="Telegram, email or phone" />
          </label>
          <button className="submit-button" disabled={leadStatus === "sending"} type="submit">
            {leadStatus === "sending" ? "Sending..." : leadStatus === "sent" ? "Brief received" : "Send brief"}
            {leadStatus === "sent" ? <Check size={17} /> : <Send size={16} />}
          </button>
          {leadStatus === "error" && <p className="form-error">Something went wrong. Please try again.</p>}
        </form>
      </section>

      <footer>
        <a className="brand" href="#top">C3<span>FLEX</span><sup>.com</sup></a>
        <p>Independent production studio / {currentYear}</p>
        <div><a href="#work">Work</a><a href="#contact">Contact</a></div>
      </footer>

      <AnimatePresence>
        {menuOpen && (
          <motion.div className="menu-overlay" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <button type="button" onClick={() => setMenuOpen(false)} aria-label="Close menu"><X size={22} /></button>
            <p>C3FLEX / Navigate</p>
            {["Work", "Directions", "Services", "Brief us"].map((item, index) => (
              <a key={item} href={`#${item === "Brief us" ? "contact" : item.toLowerCase()}`} onClick={() => setMenuOpen(false)}>
                <span>0{index + 1}</span>{item}
              </a>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {selectedCase && (
          <motion.div className="case-modal" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <motion.div className="case-modal-card" initial={{ y: 50 }} animate={{ y: 0 }} exit={{ y: 50 }}>
              <video src={selectedCase.video} poster={selectedCase.image} autoPlay loop muted playsInline />
              <button type="button" onClick={() => setSelectedCase(null)} aria-label="Close case"><X size={20} /></button>
              <div>
                <p>{selectedCase.category} / {selectedCase.year}</p>
                <h2>{selectedCase.title}</h2>
                <span>{selectedCase.statement}</span>
                <a href="#contact" onClick={() => setSelectedCase(null)}>Make something together <ArrowUpRight size={15} /></a>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </main>
  );
}

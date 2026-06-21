"use client";

import { useRef, useState } from "react";

export function useVideoPlayer() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [soundOn, setSoundOn] = useState(false);
  const [playing, setPlaying] = useState(true);

  const toggleSound = () => {
    if (!videoRef.current) return;
    videoRef.current.muted = soundOn;
    setSoundOn((prev) => !prev);
  };

  const togglePlayback = () => {
    if (!videoRef.current) return;
    if (playing) {
      videoRef.current.pause();
    } else {
      void videoRef.current.play();
    }
    setPlaying((prev) => !prev);
  };

  return { videoRef, soundOn, playing, toggleSound, togglePlayback };
}

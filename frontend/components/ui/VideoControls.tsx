import { Pause, Play, Volume2, VolumeX } from "lucide-react";

type Props = {
  playing: boolean;
  soundOn: boolean;
  onTogglePlay: () => void;
  onToggleSound: () => void;
};

export function VideoControls({ playing, soundOn, onTogglePlay, onToggleSound }: Props) {
  return (
    <div className="video-controls">
      <button type="button" onClick={onTogglePlay} aria-label={playing ? "Pause video" : "Play video"}>
        {playing ? <Pause size={15} /> : <Play size={15} />}
      </button>
      <button type="button" onClick={onToggleSound} aria-label={soundOn ? "Mute video" : "Unmute video"}>
        {soundOn ? <Volume2 size={16} /> : <VolumeX size={16} />}
      </button>
    </div>
  );
}

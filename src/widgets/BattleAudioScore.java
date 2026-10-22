package widgets;

import java.util.Iterator;
import static widgets.BattleAudio.MOTIF;
import static widgets.BattleAudio.PENTATONIC;

/**
 * The score of the soundtrack: the chords and the pad, and the steps of each song, which play their voices in the mixer
 */
final class BattleAudioScore {

	private final BattleAudio host;

	/** Creates the score of the given battle audio. */
	BattleAudioScore(BattleAudio host) {
		this.host = host;
	}

	/**
	 * The notes and drums of each 16th
	 */
	void onStep(int step, double tension) {
		Song playing = host.song;
		int inBar = step % 16;
		int bar = (step / 16) % playing.roots.length;
		double root = playing.roots[bar];
		boolean minor = playing.minor[bar];

		if (host.slow > 0.5) {
			// Only a heartbeat
			if (inBar == 0 || inBar == 8) {
				host.musicVoices.add(new Voice(host.heart, 0.9, 1));
			}
			return;
		}
		switch (playing) {
		case EPIC -> this.epicStep(step, inBar, root, minor, tension);
		case SYNTHWAVE -> this.synthwaveStep(step, inBar, root, minor, tension);
		case WAR_DRUMS -> this.drumsStep(step, inBar, bar, root, tension);
		}
	}

	/** Plays one step of the epic loop: the drums and the bass, more of them as the tension grows. */
	void epicStep(int step, int inBar, double root, boolean minor, double tension) {

		boolean fourOnFloor = tension > 0.6;
		if (inBar == 0 || inBar == 8 || (tension > 0.15 && inBar == 10) || (fourOnFloor && inBar % 4 == 0)) {
			if (tension > 0.12) {
				host.musicVoices.add(new Voice(host.kick, 0.85, 1));
			}
		}
		if (tension > 0.45 && (inBar == 4 || inBar == 12)) {
			host.musicVoices.add(new Voice(host.snare, 0.55, 1));
		}
		if (tension > 0.3 && (inBar % 2 == 0 || tension > 0.72)) {
			host.musicVoices.add(new Voice(host.hat, inBar % 4 == 2 ? 0.28 : 0.16, 1));
		}
		if (tension > 0.25 && inBar % 2 == 0) {
			double[] pattern = { 1, 1, 2, 1, 1.5, 1, 2, 1.5 };
			double frequency = root * pattern[(inBar / 2) % pattern.length];
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.saw(frequency, 0.16, 6), 0.25), 0.5, 1));
		}
		if (tension > 0.65) {
			double[] chord = minor ? new double[] { 1, 1.189, 1.498, 2, 2.378, 2.997 } : new double[] { 1, 1.26, 1.498, 2, 2.52, 2.997 };
			double frequency = root * 4 * chord[(step * (tension > 0.85 ? 3 : 1)) % chord.length];
			host.musicVoices.add(new Voice(Synth.square(frequency, 0.09), 0.13, 1));
		}
		if (tension > 0.88 && (inBar == 0 || inBar == 8)) {
			host.musicVoices.add(new Voice(Synth.sweep(880, 1320, 0.35, false), 0.12, 1));
		}
	}

	/**
	 * Synthwave: a bass bouncing in octaves every 16th, four on the floor,
	 * backbeat, an arpeggiated lead and tom fills at the top
	 */
	void synthwaveStep(int step, int inBar, double root, boolean minor, double tension) {
		// Bass: 8ths when calm, 16ths bouncing in octaves when the fight heats up
		if (inBar % 2 == 0 || tension > 0.3) {
			double frequency = root * (inBar % 2 == 1 ? 2 : 1);
			double[] bass = Synth.lowpass(Synth.mix(Synth.saw(frequency, 0.14, 9), Synth.saw(frequency * 1.006, 0.14, 9), 0.6), 0.12 + 0.2 * tension);
			host.musicVoices.add(new Voice(bass, 0.32 + 0.25 * Math.min(1, tension * 2), 1));
		}
		if (tension > 0.18 && inBar % 4 == 0) {
			host.musicVoices.add(new Voice(host.kick, 0.85, 1));
		}
		if (tension > 0.35 && (inBar == 4 || inBar == 12)) {
			// A long snare, like the gated reverb of the 80s
			host.musicVoices.add(new Voice(Synth.mix(host.snare, Synth.lowpass(Synth.noise(0.32, 2), 0.4), 0.5), 0.55, 1));
		}
		if (tension > 0.3 && (inBar % 4 == 2 || tension > 0.7)) {
			host.musicVoices.add(new Voice(host.hat, inBar % 4 == 2 ? 0.24 : 0.12, 1));
		}
		if (tension > 0.5 && inBar % 2 == 0) {
			double[] chord = minor ? new double[] { 1, 1.189, 1.498, 2, 1.498, 1.189, 2, 2.378 } : new double[] { 1, 1.26, 1.498, 2, 1.498, 1.26, 2, 2.52 };
			double frequency = root * 4 * chord[(inBar / 2) % chord.length] * (tension > 0.82 && inBar >= 8 ? 2 : 1);
			double[] lead = Synth.lowpass(Synth.mix(Synth.saw(frequency, 0.2, 7), Synth.saw(frequency * 1.007, 0.2, 7), 0.8), 0.35);
			host.musicVoices.add(new Voice(lead, 0.16, 1));
		}
		if (tension > 0.75 && (step / 16) % 2 == 1 && inBar >= 12) {
			// High, mid and low tom going down to the next bar
			host.musicVoices.add(new Voice(host.toms[Math.max(0, inBar - 13)], 0.5, 1));
		}
		if (tension > 0.9 && inBar == 0) {
			host.musicVoices.add(new Voice(Synth.sweep(root * 8, root * 12, 0.4, false), 0.1, 1));
		}
	}

	/**
	 * War drums, a groove: a wood block clave that never stops, taikos in a
	 * syncopated ostinato, small drums answering them, hats and a plucked bass in
	 * the pulse, and a kalimba motif that calls and answers. When the fight heats
	 * up, the taikos fill the bar, the kick comes on every beat, the kalimba plays
	 * eighths, and war horns close the phrases.
	 */
	void drumsStep(int step, int inBar, int bar, double root, double tension) {
		// The wood block clave (3-2), the pulse under everything
		if (inBar == 0 || inBar == 3 || inBar == 6 || inBar == 10 || inBar == 12) {
			host.musicVoices.add(new Voice(host.woodBlock, inBar == 0 ? 0.5 : 0.36, 1));
		}
		// Taikos: a syncopated ostinato, sparse when calm, the whole pattern when tense
		boolean calmTaiko = inBar == 0 || inBar == 8 || inBar == 10;
		boolean tenseTaiko = calmTaiko || inBar == 3 || inBar == 6 || inBar == 12 || inBar == 14;
		if (tension > 0.5 ? tenseTaiko : calmTaiko) {
			host.musicVoices.add(new Voice(host.taiko, 0.45 + 0.5 * Math.min(1, tension * 1.6), inBar == 0 ? 1 : 1.12));
		}
		// The kick on every beat when the fight heats up
		if (tension > 0.6 && inBar % 4 == 0) {
			host.musicVoices.add(new Voice(host.kick, 0.6, 1));
		}
		// Small drums answer the taikos
		if (tension > 0.2 && (inBar == 5 || inBar == 7 || inBar == 13 || inBar == 15 || (tension > 0.6 && inBar % 2 == 1))) {
			host.musicVoices.add(new Voice(host.smallDrum, 0.3 + 0.15 * tension, 0.95 + 0.1 * ((inBar * 7) % 3)));
		}
		this.hatsAndBass(inBar, root, tension);
		this.kalimbaMotif(step, inBar, bar, root, tension);
		this.warHorns(inBar, bar, root, tension);
	}

	/** Hats on the eighths, ghost notes on the sixteenths, and a plucked bass in the pulse: the root, and the octave on the off beats */
	void hatsAndBass(int inBar, double root, double tension) {
		if (tension > 0.25 && (inBar % 2 == 0 || tension > 0.55)) {
			double gain = inBar % 2 == 1 ? 0.08 : inBar % 4 == 2 ? 0.24 : 0.16;
			host.musicVoices.add(new Voice(host.hat, gain, 1));
		}
		if (tension > 0.15 && (inBar % 2 == 0 || tension > 0.5)) {
			double frequency = root * (inBar % 4 == 2 ? 2 : 1);
			host.musicVoices.add(new Voice(Synth.lowpass(Synth.saw(frequency, 0.14, 12), 0.25), 0.22 + 0.2 * tension, 1));
		}
	}

	/** The kalimba motif: the first two bars call, the next two answer two steps higher */
	void kalimbaMotif(int step, int inBar, int bar, double root, double tension) {
		if (tension > 0.12 && (inBar % 4 == 0 || (tension > 0.3 && inBar % 2 == 0))) {
			int degree = MOTIF[(step / 2) % MOTIF.length] + (bar >= 2 ? 2 : 0);
			double frequency = root * 4 * PENTATONIC[Math.floorMod(degree, PENTATONIC.length)];
			double[] kalimba = Synth.mix(Synth.sine(frequency, 0.35, 7), Synth.sine(frequency * 2.01, 0.35, 16), 0.3);
			host.musicVoices.add(new Voice(kalimba, 0.2 + 0.1 * tension, 1));
		}
	}

	/** War horns close the phrases at the top, and a roll sits under everything when the fight is at its peak */
	void warHorns(int inBar, int bar, double root, double tension) {
		if (tension > 0.8 && inBar == 0 && bar % 2 == 0) {
			double[] horn = Synth.lowpass(Synth.mix(Synth.saw(root * 2, 1.5, 1.1), Synth.saw(root * 3, 1.5, 1.1), 0.6), 0.06);
			host.musicVoices.add(new Voice(horn, 0.35, 1));
		}
		if (tension > 0.9) {
			// A roll under everything
			host.musicVoices.add(new Voice(host.smallDrum, 0.12, 1.3));
		}
	}

	/**
	 * One sample of the soundtrack
	 */
	double music() {
		double tension = host.intensity;
		Song current = host.song;
		double bpm = current.calmBpm + current.tenseBpm * tension;
		if (host.slow > 0.5) {
			bpm = 52;
		}
		host.beat += bpm / 60 / Synth.RATE;
		int step = (int) Math.floor(host.beat * 4);
		if (step != host.lastStep) {
			host.lastStep = step;
			this.onStep(step, tension);
		}

		int bar = (int) Math.floor(host.beat / 4) % current.roots.length;
		double pad = this.pad(current, bar, tension);

		double voices = 0;
		for (Iterator<Voice> iterator2 = host.musicVoices.iterator(); iterator2.hasNext();) {
			Voice playing = iterator2.next();
			voices += playing.next();
			if (playing.finished()) {
				iterator2.remove();
			}
		}
		return pad + voices;
	}

	/** The pad: three detuned saws through a low pass that opens with the tension, and the pump of synthwave with the kick */
	double pad(Song current, int bar, double tension) {
		// Pad: three detuned saws through a low pass that opens with the tension.
		// The drums have a drone instead: root, fifth and octave, darker.
		host.padFrequency += (current.roots[bar] - host.padFrequency) * 0.0005;
		double third = current == Song.WAR_DRUMS ? 1 : current.minor[bar] ? 1.189 : 1.26;
		host.padPhase1 = (host.padPhase1 + host.padFrequency / Synth.RATE) % 1;
		host.padPhase2 = (host.padPhase2 + host.padFrequency * 1.004 * 1.498 / Synth.RATE) % 1;
		host.padPhase3 = (host.padPhase3 + host.padFrequency * 0.997 * third * 2 / Synth.RATE) % 1;
		double raw = (host.padPhase1 * 2 - 1) + (host.padPhase2 * 2 - 1) * 0.7 + (host.padPhase3 * 2 - 1) * 0.5;
		double cutoff = (current == Song.WAR_DRUMS ? 0.006 + 0.02 * tension : current == Song.SYNTHWAVE ? 0.02 + 0.05 * tension : 0.012 + 0.05 * tension) * (1 - 0.7 * host.slow);
		host.padFilter += (raw - host.padFilter) * cutoff;
		double pad = host.padFilter * (current == Song.WAR_DRUMS ? 0.1 : 0.32 - 0.1 * tension);
		if (current == Song.SYNTHWAVE && tension > 0.2 && host.slow < 0.5) {
			// Pumps with the kick, the synthwave sound
			double inBeat = host.beat - Math.floor(host.beat);
			pad *= 1 - 0.65 * Math.exp(-inBeat * 7);
		}
		return pad;
	}
}

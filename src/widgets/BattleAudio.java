package widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The sound of a 3D battle: an adaptive soundtrack and the sound effects,
 * mixed together in real time in a single audio line, everything synthesized.
 *
 * The music follows the {@link #setIntensity(double) intensity} of the fight:
 * calm it is a dark pad, then come the drums, the bass, the melody and at last
 * an alarm, while the tempo grows. There are three {@link Song songs}. In slow
 * motion the music becomes a muffled heartbeat and the effects sound deep and
 * slow.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class BattleAudio {

	private static final int CHUNK = 512;
	private static final double MUSIC_GAIN = 0.75;
	private static final double SFX_GAIN = 0.9;

	/** The adaptive songs */
	public enum Song {
		/** A minor, i - VI - III - VII: pad, kick, snare, arpeggio */
		EPIC("Épica", new double[] { 110.0, 87.31, 130.81, 98.0 }, new boolean[] { true, false, false, false }, 96, 54),
		/** E minor, i - VI - VII - v: pumping pad, bouncing bass, lead and toms */
		SYNTHWAVE("Synthwave", new double[] { 82.41, 65.41, 73.42, 61.74 }, new boolean[] { true, false, false, true }, 108, 44),
		/** D minor drone: taikos, small drums, a pentatonic flute and war horns */
		WAR_DRUMS("Tambores de guerra", new double[] { 73.42, 73.42, 58.27, 65.41 }, new boolean[] { true, true, false, false }, 80, 56);

		public final String label;
		final double[] roots;
		final boolean[] minor;
		final double calmBpm;
		final double tenseBpm;

		Song(String label, double[] roots, boolean[] minor, double calmBpm, double moreBpm) {
			this.label = label;
			this.roots = roots;
			this.minor = minor;
			this.calmBpm = calmBpm;
			this.tenseBpm = moreBpm;
		}

		/** Random songs do not repeat until all were played */
		private static final Rotation<Song> ROTATION = new Rotation<Song>(values());

		/**
		 * The configured song, the next of the rotation if not a song (like
		 * "RANDOM")
		 */
		public static Song resolve(String configured, Random random) {
			try {
				return Song.valueOf(configured);
			} catch (IllegalArgumentException | NullPointerException e) {
				return ROTATION.next(random);
			}
		}
	}

	/** Pentatonic minor, for the flute */
	private static final double[] PENTATONIC = { 1, 1.189, 1.335, 1.498, 1.782, 2, 2.378, 2.67 };

	private final ConcurrentLinkedQueue<Voice> incoming = new ConcurrentLinkedQueue<Voice>();
	private final Random random = new Random();
	private Thread thread;
	private volatile boolean running;

	// Controls, written by the JavaFX thread
	private volatile double targetIntensity = 0;
	private volatile boolean slowMotion = false;
	private volatile boolean musicOn = true;
	private volatile Song song = Song.EPIC;
	/** The volume of the music, from 0 to 1. The effects are not affected. */
	private volatile double musicLevel = 1;

	// Mixer state, only the audio thread touches it
	private final List<Voice> sfxVoices = new ArrayList<Voice>();
	private final List<Voice> musicVoices = new ArrayList<Voice>();
	private double intensity = 0;
	private double musicVolume = 0;
	private double slow = 0;
	private double duck = 1;
	private double beat = 0;
	private int lastStep = -1;
	private double padPhase1;
	private double padPhase2;
	private double padPhase3;
	private double padFrequency = Song.EPIC.roots[0];
	private double padFilter;
	private double sfxFilter;
	private final double[] echo = new double[(int) (Synth.RATE * 0.13)];
	private int echoPosition;

	// Ready made sounds
	private final double[] kick = kick();
	private final double[] snare = snare();
	private final double[] hat = hat();
	private final double[] heart = heartbeat();
	private final double[] taiko = taiko();
	private final double[] smallDrum = smallDrum();
	private final double[] rim = rim();
	private final double[][] toms = { Synth.sineSweep(230, 115, 0.28), Synth.sineSweep(175, 88, 0.3), Synth.sineSweep(130, 62, 0.34) };

	/**
	 * A sound being played, at a rate (pitch) and gain
	 */
	private static final class Voice {
		final double[] samples;
		final double gain;
		final double rate;
		double position;

		Voice(double[] samples, double gain, double rate) {
			this.samples = samples;
			this.gain = gain;
			this.rate = rate;
		}

		boolean finished() {
			return this.position >= this.samples.length - 1;
		}

		double next() {
			int index = (int) this.position;
			double fraction = this.position - index;
			double value = this.samples[index] + (this.samples[Math.min(index + 1, this.samples.length - 1)] - this.samples[index]) * fraction;
			this.position += this.rate;
			return value * this.gain;
		}
	}

	// ------------------------------------------------------------------- control

	public synchronized void start() {
		if (this.running) {
			return;
		}
		this.running = true;
		this.thread = new Thread(this::run, "battle-audio");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public synchronized void stop() {
		this.running = false;
	}

	/**
	 * How tense the fight is, from 0 (calm) to 1 (last breath)
	 */
	public void setIntensity(double intensity) {
		this.targetIntensity = Math.max(0, Math.min(1, intensity));
	}

	/**
	 * Slow motion: muffled music, heartbeat, deep effects
	 */
	public void setSlowMotion(boolean slowMotion) {
		this.slowMotion = slowMotion;
	}

	public void setMusicOn(boolean musicOn) {
		this.musicOn = musicOn;
	}

	/**
	 * @param level
	 *            from 0 (no music) to 1
	 */
	public void setMusicLevel(double level) {
		this.musicLevel = Math.max(0, Math.min(1, level));
	}

	public void setSong(Song song) {
		this.song = song;
	}

	public Song getSong() {
		return this.song;
	}

	// ------------------------------------------------------------------ effects

	/**
	 * A cannon shot, heavier with more power (0.1 to 3)
	 */
	public void shot(double power) {
		double weight = Math.max(0, Math.min(1, (power - 0.1) / 2.9));
		double[] crack = Synth.mix(Synth.lowpass(Synth.noise(0.09, 3), 0.55), Synth.sweep(1800 - 900 * weight, 160, 0.12, true), 0.7);
		double[] thump = Synth.sineSweep(110 - 40 * weight, 45, 0.22);
		this.sfx(Synth.mix(crack, thump, 0.5 + 0.5 * weight), 0.45 + 0.4 * weight, this.vary());
	}

	/**
	 * A bullet hitting a robot: crunch, metallic ring and a low boom
	 */
	public void hit(double power) {
		double weight = Math.max(0, Math.min(1, power / 3));
		double[] crunch = Synth.lowpass(Synth.noise(0.35, 2.2), 0.35);
		double[] ring = Synth.mix(Synth.sine(930, 0.4, 7), Synth.sine(1395, 0.4, 9), 0.5);
		double[] boom = Synth.sineSweep(90, 35, 0.5);
		this.sfx(Synth.mix(Synth.mix(crunch, ring, 0.25), boom, 0.6), 0.7 + 0.3 * weight, this.vary());
		this.duck = Math.min(this.duck, 0.75);
	}

	/**
	 * Bullets hitting each other or a wall
	 */
	public void spark() {
		this.sfx(Synth.mix(Synth.lowpass(Synth.noise(0.12, 4), 0.6), Synth.sine(2200, 0.12, 25), 0.3), 0.35, this.vary());
	}

	/**
	 * A robot blowing up: a long and deep explosion with a second blast
	 */
	public void destroyed() {
		double[] blast = Synth.mix(Synth.lowpass(Synth.noise(2.6, 1.1), 0.12), Synth.sineSweep(75, 22, 2.2), 0.65);
		double[] crackle = Synth.crackle(1.8);
		double[] second = Synth.concat(Synth.silence(0.35), Synth.mix(Synth.lowpass(Synth.noise(1.4, 1.6), 0.2), Synth.sineSweep(60, 25, 1.2), 0.6));
		this.sfx(Synth.mix(Synth.mix(blast, crackle, 0.25), second, 0.45), 1.0, 1);
		this.duck = 0.25;
	}

	/** A robot hitting a wall or another robot */
	public void thud() {
		this.sfx(Synth.sineSweep(70, 40, 0.18), 0.5, this.vary());
	}

	/** Alarm when a robot is about to die */
	public void lowEnergy() {
		this.sfx(Synth.concat(Synth.square(1320, 0.08), Synth.silence(0.06), Synth.square(1320, 0.08)), 0.22, 1);
	}

	/** "ROUND N": an impact */
	public void roundCall() {
		this.sfx(Synth.mix(Synth.sineSweep(120, 40, 0.9), Synth.lowpass(Synth.noise(0.9, 2.5), 0.15), 0.6), 0.8, 1);
	}

	/** One number of the countdown: a beep, higher for the last */
	public void countdown(boolean last) {
		this.sfx(Synth.mix(Synth.sine(last ? 880 : 660, 0.25, 6), Synth.square(last ? 880 : 660, 0.2), 0.15), 0.7, 1);
	}

	/** "FIGHT!": a brass like stab */
	public void fight() {
		double[] stab = new double[(int) (Synth.RATE * 0.9)];
		for (double frequency : new double[] { 220, 261.63, 329.63, 440, 523.25 }) {
			stab = Synth.mix(stab, Synth.saw(frequency, 0.9, 2.5), 0.85);
		}
		this.sfx(Synth.mix(stab, Synth.sineSweep(100, 40, 0.6), 0.5), 0.75, 1);
		this.duck = 0.5;
	}

	/** "K.O.!": a big hit and a falling tone */
	public void knockOut() {
		this.sfx(Synth.mix(Synth.mix(Synth.sineSweep(80, 20, 1.6), Synth.lowpass(Synth.noise(1.5, 1.4), 0.15), 0.6), Synth.saw(110, 1.5, 1.2), 0.3), 1.0, 1);
		this.duck = 0.2;
	}

	/** Entering the replay: a reversed rising whoosh */
	public void whoosh() {
		double[] rise = Synth.lowpass(Synth.noise(0.9, 0), 0.25);
		for (int i = 0; i < rise.length; i++) {
			double t = (double) i / rise.length;
			rise[i] *= t * t;
		}
		this.sfx(Synth.mix(rise, Synth.sineSweep(200, 900, 0.9), 0.3), 0.6, 1);
	}

	/**
	 * The mix of some seconds of music at a fixed intensity, without a sound
	 * card: for tests and previews
	 */
	static double[] render(Song song, double intensity, double seconds) {
		return render(song, intensity, seconds, 1);
	}

	static double[] render(Song song, double intensity, double seconds, double musicLevel) {
		BattleAudio audio = new BattleAudio();
		audio.song = song;
		audio.musicLevel = musicLevel;
		audio.intensity = intensity;
		audio.targetIntensity = intensity;
		audio.musicVolume = 1;
		double[] samples = new double[(int) (seconds * Synth.RATE)];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = audio.nextSample();
		}
		return samples;
	}

	private double vary() {
		return 0.92 + this.random.nextDouble() * 0.16;
	}

	private void sfx(double[] samples, double gain, double rate) {
		this.incoming.add(new Voice(samples, gain, rate));
	}

	// -------------------------------------------------------------------- mixer

	private void run() {
		AudioFormat format = new AudioFormat(Synth.RATE, 16, 1, true, false);
		try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
			// Small buffer: the effects must sound with the images
			line.open(format, CHUNK * 2 * 6);
			line.start();
			byte[] pcm = new byte[CHUNK * 2];
			double fadeOut = 1;
			while (this.running || fadeOut > 0) {
				if (!this.running) {
					fadeOut = Math.max(0, fadeOut - 0.08);
				}
				for (int i = 0; i < CHUNK; i++) {
					int value = (int) (this.nextSample() * fadeOut * Short.MAX_VALUE);
					pcm[2 * i] = (byte) value;
					pcm[2 * i + 1] = (byte) (value >> 8);
				}
				line.write(pcm, 0, pcm.length);
			}
			line.drain();
		} catch (LineUnavailableException | IllegalArgumentException e) {
			// No sound card: the battle is still shown
			this.running = false;
		}
	}

	private double nextSample() {
		// Eases the controls, per sample, so nothing clicks
		Voice voice;
		while ((voice = this.incoming.poll()) != null) {
			this.sfxVoices.add(new Voice(voice.samples, voice.gain, voice.rate * (this.slowMotion ? 0.45 : 1)));
		}
		this.intensity += (this.targetIntensity - this.intensity) * 0.00003;
		this.slow += ((this.slowMotion ? 1 : 0) - this.slow) * 0.0002;
		this.musicVolume += ((this.musicOn ? 1 : 0) - this.musicVolume) * 0.00005;
		this.duck += (1 - this.duck) * 0.00004;

		double music = this.music() * this.musicVolume * this.duck * MUSIC_GAIN * this.musicLevel;

		double effects = 0;
		for (Iterator<Voice> it = this.sfxVoices.iterator(); it.hasNext();) {
			Voice playing = it.next();
			effects += playing.next();
			if (playing.finished()) {
				it.remove();
			}
		}
		// In slow motion the effects are muffled too
		this.sfxFilter += (effects - this.sfxFilter) * (1 - 0.8 * this.slow);
		effects = this.sfxFilter * SFX_GAIN;

		// A short echo gives the effects some space
		double delayed = this.echo[this.echoPosition];
		this.echo[this.echoPosition] = effects + delayed * 0.32;
		this.echoPosition = (this.echoPosition + 1) % this.echo.length;
		effects += delayed * 0.35;

		// Soft clipping: loud without distortion
		return Math.tanh((music + effects) * 1.1) * 0.92;
	}

	/**
	 * One sample of the soundtrack
	 */
	private double music() {
		double tension = this.intensity;
		Song current = this.song;
		double bpm = current.calmBpm + current.tenseBpm * tension;
		if (this.slow > 0.5) {
			bpm = 52;
		}
		this.beat += bpm / 60 / Synth.RATE;
		int step = (int) Math.floor(this.beat * 4);
		if (step != this.lastStep) {
			this.lastStep = step;
			this.onStep(step, tension);
		}

		// Pad: three detuned saws through a low pass that opens with the tension.
		// The drums have a drone instead: root, fifth and octave, darker.
		int bar = (int) Math.floor(this.beat / 4) % current.roots.length;
		this.padFrequency += (current.roots[bar] - this.padFrequency) * 0.0005;
		double third = current == Song.WAR_DRUMS ? 1 : current.minor[bar] ? 1.189 : 1.26;
		this.padPhase1 = (this.padPhase1 + this.padFrequency / Synth.RATE) % 1;
		this.padPhase2 = (this.padPhase2 + this.padFrequency * 1.004 * 1.498 / Synth.RATE) % 1;
		this.padPhase3 = (this.padPhase3 + this.padFrequency * 0.997 * third * 2 / Synth.RATE) % 1;
		double raw = (this.padPhase1 * 2 - 1) + (this.padPhase2 * 2 - 1) * 0.7 + (this.padPhase3 * 2 - 1) * 0.5;
		double cutoff = (current == Song.WAR_DRUMS ? 0.006 + 0.02 * tension : current == Song.SYNTHWAVE ? 0.02 + 0.05 * tension : 0.012 + 0.05 * tension) * (1 - 0.7 * this.slow);
		this.padFilter += (raw - this.padFilter) * cutoff;
		double pad = this.padFilter * (0.32 - 0.1 * tension);
		if (current == Song.SYNTHWAVE && tension > 0.2 && this.slow < 0.5) {
			// Pumps with the kick, the synthwave sound
			double inBeat = this.beat - Math.floor(this.beat);
			pad *= 1 - 0.65 * Math.exp(-inBeat * 7);
		}

		double voices = 0;
		for (Iterator<Voice> it = this.musicVoices.iterator(); it.hasNext();) {
			Voice playing = it.next();
			voices += playing.next();
			if (playing.finished()) {
				it.remove();
			}
		}
		return pad + voices;
	}

	/**
	 * The notes and drums of each 16th
	 */
	private void onStep(int step, double tension) {
		Song playing = this.song;
		int inBar = step % 16;
		int bar = (step / 16) % playing.roots.length;
		double root = playing.roots[bar];
		boolean minor = playing.minor[bar];

		if (this.slow > 0.5) {
			// Only a heartbeat
			if (inBar == 0 || inBar == 8) {
				this.musicVoices.add(new Voice(this.heart, 0.9, 1));
			}
			return;
		}
		switch (playing) {
		case EPIC -> this.epicStep(step, inBar, root, minor, tension);
		case SYNTHWAVE -> this.synthwaveStep(step, inBar, root, minor, tension);
		case WAR_DRUMS -> this.drumsStep(step, inBar, root, tension);
		}
	}

	private void epicStep(int step, int inBar, double root, boolean minor, double tension) {

		boolean fourOnFloor = tension > 0.6;
		if (inBar == 0 || inBar == 8 || (tension > 0.15 && inBar == 10) || (fourOnFloor && inBar % 4 == 0)) {
			if (tension > 0.12) {
				this.musicVoices.add(new Voice(this.kick, 0.85, 1));
			}
		}
		if (tension > 0.45 && (inBar == 4 || inBar == 12)) {
			this.musicVoices.add(new Voice(this.snare, 0.55, 1));
		}
		if (tension > 0.3 && (inBar % 2 == 0 || tension > 0.72)) {
			this.musicVoices.add(new Voice(this.hat, inBar % 4 == 2 ? 0.28 : 0.16, 1));
		}
		if (tension > 0.25 && inBar % 2 == 0) {
			double[] pattern = { 1, 1, 2, 1, 1.5, 1, 2, 1.5 };
			double frequency = root * pattern[(inBar / 2) % pattern.length];
			this.musicVoices.add(new Voice(Synth.lowpass(Synth.saw(frequency, 0.16, 6), 0.25), 0.5, 1));
		}
		if (tension > 0.65) {
			double[] chord = minor ? new double[] { 1, 1.189, 1.498, 2, 2.378, 2.997 } : new double[] { 1, 1.26, 1.498, 2, 2.52, 2.997 };
			double frequency = root * 4 * chord[(step * (tension > 0.85 ? 3 : 1)) % chord.length];
			this.musicVoices.add(new Voice(Synth.square(frequency, 0.09), 0.13, 1));
		}
		if (tension > 0.88 && (inBar == 0 || inBar == 8)) {
			this.musicVoices.add(new Voice(Synth.sweep(880, 1320, 0.35, false), 0.12, 1));
		}
	}

	/**
	 * Synthwave: a bass bouncing in octaves every 16th, four on the floor,
	 * backbeat, an arpeggiated lead and tom fills at the top
	 */
	private void synthwaveStep(int step, int inBar, double root, boolean minor, double tension) {
		// Bass: 8ths when calm, 16ths bouncing in octaves when the fight heats up
		if (inBar % 2 == 0 || tension > 0.3) {
			double frequency = root * (inBar % 2 == 1 ? 2 : 1);
			double[] bass = Synth.lowpass(Synth.mix(Synth.saw(frequency, 0.14, 9), Synth.saw(frequency * 1.006, 0.14, 9), 0.6), 0.12 + 0.2 * tension);
			this.musicVoices.add(new Voice(bass, 0.32 + 0.25 * Math.min(1, tension * 2), 1));
		}
		if (tension > 0.18 && inBar % 4 == 0) {
			this.musicVoices.add(new Voice(this.kick, 0.85, 1));
		}
		if (tension > 0.35 && (inBar == 4 || inBar == 12)) {
			// A long snare, like the gated reverb of the 80s
			this.musicVoices.add(new Voice(Synth.mix(this.snare, Synth.lowpass(Synth.noise(0.32, 2), 0.4), 0.5), 0.55, 1));
		}
		if (tension > 0.3 && (inBar % 4 == 2 || tension > 0.7)) {
			this.musicVoices.add(new Voice(this.hat, inBar % 4 == 2 ? 0.24 : 0.12, 1));
		}
		if (tension > 0.5 && inBar % 2 == 0) {
			double[] chord = minor ? new double[] { 1, 1.189, 1.498, 2, 1.498, 1.189, 2, 2.378 } : new double[] { 1, 1.26, 1.498, 2, 1.498, 1.26, 2, 2.52 };
			double frequency = root * 4 * chord[(inBar / 2) % chord.length] * (tension > 0.82 && inBar >= 8 ? 2 : 1);
			double[] lead = Synth.lowpass(Synth.mix(Synth.saw(frequency, 0.2, 7), Synth.saw(frequency * 1.007, 0.2, 7), 0.8), 0.35);
			this.musicVoices.add(new Voice(lead, 0.16, 1));
		}
		if (tension > 0.75 && (step / 16) % 2 == 1 && inBar >= 12) {
			// High, mid and low tom going down to the next bar
			this.musicVoices.add(new Voice(this.toms[Math.max(0, inBar - 13)], 0.5, 1));
		}
		if (tension > 0.9 && inBar == 0) {
			this.musicVoices.add(new Voice(Synth.sweep(root * 8, root * 12, 0.4, false), 0.1, 1));
		}
	}

	/**
	 * War drums: taikos even when calm, small drums and rims as it grows, a
	 * pentatonic flute and war horns at the top
	 */
	private void drumsStep(int step, int inBar, double root, double tension) {
		if (inBar == 0 || inBar == 6 || inBar == 10 || (tension > 0.55 && inBar == 14)) {
			this.musicVoices.add(new Voice(this.taiko, 0.45 + 0.5 * Math.min(1, tension * 1.6), inBar == 0 ? 1 : 1.12));
		}
		if (tension > 0.25 && (inBar == 3 || inBar == 7 || inBar == 11 || inBar == 13 || inBar == 15 || (tension > 0.65 && inBar % 2 == 1))) {
			this.musicVoices.add(new Voice(this.smallDrum, 0.35, 0.95 + 0.1 * ((inBar * 7) % 3)));
		}
		if (tension > 0.4 && (inBar == 4 || inBar == 12 || (tension > 0.8 && inBar % 4 == 2))) {
			this.musicVoices.add(new Voice(this.rim, 0.3, 1));
		}
		boolean phrase = tension > 0.8 ? inBar % 2 == 0 : inBar == 0 || inBar == 3 || inBar == 6 || inBar == 8 || inBar == 11 || inBar == 14;
		if (tension > 0.45 && phrase) {
			// A melody that walks the scale
			int degree = Math.floorMod((int) (step * 0.75) + (step / 16) * 2 + (int) Math.round(Math.sin(step * 0.9) * 2), PENTATONIC.length);
			double frequency = root * 4 * PENTATONIC[degree];
			double[] flute = Synth.mix(Synth.sine(frequency, 0.32, 4), Synth.lowpass(Synth.noise(0.32, 6), 0.5), 0.04);
			this.musicVoices.add(new Voice(flute, 0.2, 1));
		}
		if (tension > 0.82 && inBar == 0 && (step / 16) % 2 == 0) {
			double[] horn = Synth.lowpass(Synth.mix(Synth.saw(root * 2, 1.5, 1.1), Synth.saw(root * 3, 1.5, 1.1), 0.6), 0.06);
			this.musicVoices.add(new Voice(horn, 0.35, 1));
		}
		if (tension > 0.9) {
			// A roll under everything
			this.musicVoices.add(new Voice(this.smallDrum, 0.12, 1.3));
		}
	}

	// ---------------------------------------------------------------- synthesis

	private static double[] taiko() {
		return Synth.mix(Synth.sineSweep(105, 40, 0.75), Synth.lowpass(Synth.noise(0.12, 7), 0.15), 0.6);
	}

	private static double[] smallDrum() {
		return Synth.mix(Synth.sineSweep(260, 150, 0.16), Synth.lowpass(Synth.noise(0.08, 9), 0.4), 0.4);
	}

	private static double[] rim() {
		return Synth.mix(Synth.sine(1650, 0.05, 45), hat(), 0.5);
	}

	private static double[] kick() {
		return Synth.mix(Synth.sineSweep(150, 42, 0.32), Synth.lowpass(Synth.noise(0.02, 8), 0.5), 0.15);
	}

	private static double[] snare() {
		return Synth.mix(Synth.lowpass(Synth.noise(0.2, 3.5), 0.6), Synth.sine(190, 0.12, 20), 0.35);
	}

	private static double[] hat() {
		double[] noise = Synth.noise(0.05, 6);
		// High pass: noise minus its low pass
		double[] low = Synth.lowpass(noise.clone(), 0.3);
		for (int i = 0; i < noise.length; i++) {
			noise[i] -= low[i];
		}
		return noise;
	}

	private static double[] heartbeat() {
		return Synth.concat(Synth.sineSweep(70, 40, 0.16), Synth.silence(0.1), Synth.sineSweep(60, 35, 0.2));
	}
}

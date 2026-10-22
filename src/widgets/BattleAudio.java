package widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

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
	static final double MACHINE_GAIN = 0.8;
	/** The whole mix, before the limiter: the loud peaks of the music fit under its knee */
	private static final double MASTER_GAIN = 0.6;
	/** The drive of the tracks, over the machines: how loud the hum and the rumble are */
	static final double TRACK_GAIN = 2.5;
	/** The placement of a sound in the middle: both ears hear it the same */
	static final double[] CENTER = { 1, 1 };
	/** The distance between two links of a track, in arena units */
	static final double TREAD_PITCH = 3.2;

	/** Pentatonic minor, for the kalimba */
	static final double[] PENTATONIC = { 1, 1.189, 1.335, 1.498, 1.782, 2, 2.378, 2.67 };
	/** The motif of the kalimba, as steps of the pentatonic, one per eighth note of a bar */
	static final int[] MOTIF = { 0, 2, 4, 2, 1, 0, 2, 4 };

	private final ConcurrentLinkedQueue<Voice> incoming = new ConcurrentLinkedQueue<Voice>();
	final Random random = new Random();
	private Thread thread;
	private volatile boolean running;

	// Controls, written by the JavaFX thread
	private volatile double targetIntensity = 0;
	private volatile boolean slowMotion = false;
	private volatile boolean musicOn = true;
	volatile Song song = Song.EPIC;
	/** The volume of the music, from 0 to 1. The effects are not affected. */
	private volatile double musicLevel = 1;
	/** Seconds to reach silence, 0 while it is not fading out */
	private volatile double fadeSeconds = 0;
	/** Where the listener is on the floor and its right hand: the camera of the arena */
	private volatile double listenerX;
	private volatile double listenerZ;
	private volatile double rightX = 1;
	private volatile double rightZ;
	/** The machines of each robot. The list is read by the audio thread, the map is only used by the JavaFX thread */
	final List<Machine> robotMachines = new CopyOnWriteArrayList<Machine>();
	private final Map<Integer, Machine> machinesById = new HashMap<Integer, Machine>();

	// Mixer state, only the audio thread touches it
	private final List<Voice> sfxVoices = new ArrayList<Voice>();
	final List<Voice> musicVoices = new ArrayList<Voice>();
	double intensity = 0;
	private double musicVolume = 0;
	double slow = 0;
	double duck = 1;
	/** Where the sounds of the shots and the hits go, for the JavaFX thread: the ears' gains */
	private double[] placement = CENTER;
	private final double[] effects = new double[2];
	double beat = 0;
	int lastStep = -1;
	double padPhase1;
	double padPhase2;
	double padPhase3;
	double padFrequency = Song.EPIC.roots[0];
	double padFilter;
	private double sfxFilterLeft;
	private double sfxFilterRight;
	private final double[] echo = new double[(int) (Synth.RATE * 0.13)];
	private final double[] echoRight = new double[(int) (Synth.RATE * 0.13)];
	private int echoPosition;

	// Ready made sounds
	final double[] kick = Percussion.kick();
	final double[] snare = Percussion.snare();
	final double[] hat = Percussion.hat();
	final double[] heart = Percussion.heartbeat();
	final double[] taiko = Percussion.taiko();
	final double[] smallDrum = Percussion.smallDrum();
	final double[] woodBlock = Percussion.woodBlock();
	final double[][] toms = { Synth.sineSweep(230, 115, 0.28), Synth.sineSweep(175, 88, 0.3), Synth.sineSweep(130, 62, 0.34) };
	private final BattleAudioScore score = new BattleAudioScore(this);
	private final BattleAudioEffects oneShots = new BattleAudioEffects(this);

	// ------------------------------------------------------------------- control

	/** Starts the mixer thread, once. */
	public synchronized void start() {
		if (this.running) {
			return;
		}
		this.running = true;
		this.thread = new Thread(this::run, "battle-audio");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	/** Stops the mixer thread. */
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

	/** Sets the music on. */
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

	/**
	 * Where the listener is and where it faces, on the floor: the camera. The sounds
	 * come from the side of the screen where they happen, in the headphones.
	 */
	public void setListener(double posX, double posZ, double facingX, double facingZ) {
		double length = Math.hypot(facingX, facingZ);
		this.listenerX = posX;
		this.listenerZ = posZ;
		if (length > 1e-9) {
			// The right hand of the facing, on the floor, as the camera has it
			this.rightX = facingZ / length;
			this.rightZ = -facingX / length;
		}
	}

	/**
	 * A robot's machines, called every frame: how fast its tracks run (arena units a
	 * second), how fast its turret turns (degrees a second) and where it is on the
	 * floor. Each robot has its own sound, from where it is.
	 */
	public void setMachine(int robot, double treadSpeed, double turretSpeed, double posX, double posZ) {
		Machine machine = this.machinesById.get(robot);
		if (machine == null) {
			machine = new Machine();
			this.machinesById.put(robot, machine);
			this.robotMachines.add(machine);
		}
		machine.treadSpeed = treadSpeed;
		machine.turretSpeed = turretSpeed;
		double[] gains = this.spatial(posX, posZ);
		machine.leftGain = gains[0];
		machine.rightGain = gains[1];
	}

	/**
	 * The gains of the left and the right ear for a sound at x, z: panned by where it
	 * is from the listener, quieter with the distance. Centered, both are 1.
	 */
	double[] spatial(double posX, double posZ) {
		double deltaX = posX - this.listenerX;
		double deltaZ = posZ - this.listenerZ;
		double distance = Math.hypot(deltaX, deltaZ);
		double pan = distance < 1 ? 0 : (deltaX * this.rightX + deltaZ * this.rightZ) / distance;
		double angle = (pan + 1) * Math.PI / 4;
		double loudness = 1 / (1 + distance / 600);
		return new double[] { Math.sqrt(2) * Math.cos(angle) * loudness, Math.sqrt(2) * Math.sin(angle) * loudness };
	}

	/** Runs a sound placed at x, z: the shot, the hit or the rest, heard from where it is */
	private void placedAt(double posX, double posZ, Runnable sound) {
		this.placement = this.spatial(posX, posZ);
		try {
			sound.run();
		} finally {
			this.placement = CENTER;
		}
	}

	/** Plays a shot at the given position of the arena. */
	public void shotAt(double power, double posX, double posZ) {
		this.placedAt(posX, posZ, () -> this.shot(power));
	}

	/** Plays a hit at the given position of the arena. */
	public void hitAt(double power, double posX, double posZ) {
		this.placedAt(posX, posZ, () -> this.hit(power));
	}

	/** Plays a spark at the given position of the arena. */
	public void sparkAt(double posX, double posZ) {
		this.placedAt(posX, posZ, this::spark);
	}

	/** Plays a thud at the given position of the arena. */
	public void thudAt(double posX, double posZ) {
		this.placedAt(posX, posZ, this::thud);
	}

	/** Plays the low energy warning at the given position of the arena. */
	public void lowEnergyAt(double posX, double posZ) {
		this.placedAt(posX, posZ, this::lowEnergy);
	}

	/** Plays the destruction of a robot at the given position of the arena. */
	public void destroyedAt(double posX, double posZ) {
		this.placedAt(posX, posZ, this::destroyed);
	}

	/** Sets the song. */
	public void setSong(Song song) {
		this.song = song;
	}

	/** Returns the song. */
	public Song getSong() {
		return this.song;
	}

	// ------------------------------------------------------------------ effects

	/** Lowers the whole sound of the shots: the base frequency */
	private static final double SHOT_BASE = 0.65;

	/**
	 * How high a shot sounds: robocode bullets fly at 20 - 3 * power, so the
	 * pitch goes with their speed. A quick bullet (little power, 19.7) is high, a
	 * slow one (the most power, 11) is low, about an octave and a half apart.
	 *
	 * @param power
	 *            0.1 to 3
	 */
	static double shotPitch(double power) {
		double speed = 20 - 3 * Math.max(0.1, Math.min(3, power));
		return SHOT_BASE * Math.pow(speed / 15, 1.8);
	}

	/**
	 * The mix of some seconds of music at a fixed intensity, without a sound
	 * card: for tests and previews
	 */
	static double[] render(Song song, double intensity, double seconds) {
		return render(song, intensity, seconds, 1);
	}

	/**
	 * The sound of the machines alone, for the tests: the tracks and the turrets at
	 * the speeds given, without music
	 */
	static double[] machines(double treadSpeed, double turretSpeed, double seconds) {
		BattleAudio audio = new BattleAudio();
		audio.setMachine(0, treadSpeed, turretSpeed, 0, 0);
		audio.musicLevel = 0;
		double[] samples = new double[(int) (seconds * Synth.RATE)];
		double[] frame = new double[2];
		for (int index = 0; index < samples.length; index++) {
			audio.nextFrame(frame);
			samples[index] = (frame[0] + frame[1]) / 2;
		}
		return samples;
	}

	/** Renders the music and the effects of the battle into samples, with the given mix, without playing them. */
	static double[] render(Song song, double intensity, double seconds, double musicLevel) {
		BattleAudio audio = new BattleAudio();
		audio.song = song;
		audio.musicLevel = musicLevel;
		audio.intensity = intensity;
		audio.targetIntensity = intensity;
		audio.musicVolume = 1;
		double[] samples = new double[(int) (seconds * Synth.RATE)];
		double[] frame = new double[2];
		for (int index = 0; index < samples.length; index++) {
			audio.nextFrame(frame);
			samples[index] = (frame[0] + frame[1]) / 2;
		}
		return samples;
	}

	/**
	 * Everything fades to silence, slowly (a stop is quick)
	 */
	public void fadeOut(double seconds) {
		this.fadeSeconds = Math.max(0.05, seconds);
	}

	public void shot(double power) {
		this.oneShots.shot(power);
	}

	public void hit(double power) {
		this.oneShots.hit(power);
	}

	public void spark() {
		this.oneShots.spark();
	}

	public void destroyed() {
		this.oneShots.destroyed();
	}

	public void thud() {
		this.oneShots.thud();
	}

	public void lowEnergy() {
		this.oneShots.lowEnergy();
	}

	public void roundCall() {
		this.oneShots.roundCall();
	}

	public void countdown(boolean last) {
		this.oneShots.countdown(last);
	}

	public void fight() {
		this.oneShots.fight();
	}

	public void knockOut() {
		this.oneShots.knockOut();
	}

	public void whoosh() {
		this.oneShots.whoosh();
	}

	/** A random factor close to one, so repeated sounds do not play exactly the same. */
	double vary() {
		return 0.92 + this.random.nextDouble() * 0.16;
	}

	/** Queues a sound at the position where the next effect is placed. */
	void sfx(double[] samples, double gain, double rate) {
		this.incoming.add(new Voice(samples, gain, rate, this.placement[0], this.placement[1]));
	}

	// -------------------------------------------------------------------- mixer

	/** The mixer loop: fills the audio line with the music and the sounds in progress. */
	private void run() {
		AudioFormat format = new AudioFormat(Synth.RATE, 16, 2, true, false);
		try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
			// Small buffer: the effects must sound with the images
			line.open(format, CHUNK * 4 * 6);
			line.start();
			byte[] pcm = new byte[CHUNK * 4];
			double[] frame = new double[2];
			double fadeOut = 1;
			double faded = 1;
			while (this.running || fadeOut > 0) {
				if (!this.running) {
					fadeOut = Math.max(0, fadeOut - 0.08);
				}
				// A slow fade out asked by the screen: everything gets quieter
				if (this.fadeSeconds > 0) {
					faded = Math.max(0, faded - CHUNK / (Synth.RATE * this.fadeSeconds));
				}
				for (int index = 0; index < CHUNK; index++) {
					this.nextFrame(frame);
					int left = (int) (frame[0] * fadeOut * faded * Short.MAX_VALUE);
					int right = (int) (frame[1] * fadeOut * faded * Short.MAX_VALUE);
					pcm[4 * index] = (byte) left;
					pcm[4 * index + 1] = (byte) (left >> 8);
					pcm[4 * index + 2] = (byte) right;
					pcm[4 * index + 3] = (byte) (right >> 8);
				}
				line.write(pcm, 0, pcm.length);
			}
			line.drain();
		} catch (LineUnavailableException | IllegalArgumentException illegalArgumentException) {
			// No sound card: the battle is still shown
			this.running = false;
		}
	}

	/**
	 * The next moment of the mix, for the left and the right ear
	 */
	void nextFrame(double[] out) {
		this.easeControls();

		double music = this.score.music() * this.musicVolume * this.duck * MUSIC_GAIN * this.musicLevel;

		this.mixEffects();

		// In slow motion the effects are muffled too
		this.sfxFilterLeft += (this.effects[0] - this.sfxFilterLeft) * (1 - 0.8 * this.slow);
		this.sfxFilterRight += (this.effects[1] - this.sfxFilterRight) * (1 - 0.8 * this.slow);
		double left = this.sfxFilterLeft * SFX_GAIN;
		double right = this.sfxFilterRight * SFX_GAIN;

		// A short echo gives the effects some space, each ear with its own, so the echo keeps the side
		double delayedLeft = this.echo[this.echoPosition];
		double delayedRight = this.echoRight[this.echoPosition];
		this.echo[this.echoPosition] = left + delayedLeft * 0.32;
		this.echoRight[this.echoPosition] = right + delayedRight * 0.32;
		this.echoPosition = (this.echoPosition + 1) % this.echo.length;
		left += delayedLeft * 0.35;
		right += delayedRight * 0.35;

		// The master gain keeps the whole mix under the tanh curve, which only bends the loudest peaks
		out[0] = Math.tanh((music + left) * MASTER_GAIN * 1.1) * 0.92;
		out[1] = Math.tanh((music + right) * MASTER_GAIN * 1.1) * 0.92;
	}

	/** Eases the controls a little toward their targets, per sample, so nothing clicks, and takes the new voices in */
	private void easeControls() {
		Voice voice;
		while ((voice = this.incoming.poll()) != null) {
			this.sfxVoices.add(new Voice(voice.samples, voice.gain, voice.rate * (this.slowMotion ? 0.45 : 1), voice.left, voice.right));
		}
		this.intensity += (this.targetIntensity - this.intensity) * 0.00003;
		this.slow += ((this.slowMotion ? 1 : 0) - this.slow) * 0.0002;
		this.musicVolume += ((this.musicOn ? 1 : 0) - this.musicVolume) * 0.00005;
		this.duck += (1 - this.duck) * 0.00004;
	}

	/** Adds up the sound effects that are playing into the effects bus, each one leaving the list when it is done */
	private void mixEffects() {
		this.effects[0] = 0;
		this.effects[1] = 0;
		for (Iterator<Voice> iterator2 = this.sfxVoices.iterator(); iterator2.hasNext();) {
			Voice playing = iterator2.next();
			double value = playing.next();
			this.effects[0] += value * playing.left;
			this.effects[1] += value * playing.right;
			if (playing.finished()) {
				iterator2.remove();
			}
		}
		this.oneShots.mixMachines(this.effects);
	}

	// ---------------------------------------------------------------- synthesis
}

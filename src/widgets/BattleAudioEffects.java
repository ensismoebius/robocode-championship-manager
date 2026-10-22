package widgets;

import static widgets.BattleAudio.MACHINE_GAIN;
import static widgets.BattleAudio.TRACK_GAIN;
import static widgets.BattleAudio.TREAD_PITCH;

import static widgets.BattleAudio.shotPitch;


/**
 * The sounds of the battle that happen once: the shots, the hits, the countdown and the fanfares
 */
final class BattleAudioEffects {

	private final BattleAudio host;

	BattleAudioEffects(BattleAudio host) {
		this.host = host;
	}

	/**
	 * A cannon shot, heavier with more power (0.1 to 3). It is higher the faster
	 * the bullet goes (see {@link #shotPitch(double)}) and lower the slower: the
	 * same sound played faster or slower.
	 */
	public void shot(double power) {
		double weight = Math.max(0, Math.min(1, (power - 0.1) / 2.9));
		double[] crack = Synth.mix(Synth.lowpass(Synth.noise(0.09, 3), 0.45), Synth.sweep(1500, 200, 0.12, true), 0.7);
		double[] thump = Synth.sineSweep(100, 45, 0.22);
		// The bullet whistling out of the barrel
		double[] whistle = Synth.sineSweep(900, 500, 0.14);
		double[] sound = Synth.mix(Synth.mix(crack, thump, 0.45 + 0.4 * weight), whistle, 0.25);
		host.sfx(sound, 0.45 + 0.4 * weight, shotPitch(power) * host.vary());
	}

	/**
	 * A bullet hitting a robot: crunch, metallic ring and a low boom
	 */
	public void hit(double power) {
		double weight = Math.max(0, Math.min(1, power / 3));
		double[] crunch = Synth.lowpass(Synth.noise(0.35, 2.2), 0.35);
		double[] ring = Synth.mix(Synth.sine(930, 0.4, 7), Synth.sine(1395, 0.4, 9), 0.5);
		double[] boom = Synth.sineSweep(90, 35, 0.5);
		host.sfx(Synth.mix(Synth.mix(crunch, ring, 0.25), boom, 0.6), 0.7 + 0.3 * weight, host.vary());
		host.duck = Math.min(host.duck, 0.75);
	}

	/**
	 * Bullets hitting each other or a wall
	 */
	public void spark() {
		host.sfx(Synth.mix(Synth.lowpass(Synth.noise(0.12, 4), 0.6), Synth.sine(2200, 0.12, 25), 0.3), 0.35, host.vary());
	}

	/**
	 * A robot blowing up: a long and deep explosion with a second blast
	 */
	public void destroyed() {
		double[] blast = Synth.mix(Synth.lowpass(Synth.noise(2.6, 1.1), 0.12), Synth.sineSweep(75, 22, 2.2), 0.65);
		double[] crackle = Synth.crackle(1.8);
		double[] second = Synth.concat(Synth.silence(0.35), Synth.mix(Synth.lowpass(Synth.noise(1.4, 1.6), 0.2), Synth.sineSweep(60, 25, 1.2), 0.6));
		host.sfx(Synth.mix(Synth.mix(blast, crackle, 0.25), second, 0.45), 1.0, 1);
		host.duck = 0.25;
	}

	/** A robot hitting a wall or another robot */
	public void thud() {
		host.sfx(Synth.sineSweep(70, 40, 0.18), 0.5, host.vary());
	}

	/** Alarm when a robot is about to die */
	public void lowEnergy() {
		host.sfx(Synth.concat(Synth.square(1320, 0.08), Synth.silence(0.06), Synth.square(1320, 0.08)), 0.22, 1);
	}

	/** "ROUND N": an impact */
	public void roundCall() {
		host.sfx(Synth.mix(Synth.sineSweep(120, 40, 0.9), Synth.lowpass(Synth.noise(0.9, 2.5), 0.15), 0.6), 0.8, 1);
	}

	/** One number of the countdown: a beep, higher for the last */
	public void countdown(boolean last) {
		host.sfx(Synth.mix(Synth.sine(last ? 880 : 660, 0.25, 6), Synth.square(last ? 880 : 660, 0.2), 0.15), 0.7, 1);
	}

	/** "FIGHT!": a brass like stab */
	public void fight() {
		double[] stab = new double[(int) (Synth.RATE * 0.9)];
		for (double frequency : new double[] { 220, 261.63, 329.63, 440, 523.25 }) {
			stab = Synth.mix(stab, Synth.saw(frequency, 0.9, 2.5), 0.85);
		}
		host.sfx(Synth.mix(stab, Synth.sineSweep(100, 40, 0.6), 0.5), 0.75, 1);
		host.duck = 0.5;
	}

	/** "K.O.!": a big hit and a falling tone */
	public void knockOut() {
		host.sfx(Synth.mix(Synth.mix(Synth.sineSweep(80, 20, 1.6), Synth.lowpass(Synth.noise(1.5, 1.4), 0.15), 0.6), Synth.saw(110, 1.5, 1.2), 0.3), 1.0, 1);
		host.duck = 0.2;
	}

	/** Entering the replay: a reversed rising whoosh */
	public void whoosh() {
		double[] rise = Synth.lowpass(Synth.noise(0.9, 0), 0.25);
		for (int index = 0; index < rise.length; index++) {
			double time = (double) index / rise.length;
			rise[index] *= time * time;
		}
		host.sfx(Synth.mix(rise, Synth.sineSweep(200, 900, 0.9), 0.3), 0.6, 1);
	}

	/**
	 * The machines of each robot, from where it is, all of them low, the inside of the
	 * tank: the drive of the tracks hums with its harmonics, its pitch rising a little
	 * with the speed, the gears swell once per turn under it and the engine rumbles
	 * below. The servo of the turret whines as it turns, higher the faster it goes.
	 */
	void mixMachines(double[] effects) {
		for (Machine machine : host.robotMachines) {
			machine.treadLevel += (machine.treadSpeed - machine.treadLevel) * 0.0005;
			machine.turretLevel += (machine.turretSpeed - machine.turretLevel) * 0.0005;
			double tread = Math.min(1, machine.treadLevel / 40);
			double turret = Math.min(1, machine.turretLevel / 90);
			// The placement eases to where the robot is now, so it does not click
			machine.left += (machine.leftGain - machine.left) * 0.001;
			machine.right += (machine.rightGain - machine.right) * 0.001;

			// The drive: a low hum and its harmonics
			machine.humPhase = (machine.humPhase + (38 + 0.2 * machine.treadLevel) / Synth.RATE) % 1;
			double hum = Math.sin(2 * Math.PI * machine.humPhase) + 0.45 * Math.sin(4 * Math.PI * machine.humPhase) + 0.2 * Math.sin(6 * Math.PI * machine.humPhase);
			// The gears: a slow swell, once for each stretch of belt that passes a tooth
			machine.gearPhase = (machine.gearPhase + machine.treadLevel / TREAD_PITCH / 8 / Synth.RATE) % 1;
			double swell = 0.8 + 0.2 * Math.sin(2 * Math.PI * machine.gearPhase);
			// The engine below everything: noise that is only low
			machine.rumble += ((host.random.nextDouble() * 2 - 1) - machine.rumble) * 0.01;
			double sound = (hum * 0.3 * swell + machine.rumble * 0.9) * tread * TRACK_GAIN;

			machine.servoPhase = (machine.servoPhase + (330 + 4 * machine.turretLevel) / Synth.RATE) % 1;
			double whine = Math.sin(2 * Math.PI * machine.servoPhase) + 0.4 * Math.sin(4 * Math.PI * machine.servoPhase);
			sound += (whine * 0.09 + (host.random.nextDouble() * 2 - 1) * 0.03) * turret;
			sound *= MACHINE_GAIN;
			effects[0] += sound * machine.left;
			effects[1] += sound * machine.right;
		}
	}
}

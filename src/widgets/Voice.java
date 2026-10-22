package widgets;

/**
 * A sound being played, at a rate (pitch) and gain
 */
final class Voice {
	final double[] samples;
	final double gain;
	final double rate;
	double position;

	final double left;
	final double right;

	/** A sound at the centre of the stereo field. */
	Voice(double[] samples, double gain, double rate) {
		this(samples, gain, rate, BattleAudio.CENTER[0], BattleAudio.CENTER[1]);
	}

	/** A sound that plays from the given stereo position. */
	Voice(double[] samples, double gain, double rate, double left, double right) {
		this.samples = samples;
		this.gain = gain;
		this.rate = rate;
		this.left = left;
		this.right = right;
	}

	/** Tells whether the sound has played to its end. */
	boolean finished() {
		return this.position >= this.samples.length - 1;
	}

	/** The next sample of the sound, interpolated between the two closest ones. */
	double next() {
		int index = (int) this.position;
		double fraction = this.position - index;
		double value = this.samples[index] + (this.samples[Math.min(index + 1, this.samples.length - 1)] - this.samples[index]) * fraction;
		this.position += this.rate;
		return value * this.gain;
	}
}

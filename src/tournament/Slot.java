package tournament;

/**
 * One of the two places of a match
 */
public final class Slot {

	/** Where the participant comes from, null for the first round */
	final Match source;
	final boolean sourceWinner;

	boolean resolved;
	Long participant;

	/** A slot filled by a seed of the championship. */
	Slot(Long seed) {
		this.source = null;
		this.sourceWinner = false;
		this.participant = seed;
		this.resolved = true;
	}

	/** A slot that takes the winner or the loser of another match. */
	Slot(Match source, boolean sourceWinner) {
		this.source = source;
		this.sourceWinner = sourceWinner;
	}

	/**
	 * @return the participant, or null if not known yet or if it is a bye
	 */
	public Long getParticipant() {
		return participant;
	}

	/**
	 * @return true when it is known who (or nobody) is in this slot
	 */
	public boolean isResolved() {
		return resolved;
	}

	/** Returns the source. */
	public Match getSource() {
		return source;
	}

	/** Tells whether the slot takes the winner of its source match, not the loser. */
	public boolean isSourceWinner() {
		return sourceWinner;
	}

	/**
	 * Describes where the participant will come from, e.g. "Vencedor de V1-2"
	 */
	public String describeSource() {
		if (this.source == null) {
			return "Folga";
		}
		return (this.sourceWinner ? "Vencedor de " : "Perdedor de ") + this.source.getCode();
	}

	/** Fills the slot with the winner or the loser of its source match, once that match is done. */
	void resolve() {
		if (!this.resolved && this.source != null && this.source.status.isDone()) {
			this.participant = this.sourceWinner ? this.source.winner : this.source.loser;
			this.resolved = true;
		}
	}
}

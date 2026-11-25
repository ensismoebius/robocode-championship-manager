package tournament;


/** A match of the bracket: its two slots, its round and its place in the bracket. */
public final class Match {

	final String code;
	final Side side;
	final int round;
	final int position;
	final Slot a;
	final Slot b;
	final int depth;

	/** Whether it is the challenge of the one left without a pair in the round, against the loser of its first match */
	boolean challenge;
	Status status = Status.WAITING;
	Long winner;
	Long loser;
	Integer winnerScore;
	Integer loserScore;

	/** A match between two slots, in the given round and position of its side. */
	Match(String code, Side side, int round, int position, Slot slot, Slot slot2) {
		this.code = code;
		this.side = side;
		this.round = round;
		this.position = position;
		this.a = slot;
		this.b = slot2;
		this.depth = 1 + Math.max(depthOf(slot), depthOf(slot2));
	}

	/** How many matches come before a slot, following its sources. */
	static int depthOf(Slot slot) {
		return slot.source == null ? 0 : slot.source.depth;
	}

	/**
	 * Unique code: V (main bracket) or R (repechage) followed by round-position,
	 * or F1/F2 for the finals
	 */
	public String getCode() {
		return code;
	}

	/** Returns the side. */
	public Side getSide() {
		return side;
	}

	/**
	 * 1 based round inside its side
	 */
	public int getRound() {
		return round;
	}

	/**
	 * 1 based position inside its round
	 */
	public int getPosition() {
		return position;
	}

	/** Returns the a. */
	public Slot getA() {
		return a;
	}

	/** Returns the b. */
	public Slot getB() {
		return b;
	}

	/**
	 * How many matches must be played before this one, used for the playing order
	 */
	public int getDepth() {
		return depth;
	}

	/** Tells whether the match is a challenge: the robot left without a pair in the round plays the loser of its first match. */
	public boolean isChallenge() {
		return challenge;
	}

	/** Returns the status. */
	public Status getStatus() {
		return status;
	}

	/** Returns the winner. */
	public Long getWinner() {
		return winner;
	}

	/** Returns the loser. */
	public Long getLoser() {
		return loser;
	}

	/** Returns the winner score. */
	public Integer getWinnerScore() {
		return winnerScore;
	}

	/** Returns the loser score. */
	public Integer getLoserScore() {
		return loserScore;
	}

	/** Tells whether a participant takes part in this match. */
	public boolean involves(Long participant) {
		return participant != null && (participant.equals(this.a.participant) || participant.equals(this.b.participant));
	}

	/** Fills the slots when their sources are done, and makes the match ready when both are known. */
	void update() {
		if (this.status != Status.WAITING) {
			return;
		}
		this.a.resolve();
		this.b.resolve();
		if (!this.a.resolved || !this.b.resolved) {
			return;
		}

		this.status = Status.READY;
	}

	/** The code of the match with its participants and status. */
	@Override
	public String toString() {
		return this.code + "[" + this.a.participant + " x " + this.b.participant + " " + this.status + "]";
	}
}

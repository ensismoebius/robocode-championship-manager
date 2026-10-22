package tournament;


/** The state of a match in the bracket. */
public enum Status {
	/** Waiting for the result of previous matches */
	WAITING,
	/** Both participants known, can be played */
	READY,
	/** Played */
	FINISHED,
	/** Decided without playing: one or both participants are missing */
	BYE;

	/** Tells whether the state is final: the match is finished or it is a bye. */
	public boolean isDone() {
		return this == FINISHED || this == BYE;
	}
}

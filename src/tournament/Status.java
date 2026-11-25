package tournament;


/** The state of a match in the bracket. */
public enum Status {
	/** Waiting for the result of previous matches */
	WAITING,
	/** Both participants known, can be played */
	READY,
	/** Played */
	FINISHED;

	/** Tells whether the state is final: the match is finished. */
	public boolean isDone() {
		return this == FINISHED;
	}
}

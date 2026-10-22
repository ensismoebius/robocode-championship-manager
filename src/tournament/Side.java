package tournament;


/** The bracket a match belongs to: the winners, the repechage (losers) or the grand final. */
public enum Side {
	/** The main bracket */
	WINNERS,
	/** The repechage bracket */
	LOSERS,
	/** The grand final */
	FINAL
}

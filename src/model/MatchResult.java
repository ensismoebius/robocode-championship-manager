package model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The result of one match of a {@link Championship}
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class MatchResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "championshipId")
	private Championship championship;

	/**
	 * The match code in the bracket, like V1-3, R2-1 or F1
	 */
	@Column(nullable = false)
	private String matchCode;

	@ManyToOne(optional = false)
	@JoinColumn(name = "winnerId")
	private Opponent winner;

	@ManyToOne(optional = false)
	@JoinColumn(name = "loserId")
	private Opponent loser;

	/**
	 * Robocode scores, null when the result was informed by hand
	 */
	private Integer winnerScore;

	private Integer loserScore;

	@Column(nullable = false)
	private LocalDateTime playedAt;

	/** Returns the id. */
	public long getId() {
		return id;
	}

	/** Returns the championship. */
	public Championship getChampionship() {
		return championship;
	}

	/** Sets the championship. */
	public void setChampionship(Championship championship) {
		this.championship = championship;
	}

	/** Returns the match code. */
	public String getMatchCode() {
		return matchCode;
	}

	/** Sets the match code. */
	public void setMatchCode(String matchCode) {
		this.matchCode = matchCode;
	}

	/** Returns the winner. */
	public Opponent getWinner() {
		return winner;
	}

	/** Sets the winner. */
	public void setWinner(Opponent winner) {
		this.winner = winner;
	}

	/** Returns the loser. */
	public Opponent getLoser() {
		return loser;
	}

	/** Sets the loser. */
	public void setLoser(Opponent loser) {
		this.loser = loser;
	}

	/** Returns the winner score. */
	public Integer getWinnerScore() {
		return winnerScore;
	}

	/** Sets the winner score. */
	public void setWinnerScore(Integer winnerScore) {
		this.winnerScore = winnerScore;
	}

	/** Returns the loser score. */
	public Integer getLoserScore() {
		return loserScore;
	}

	/** Sets the loser score. */
	public void setLoserScore(Integer loserScore) {
		this.loserScore = loserScore;
	}

	/** Returns the played at. */
	public LocalDateTime getPlayedAt() {
		return playedAt;
	}

	/** Sets the played at. */
	public void setPlayedAt(LocalDateTime playedAt) {
		this.playedAt = playedAt;
	}
}

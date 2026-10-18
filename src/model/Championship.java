package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A championship: the drawn order of the robots and the results played so far.
 * The bracket itself is rebuilt from them, see {@link tournament.Bracket}
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class Championship {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	/**
	 * Double elimination: losers get a second chance in the repechage bracket
	 */
	@Column(nullable = false)
	private boolean repechage;

	/**
	 * If the repechage champion wins the grand final a second final is played,
	 * so both finalists have lost twice
	 */
	@Column(nullable = false)
	private boolean finalReset;

	/**
	 * Robots in seed order, the first ones get the byes
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ChampionshipSeed", joinColumns = @JoinColumn(name = "championshipId"), inverseJoinColumns = @JoinColumn(name = "opponentId"))
	@OrderColumn(name = "seed")
	private List<Opponent> seeds = new ArrayList<Opponent>();

	@OneToMany(mappedBy = "championship", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("id")
	private List<MatchResult> results = new ArrayList<MatchResult>();

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public boolean isRepechage() {
		return repechage;
	}

	public void setRepechage(boolean repechage) {
		this.repechage = repechage;
	}

	public boolean isFinalReset() {
		return finalReset;
	}

	public void setFinalReset(boolean finalReset) {
		this.finalReset = finalReset;
	}

	public List<Opponent> getSeeds() {
		return seeds;
	}

	public List<MatchResult> getResults() {
		return results;
	}
}

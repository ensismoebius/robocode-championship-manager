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
	 * Not used anymore (the rematch of the final was removed). The column stays,
	 * always false, because databases created before have it as not null.
	 */
	@Column(nullable = false)
	private boolean finalReset;

	/**
	 * How many times each presenter was on the screen in the battles of the championship, so that all of them are on it the same
	 * number of times (see widgets.scenes.PresenterRoster). Null before the first battle.
	 */
	@Column(length = 500)
	private String presenterAppearances;

	/**
	 * Robots in seed order: the order of the draw, which spreads the first ones so they only meet at the end
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "ChampionshipSeed", joinColumns = @JoinColumn(name = "championshipId"), inverseJoinColumns = @JoinColumn(name = "opponentId"))
	@OrderColumn(name = "seed")
	private List<Opponent> seeds = new ArrayList<Opponent>();

	@OneToMany(mappedBy = "championship", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("id")
	private List<MatchResult> results = new ArrayList<MatchResult>();

	/** Returns the id. */
	public long getId() {
		return id;
	}

	/** Returns the name. */
	public String getName() {
		return name;
	}

	/** Sets the name. */
	public void setName(String name) {
		this.name = name;
	}

	/** Returns the created at. */
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	/** Sets the created at. */
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	/** Tells whether the championship has a repechage, a second chance for the losers. */
	public boolean isRepechage() {
		return repechage;
	}

	/** Sets the repechage. */
	public void setRepechage(boolean repechage) {
		this.repechage = repechage;
	}

	/** Returns the seeds. */
	public List<Opponent> getSeeds() {
		return seeds;
	}

	/** How many times each presenter was on the screen, as text; null before the first battle. */
	public String getPresenterAppearances() {
		return presenterAppearances;
	}

	/** Sets how many times each presenter was on the screen, as text. */
	public void setPresenterAppearances(String presenterAppearances) {
		this.presenterAppearances = presenterAppearances;
	}

	/** Returns the results. */
	public List<MatchResult> getResults() {
		return results;
	}
}

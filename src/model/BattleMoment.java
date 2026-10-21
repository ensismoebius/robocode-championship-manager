package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * A photo of a key moment of a 3D battle: a hit that hurts or a robot destroyed.
 * The photos are kept with their match, so the ceremony of the champion can show
 * the battles of each robot on the podium. The robots are kept by id, inside
 * their championship: the ids mean nothing outside it.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class BattleMoment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private long championshipId;

	/**
	 * The match code in the bracket, like V1-3
	 */
	@Column(nullable = false)
	private String matchCode;

	/**
	 * The round of the battle, from 1
	 */
	private int roundNumber;

	/**
	 * True when the subject was destroyed, false when it hit
	 */
	private boolean destruction;

	/**
	 * The robot that fired the hit, or the robot that was destroyed
	 */
	@Column(nullable = false)
	private long subjectId;

	/**
	 * Power of the hit, zero for a destruction
	 */
	private double power;

	/**
	 * The two robots of the battle
	 */
	@Column(nullable = false)
	private long firstId;

	@Column(nullable = false)
	private long secondId;

	/**
	 * The picture of the 3D view, a JPEG
	 */
	private byte[] image;

	/** Returns the id. */
	public long getId() {
		return id;
	}

	/** Returns the championship id. */
	public long getChampionshipId() {
		return championshipId;
	}

	/** Sets the championship id. */
	public void setChampionshipId(long championshipId) {
		this.championshipId = championshipId;
	}

	/** Returns the match code. */
	public String getMatchCode() {
		return matchCode;
	}

	/** Sets the match code. */
	public void setMatchCode(String matchCode) {
		this.matchCode = matchCode;
	}

	/** Returns the round number. */
	public int getRoundNumber() {
		return roundNumber;
	}

	/** Sets the round number. */
	public void setRoundNumber(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	/** Tells whether the destruction. */
	public boolean isDestruction() {
		return destruction;
	}

	/** Sets the destruction. */
	public void setDestruction(boolean destruction) {
		this.destruction = destruction;
	}

	/** Returns the subject id. */
	public long getSubjectId() {
		return subjectId;
	}

	/** Sets the subject id. */
	public void setSubjectId(long subjectId) {
		this.subjectId = subjectId;
	}

	/** Returns the power. */
	public double getPower() {
		return power;
	}

	/** Sets the power. */
	public void setPower(double power) {
		this.power = power;
	}

	/** Returns the first id. */
	public long getFirstId() {
		return firstId;
	}

	/** Sets the first id. */
	public void setFirstId(long firstId) {
		this.firstId = firstId;
	}

	/** Returns the second id. */
	public long getSecondId() {
		return secondId;
	}

	/** Sets the second id. */
	public void setSecondId(long secondId) {
		this.secondId = secondId;
	}

	/** Returns the image. */
	public byte[] getImage() {
		return image;
	}

	/** Sets the image. */
	public void setImage(byte[] image) {
		this.image = image;
	}
}

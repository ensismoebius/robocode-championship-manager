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

	public long getId() {
		return id;
	}

	public long getChampionshipId() {
		return championshipId;
	}

	public void setChampionshipId(long championshipId) {
		this.championshipId = championshipId;
	}

	public String getMatchCode() {
		return matchCode;
	}

	public void setMatchCode(String matchCode) {
		this.matchCode = matchCode;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public void setRoundNumber(int roundNumber) {
		this.roundNumber = roundNumber;
	}

	public boolean isDestruction() {
		return destruction;
	}

	public void setDestruction(boolean destruction) {
		this.destruction = destruction;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(long subjectId) {
		this.subjectId = subjectId;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public long getFirstId() {
		return firstId;
	}

	public void setFirstId(long firstId) {
		this.firstId = firstId;
	}

	public long getSecondId() {
		return secondId;
	}

	public void setSecondId(long secondId) {
		this.secondId = secondId;
	}

	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}
}

package model;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class Opponent {

	public static final int MAX_BATTLE_CRY_LENGTH = 500;

	/**
	 * Column size of files. Not mapped as Lob because the SQLite driver does not
	 * support java.sql.Blob/Clob, plain binary/text columns hold them fine
	 */
	private static final int MAX_FILE_LENGTH = 16 * 1024 * 1024;

	private long id;

	private String name;

	private String team;

	private String battleCry;

	private String sourceCode;

	private byte[] image;

	private byte[] music;

	private byte[] compiledRobot;

	private double musicStart;

	private List<Author> authors;

	/** Creates an empty robot, with no authors, to be filled later. */
	public Opponent() {
		this.authors = new ArrayList<Author>();
	}

	/**
	 * The robot class name as robocode knows it: team.RobotName
	 */
	@Transient
	public String getFullName() {
		return this.team + "." + this.name;
	}

	/** Returns the authors. */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@JoinColumn(name = "opponentId")
	public List<Author> getAuthors() {
		return authors;
	}

	/** Returns the battle cry. */
	@Column(nullable = true, length = MAX_BATTLE_CRY_LENGTH)
	public String getBattleCry() {
		return battleCry;
	}

	/** Returns the compiled robot. */
	@Column(nullable = true, length = MAX_FILE_LENGTH)
	public byte[] getCompiledRobot() {
		return this.compiledRobot;
	}

	/** Returns the id. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	/** Returns the image. */
	@Column(nullable = true, length = MAX_FILE_LENGTH)
	public byte[] getImage() {
		return this.image;
	}

	/** Returns the music. */
	@Column(nullable = true, length = MAX_FILE_LENGTH)
	public byte[] getMusic() {
		return this.music;
	}

	/** Returns the music start. */
	@Column(nullable = true)
	public double getMusicStart() {
		return musicStart;
	}

	/** Returns the name. */
	@Column(nullable = false)
	public String getName() {
		return name;
	}

	/** Returns the source code. */
	@Column(nullable = true, length = MAX_FILE_LENGTH)
	public String getSourceCode() {
		return sourceCode;
	}

	/** Returns the team. */
	@Column(nullable = false)
	public String getTeam() {
		return team;
	}

	/** Sets the authors. */
	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}

	/** Sets the battle cry. */
	public void setBattleCry(String battleCry) {
		this.battleCry = battleCry;
	}

	/** Sets the compiled robot. */
	public void setCompiledRobot(byte[] jarBytes) {
		this.compiledRobot = jarBytes;
	}

	/** Sets the id. */
	public void setId(long identifier) {
		this.id = identifier;
	}

	/** Sets the image. */
	public void setImage(byte[] pictureBytes) {
		this.image = pictureBytes;
	}

	/** Sets the music. */
	public void setMusic(byte[] musicBytes) {
		this.music = musicBytes;
	}

	/** Sets the music start. */
	public void setMusicStart(double musicStart) {
		this.musicStart = musicStart;
	}

	/** Sets the name. */
	public void setName(String name) {
		this.name = name;
	}

	/** Sets the source code. */
	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	/** Sets the team. */
	public void setTeam(String team) {
		this.team = team;
	}

}

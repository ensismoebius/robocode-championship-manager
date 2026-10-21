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

	public Opponent() {
		this.authors = new ArrayList<Author>();
	}

	/**
	 * The robot class name as robocode knows it: team.RobotName
	 */
	@Transient
	/** Returns the full name. */
	public String getFullName() {
		return this.team + "." + this.name;
	}

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	@JoinColumn(name = "opponentId")
	/** Returns the authors. */
	public List<Author> getAuthors() {
		return authors;
	}

	@Column(nullable = true, length = MAX_BATTLE_CRY_LENGTH)
	/** Returns the battle cry. */
	public String getBattleCry() {
		return battleCry;
	}

	@Column(nullable = true, length = MAX_FILE_LENGTH)
	/** Returns the compiled robot. */
	public byte[] getCompiledRobot() {
		return this.compiledRobot;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	/** Returns the id. */
	public long getId() {
		return id;
	}

	@Column(nullable = true, length = MAX_FILE_LENGTH)
	/** Returns the image. */
	public byte[] getImage() {
		return this.image;
	}

	@Column(nullable = true, length = MAX_FILE_LENGTH)
	/** Returns the music. */
	public byte[] getMusic() {
		return this.music;
	}

	@Column(nullable = true)
	/** Returns the music start. */
	public double getMusicStart() {
		return musicStart;
	}

	@Column(nullable = false)
	/** Returns the name. */
	public String getName() {
		return name;
	}

	@Column(nullable = true, length = MAX_FILE_LENGTH)
	/** Returns the source code. */
	public String getSourceCode() {
		return sourceCode;
	}

	@Column(nullable = false)
	/** Returns the team. */
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

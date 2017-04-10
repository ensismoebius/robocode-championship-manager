package model;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
@Entity
public class Opponent implements Cloneable {

	private long id;

	private List<Integer> levels;

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
		this.levels = new ArrayList<Integer>();
		this.authors = new ArrayList<Author>();
	}

	public Opponent(String name, URI image) {
		this();
		this.name = name;
		this.setImage(image);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Opponent o = new Opponent();
		o.setName(this.name);
		o.setTeam(this.team);
		o.setImage(this.image);
		o.setLevels(this.levels);
		o.setMusic(this.music);
		o.setBattleCry(this.battleCry);
		o.setSourceCode(this.sourceCode);
		o.setCompiledRobot(this.compiledRobot);

		this.authors.forEach(author -> {
			try {
				o.getAuthors().add((Author) author.clone());
			} catch (CloneNotSupportedException e) {
			}
		});

		return o;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "opponentId")
	public List<Author> getAuthors() {
		return authors;
	}

	@Column(nullable = true)
	public String getBattleCry() {
		return battleCry;
	}

	@Column(nullable = true, columnDefinition = "LONGBLOB")
	public byte[] getCompiledRobot() {
		return this.compiledRobot;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	@Column(nullable = true, columnDefinition = "LONGBLOB")
	public byte[] getImage() {
		return this.image;
	}

	@ElementCollection(fetch = FetchType.EAGER)
	public List<Integer> getLevels() {
		return levels;
	}

	@Column(nullable = true, columnDefinition = "LONGBLOB")
	public byte[] getMusic() {
		return this.music;
	}

	@Column(nullable = true)
	public double getMusicStart() {
		return musicStart;
	}

	@Column(nullable = false)
	public String getName() {
		return name;
	}

	@Lob
	@Column(nullable = true)
	public String getSourceCode() {
		return sourceCode;
	}

	@Column(nullable = false)
	public String getTeam() {
		return team;
	}

	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}

	public void setBattleCry(String battleCry) {
		this.battleCry = battleCry;
	}

	public void setCompiledRobot(byte[] jarBytes) {
		this.compiledRobot = jarBytes;
	}

	public void setCompiledRobot(URI compiledRobot) {

		File file = new File(compiledRobot);
		byte[] jarBytes = new byte[(int) file.length()];

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(jarBytes);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.compiledRobot = jarBytes;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setImage(byte[] pictureBytes) {
		this.image = pictureBytes;
	}

	public void setImage(URI image) {

		File file = new File(image);
		byte[] pictureBytes = new byte[(int) file.length()];

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(pictureBytes);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.image = pictureBytes;
	}

	public void setLevels(List<Integer> levels) {
		this.levels = levels;
	}

	public void setMusic(byte[] musicBytes) {
		this.music = musicBytes;
	}

	public void setMusic(URI music) {

		File file = new File(music);
		byte[] musicBytes = new byte[(int) file.length()];

		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(musicBytes);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.music = musicBytes;
	}

	public void setMusicStart(double musicStart) {
		this.musicStart = musicStart;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public void setTeam(String team) {
		this.team = team;
	}

}

package widgets.scenes;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The kinds of events of a battle, each one commented by its own presenter
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public enum Topic {
	/** Walls and collisions */
	FOULS("Zuri", "faltas"),
	/** Hits, critical damage and combos */
	BLOWS("Dandara", "golpes"),
	/** Long shots, bullets that cancel each other, misses */
	AIM("Luana", "pontaria"),
	/** The state of the robots: fire, danger, double knock out */
	HEALTH("Nala", "saude-dos-robos"),
	/** Rounds, fight, knock out and the headlines of the end */
	SHOW("Kayodê", "abertura-e-fim"),
	/** Who is ahead, comebacks and long silences */
	PACE("Caio", "ritmo-da-luta"),
	/** Numbers and achievements */
	STATS("Benê", "estatisticas");

	/** The name of the presenter */
	public final String presenter;
	private final String file;

	/** A topic of the presenter: the presenter name and the file of the text. */
	Topic(String presenter, String file) {
		this.presenter = presenter;
		this.file = file;
	}

	/**
	 * Where the picture of the presenter is in the classpath
	 */
	public String image() {
		return "/assets/" + this.file + "-transparente.png";
	}
}

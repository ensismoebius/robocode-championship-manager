package widgets.scenes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import utils.robocode.BattleRecording;
import utils.robocode.BattleRecording.Bullet;
import utils.robocode.BattleRecording.RobotState;
import utils.robocode.BattleRecording.Turn;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Watches the battle turn by turn and says what is happening: callouts, like
 * in fighting games ("PRIMEIRO HIT!", "DANO CRÍTICO!", "PERIGO!"), and the
 * comments of a commentator.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class BattleCommentary {

	/** Robocode's strongest bullet */
	static final double MAX_POWER = 3;
	/** A bullet of full power takes this energy, below it the robot is in danger */
	static final double DANGER_ENERGY = 16;
	/** Energy behind the opponent that makes a win a comeback */
	static final double COMEBACK_DEFICIT = 30;
	/** Energy difference to take the lead */
	static final double LEAD_MARGIN = 15;
	/** Turns without hits to comment the silence (15 seconds) */
	static final int QUIET_TURNS = 450;
	static final double SNIPER_DISTANCE = 450;
	static final double POINT_BLANK_DISTANCE = 90;

	/** How it is shown */
	public enum Tone {
		/** In the color of the robot */
		PLAYER,
		/** Red, blinking */
		DANGER,
		/** Golden, for the big moments */
		GOLD,
		/** Neither robot */
		NEUTRAL
	}

	/**
	 * @param callout
	 *            a big callout, otherwise a comment
	 * @param robot
	 *            the robot it is about, -1 if none
	 * @param priority
	 *            the bigger the more important
	 * @param topic
	 *            what kind of event it is, so the presenter of it comments
	 */
	public record Message(boolean callout, String text, int robot, Tone tone, int priority, Topic topic) {
	}

	/** Shown when the battle ends, if it was special */
	public record Headline(String title, String subtitle, Topic topic) {
	}

	private static final class Stats {
		int shots;
		int hits;
		int combo;
		int missStreak;
		int wallHits;
		double minEnergy = 100;
		double energy = 100;
		double maxDeficit;
		boolean danger;
		boolean hitByBullet;
		boolean dead;
	}

	private final Map<Integer, String> names;
	private final Random random;
	private final Map<Integer, Stats> stats = new HashMap<Integer, Stats>();
	private final Map<Integer, Integer> roundWins = new HashMap<Integer, Integer>();
	/** Robots that were behind in rounds won at some moment */
	private final Set<Integer> wereBehind = new HashSet<Integer>();
	private final Set<Integer> bulletsSeen = new HashSet<Integer>();
	private final Set<Integer> bulletsResolved = new HashSet<Integer>();
	private int lastTurn;
	private boolean firstHit;
	private int leader;
	private int lastActionTurn;
	private int lastRamTurn;
	private int roundsPlayed;
	private boolean tiedBeforeLast;

	/**
	 * @param names
	 *            the name of each robot, by its index
	 */
	public BattleCommentary(Map<Integer, String> names, Random random) {
		this.names = names;
		this.random = random;
		for (Integer index : names.keySet()) {
			this.roundWins.put(index, 0);
		}
	}

	/** Starts a new round: the statistics of every robot are cleared. */
	public void startRound() {
		this.stats.clear();
		for (Integer index : this.names.keySet()) {
			this.stats.put(index, new Stats());
		}
		this.bulletsSeen.clear();
		this.bulletsResolved.clear();
		this.lastTurn = -1;
		this.firstHit = false;
		this.leader = -1;
		this.lastActionTurn = 0;
		this.lastRamTurn = -1000;
	}

	/**
	 * What happened in this turn. Turns already seen (like in a replay) say
	 * nothing.
	 */
	public List<Message> turn(Turn turn, Turn previous) {
		List<Message> messages = new ArrayList<Message>();
		if (turn.turn() <= this.lastTurn) {
			return messages;
		}
		this.lastTurn = turn.turn();
		if (this.stats.isEmpty()) {
			this.startRound();
		}

		this.bullets(turn, messages);
		this.robots(turn, previous, messages);

		if (turn.turn() - this.lastActionTurn > QUIET_TURNS) {
			this.lastActionTurn = turn.turn();
			messages.add(this.comment(-1, 1, Topic.PACE, "Os dois se estudam... ninguém acerta nada!", "Tiros pra todo lado e nada de acertar!", "Silêncio na arena: quem vai arriscar primeiro?"));
		}
		return messages;
	}

	/** Comments the bullets of a turn: the shots that are fired. */
	private void bullets(Turn turn, List<Message> messages) {
		for (Bullet bullet : turn.bullets()) {
			Stats owner = this.stats.get(bullet.owner());
			if (owner == null) {
				continue;
			}
			// Counted when it shows up, even if it hits in the same turn
			if (this.bulletsSeen.add(bullet.id())) {
				owner.shots++;
			}
			if (!bullet.isHit() || !this.bulletsResolved.add(bullet.id())) {
				continue;
			}
			if (bullet.state() == BattleRecording.BULLET_HIT_VICTIM) {
				this.hit(turn, bullet, owner, messages);
			} else if (bullet.state() == BattleRecording.BULLET_HIT_BULLET) {
				this.lastActionTurn = turn.turn();
				// Both bullets end in the same turn, said once
				if (messages.stream().noneMatch(matrix -> matrix.text().equals("TIROS ANULADOS!"))) {
					messages.add(new Message(true, "TIROS ANULADOS!", -1, Tone.NEUTRAL, 2, Topic.AIM));
					messages.add(this.comment(-1, 2, Topic.AIM, "Tiro contra tiro! Os projéteis se anularam no ar!", "Que reflexo! Bala na bala!"));
				}
			} else {
				owner.combo = 0;
				owner.missStreak++;
				if (owner.missStreak == 6 || owner.missStreak == 12) {
					messages.add(this.comment(bullet.owner(), 1, Topic.AIM, "{A} errou " + owner.missStreak + " tiros seguidos... calibra essa mira!",
							"{A} está atirando nas paredes: " + owner.missStreak + " erros seguidos!"));
				}
			}
		}
	}

	/** Comments a bullet that hit a robot: the attacker and the victim. */
	private void hit(Turn turn, Bullet bullet, Stats owner, List<Message> messages) {
		int attacker = bullet.owner();
		int victim = bullet.victim();
		Stats target = this.stats.get(victim);
		this.lastActionTurn = turn.turn();
		owner.hits++;
		owner.combo++;
		owner.missStreak = 0;
		if (target != null) {
			target.combo = 0;
			target.hitByBullet = true;
		}

		if (!this.firstHit) {
			this.firstHit = true;
			messages.add(new Message(true, "PRIMEIRO HIT!", attacker, Tone.PLAYER, 3, Topic.BLOWS));
			messages.add(this.comment(attacker, 3, Topic.BLOWS, "{A} abre o placar!", "{A} acerta primeiro!", "Primeiro sangue para {A}!"));
		}
		if (bullet.power() >= MAX_POWER - 0.05) {
			messages.add(new Message(true, "DANO CRÍTICO!", attacker, Tone.GOLD, 4, Topic.BLOWS));
			messages.add(this.comment(attacker, 3, Topic.BLOWS, "Que pancada de {A}! Tiro de potência máxima!", "{B} sentiu esse golpe!", "Potência máxima! {A} não economizou energia!"));
		}
		if (owner.combo >= 3 && owner.combo % 2 == 1) {
			messages.add(new Message(true, "COMBO x" + owner.combo + "!", attacker, Tone.PLAYER, 3, Topic.BLOWS));
			if (owner.combo == 3) {
				messages.add(this.comment(attacker, 2, Topic.BLOWS, "{A} está pegando fogo!", "Sequência implacável de {A}!", "{B} não consegue sair da mira de {A}!"));
			}
		}

		RobotState shooter = find(turn, attacker);
		RobotState hit = find(turn, victim);
		if (shooter != null && hit != null) {
			double distance = Math.hypot(shooter.x() - hit.x(), shooter.y() - hit.y());
			if (distance > SNIPER_DISTANCE) {
				messages.add(new Message(true, "SNIPER!", attacker, Tone.PLAYER, 2, Topic.AIM));
				messages.add(this.comment(attacker, 2, Topic.AIM, "Que pontaria de {A}! Acertou do outro lado da arena!", "{A} acerta de longe, tiro de sniper!"));
			} else if (distance < POINT_BLANK_DISTANCE) {
				messages.add(new Message(true, "À QUEIMA-ROUPA!", attacker, Tone.PLAYER, 2, Topic.BLOWS));
			}
		}
	}

	/** Comments the robots of a turn: the deaths and the energy that changes. */
	private void robots(Turn turn, Turn previous, List<Message> messages) {
		int deaths = 0;
		for (RobotState state : turn.robots()) {
			Stats robot = this.stats.get(state.index());
			if (robot == null) {
				continue;
			}
			RobotState before = previous == null ? null : find(previous, state.index());
			robot.energy = state.isDead() ? 0 : state.energy();
			robot.minEnergy = Math.min(robot.minEnergy, robot.energy);

			if (state.isDead()) {
				if (!robot.dead) {
					robot.dead = true;
					deaths++;
				}
				continue;
			}
			boolean changed = before == null || before.state() != state.state();
			if (state.state() == BattleRecording.ROBOT_HIT_WALL && changed) {
				robot.wallHits++;
				if (robot.wallHits == 1 || robot.wallHits % 4 == 0) {
					messages.add(this.comment(state.index(), 1, Topic.FOULS, "{A} deu de cara com o campo de força!", "{A} testou a parede da arena... e perdeu.", "Cuidado com a parede, {A}!"));
				}
			}
			if (state.state() == BattleRecording.ROBOT_HIT_ROBOT && changed && turn.turn() - this.lastRamTurn > 90) {
				this.lastRamTurn = turn.turn();
				messages.add(this.comment(state.index(), 2, Topic.FOULS, "{A} e {B} se chocam! Luta corpo a corpo!", "Batida! {A} parte pra cima de {B}!"));
			}
			if (robot.energy < DANGER_ENERGY && !robot.danger) {
				robot.danger = true;
				messages.add(new Message(true, "PERIGO!", state.index(), Tone.DANGER, 4, Topic.HEALTH));
				messages.add(this.comment(state.index(), 3, Topic.HEALTH, "{A} está por um fio! Mais um tiro forte e acabou!", "Alerta vermelho para {A}!", "{A} precisa de um milagre!"));
			}
		}
		if (deaths >= 2) {
			messages.add(new Message(true, "DUPLO K.O.!", -1, Tone.GOLD, 5, Topic.HEALTH));
			messages.add(this.comment(-1, 4, Topic.HEALTH, "Os dois caem juntos! Ninguém leva esse round!"));
		}
		this.deficitsAndLead(messages);
	}

	/**
	 * How far behind each one was, and who leads in energy
	 */
	private void deficitsAndLead(List<Message> messages) {
		List<Integer> alive = this.stats.entrySet().stream().filter(event -> !event.getValue().dead).map(Map.Entry::getKey).toList();
		for (Map.Entry<Integer, Stats> entry : this.stats.entrySet()) {
			double best = 0;
			for (Map.Entry<Integer, Stats> other : this.stats.entrySet()) {
				if (other.getKey() != entry.getKey().intValue()) {
					best = Math.max(best, other.getValue().energy);
				}
			}
			entry.getValue().maxDeficit = Math.max(entry.getValue().maxDeficit, best - entry.getValue().energy);
		}
		if (alive.size() != 2) {
			return;
		}
		Stats stats2 = this.stats.get(alive.get(0));
		Stats stats3 = this.stats.get(alive.get(1));
		int ahead = stats2.energy - stats3.energy >= LEAD_MARGIN ? alive.get(0) : stats3.energy - stats2.energy >= LEAD_MARGIN ? alive.get(1) : -1;
		if (ahead >= 0 && ahead != this.leader) {
			if (this.leader >= 0) {
				messages.add(this.comment(ahead, 3, Topic.PACE, "Reviravolta! {A} assume a liderança!", "{A} vira o jogo e passa à frente!", "Agora quem manda é {A}!"));
			} else {
				messages.add(this.comment(ahead, 1, Topic.PACE, "{A} abre vantagem!", "{A} começa a dominar a luta!"));
			}
			this.leader = ahead;
		}
	}

	/**
	 * The round ended
	 *
	 * @param winner
	 *            the one left alive, -1 if none
	 */
	public List<Message> roundEnd(int winner) {
		List<Message> messages = new ArrayList<Message>();
		this.roundsPlayed++;
		if (winner >= 0 && this.roundWins.containsKey(winner)) {
			this.roundWins.merge(winner, 1, Integer::sum);
			Stats won = this.stats.get(winner);
			if (won != null) {
				if (won.maxDeficit >= COMEBACK_DEFICIT || won.danger) {
					messages.add(new Message(true, "VITÓRIA DE VIRADA!", winner, Tone.GOLD, 5, Topic.PACE));
					messages.add(this.comment(winner, 4, Topic.PACE, "Que virada de {A}! Estava perdendo e deu a volta por cima!", "Inacreditável! {A} vence de virada!"));
				} else if (!won.hitByBullet) {
					messages.add(new Message(true, "PERFECT!", winner, Tone.GOLD, 5, Topic.STATS));
					messages.add(this.comment(winner, 4, Topic.STATS, "{A} vence sem levar um tiro sequer!", "Atuação perfeita de {A}!"));
				}
				if (won.energy < 10) {
					messages.add(new Message(true, "POR UM FIO!", winner, Tone.DANGER, 4, Topic.HEALTH));
				}
			}
		}
		String accuracy = this.accuracy();
		if (accuracy != null) {
			messages.add(new Message(false, "Precisão no round: " + accuracy, -1, Tone.NEUTRAL, 2, Topic.STATS));
		}

		// Who was behind in rounds
		int best = this.roundWins.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		for (Map.Entry<Integer, Integer> entry : this.roundWins.entrySet()) {
			if (entry.getValue() < best) {
				this.wereBehind.add(entry.getKey());
			}
		}
		return messages;
	}

	/**
	 * Call before the last round: if it is tied, it decides the battle
	 */
	public void lastRoundStarts() {
		this.tiedBeforeLast = this.roundsPlayed > 0 && this.roundWins.values().stream().distinct().count() == 1;
	}

	/** The accuracy of every robot, for the summary of the battle. */
	private String accuracy() {
		List<String> parts = new ArrayList<String>();
		for (Integer index : this.names.keySet()) {
			Stats robot = this.stats.get(index);
			if (robot == null || robot.shots == 0) {
				continue;
			}
			parts.add(this.names.get(index) + " " + Math.round(100.0 * robot.hits / robot.shots) + "%");
		}
		return parts.isEmpty() ? null : String.join("  •  ", parts);
	}

	/**
	 * How the battle ended, if it is worth a headline: a comeback, all the rounds
	 * won or a decision in the last round
	 */
	public Headline battleEnd() {
		int best = this.roundWins.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		List<Integer> leaders = this.roundWins.entrySet().stream().filter(event -> event.getValue() == best).map(Map.Entry::getKey).toList();
		if (best == 0 || leaders.size() != 1 || this.roundsPlayed < 2) {
			return null;
		}
		int winner = leaders.get(0);
		String name = this.names.get(winner);
		int other = this.roundWins.entrySet().stream().filter(event2 -> event2.getKey() != winner).mapToInt(Map.Entry::getValue).max().orElse(0);
		String score = best + " a " + other;

		if (this.wereBehind.contains(winner)) {
			return new Headline("VIRADA HISTÓRICA!", name + " vence a batalha de virada por " + score, Topic.SHOW);
		}
		if (other == 0) {
			return new Headline("VITÓRIA ESMAGADORA!", name + " vence todos os " + best + " rounds", Topic.STATS);
		}
		if (this.tiedBeforeLast) {
			return new Headline("DECIDIDO NO ÚLTIMO ROUND!", name + " vence por " + score, Topic.STATS);
		}
		return null;
	}

	/**
	 * One of the texts, with {A} the robot and {B} its opponent
	 */
	private Message comment(int robot, int priority, Topic topic, String... texts) {
		String text = texts[this.random.nextInt(texts.length)];
		String string = robot >= 0 ? this.names.getOrDefault(robot, "?") : "";
		String string2 = this.names.entrySet().stream().filter(event -> event.getKey() != robot).map(Map.Entry::getValue).findFirst().orElse("");
		return new Message(false, text.replace("{A}", string).replace("{B}", string2), robot, Tone.PLAYER, priority, topic);
	}

	/** The state of the robot with the given index in a turn, or null when there is none. */
	private static RobotState find(Turn turn, int index) {
		for (RobotState state : turn.robots()) {
			if (state.index() == index) {
				return state;
			}
		}
		return null;
	}
}

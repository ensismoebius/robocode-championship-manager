package widgets.scenes;

import java.util.EnumMap;
import java.util.Map;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Keeps the presenters in balance during a battle: it counts how many comments each topic had to say and how many times each
 * presenter was on the screen. A topic that has plenty to say, like the blows, would take the screen from the others, so
 * the chance of a comment of a presenter who was on more than the others goes down, and the chance of one who was on less
 * goes up. The same presenter is not welcome twice in a row.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class PresenterBalance {

	/** How much less welcome a presenter is right after being on the screen */
	private static final double REPEAT = 0.5;

	private final Map<Topic, Integer> offered = new EnumMap<Topic, Integer>(Topic.class);
	private final Map<Topic, Integer> shown = new EnumMap<Topic, Integer>(Topic.class);
	private Topic last;

	/** A comment of the topic wants the screen, whether it gets it or not. */
	void offer(Topic topic) {
		this.offered.merge(topic, 1, Integer::sum);
	}

	/** The presenter of the topic is on the screen. */
	void appeared(Topic topic) {
		this.shown.merge(topic, 1, Integer::sum);
		this.last = topic;
	}

	/** How many times the presenter of the topic was on the screen. */
	int appearances(Topic topic) {
		return this.shown.getOrDefault(topic, 0);
	}

	/**
	 * What the chance of a comment of the topic is multiplied by: less than 1 for a presenter who was on more than the others
	 * that have something to say, more than 1 for one who was on less, and half of it right after the same presenter.
	 */
	double factor(Topic topic) {
		double total = this.appearances(topic);
		int topics = 1;
		for (Topic other : this.offered.keySet()) {
			if (other != topic) {
				total += this.appearances(other);
				topics++;
			}
		}
		double behind = total / topics - this.appearances(topic);
		double factor = behind < 0 ? 1 / (1 - behind) : 1 + behind;
		return topic == this.last ? factor * REPEAT : factor;
	}
}

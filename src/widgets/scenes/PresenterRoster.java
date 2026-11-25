package widgets.scenes;

import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Who of the presenters says each comment, so that all of them are on the screen the same number of times during a championship.
 * A comment is said by the presenter of its topic when no one was on the screen fewer times than that presenter; otherwise by one
 * of those who were on the fewest times. So, at any moment, the one who was on the most was on at most once more than the one who
 * was on the least. The count goes with the championship (see {@link #format} and {@link #parse}), across all its battles.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class PresenterRoster {

	private final Map<Topic, Integer> appearances = new EnumMap<Topic, Integer>(Topic.class);
	/** Who was the last on the screen, so the same one does not come twice in a row when another can */
	private Topic last;

	/** A roster where no one was on the screen yet. */
	public PresenterRoster() {
	}

	/**
	 * The roster as {@link #format} wrote it; what is not understood is taken as no appearances.
	 *
	 * @param text
	 *            like "FOULS=3;BLOWS=4", or null for a championship that has none yet
	 */
	public static PresenterRoster parse(String text) {
		PresenterRoster roster = new PresenterRoster();
		if (text == null || text.isBlank()) {
			return roster;
		}
		for (String entry : text.split(";")) {
			String[] parts = entry.split("=", 2);
			if (parts.length != 2) {
				continue;
			}
			try {
				int count = Integer.parseInt(parts[1].trim());
				if (count > 0) {
					roster.appearances.put(Topic.valueOf(parts[0].trim()), count);
				}
			} catch (IllegalArgumentException unknown) {
				// A presenter that does not exist anymore, or a broken number: not counted
			}
		}
		return roster;
	}

	/** The roster as text, to keep with the championship. */
	public String format() {
		StringJoiner text = new StringJoiner(";");
		for (Topic topic : Topic.values()) {
			text.add(topic.name() + "=" + this.appearances(topic));
		}
		return text.toString();
	}

	/** How many times the presenter of the topic was on the screen. */
	public int appearances(Topic topic) {
		return this.appearances.getOrDefault(topic, 0);
	}

	/** How many times the presenter who was on the screen the least was on. */
	private int fewest() {
		int fewest = Integer.MAX_VALUE;
		for (Topic topic : Topic.values()) {
			fewest = Math.min(fewest, this.appearances(topic));
		}
		return fewest;
	}

	/**
	 * Who says a comment of the topic: its own presenter, if no one was on the screen fewer times; otherwise the next one, in the
	 * order of the topics, among those who were on the fewest times. The one who was just on the screen only comes again when no
	 * other one can.
	 */
	public Topic speaker(Topic topic) {
		int fewest = this.fewest();
		Topic[] topics = Topic.values();
		for (int step = 0; step < topics.length; step++) {
			Topic candidate = topics[(topic.ordinal() + step) % topics.length];
			if (this.appearances(candidate) == fewest && candidate != this.last) {
				return candidate;
			}
		}
		return this.last;
	}

	/** The presenter is on the screen. */
	public void appeared(Topic speaker) {
		this.appearances.merge(speaker, 1, Integer::sum);
		this.last = speaker;
	}
}

package utils.robocode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Watches the lines of a battle while it is fought and tells the colors of each robot as soon as they are known, which is long
 * before the battle ends and the recording is made: "#N" names a robot and "#C" gives its colors, body, gun and radar.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class BattleColors {

	/** Told the first colors of a robot, with its name as robocode reports it, like "team.Robot 1.0". */
	@FunctionalInterface
	public interface Listener {
		void colored(String robotName, int body, int gun, int radar);
	}

	private final Listener listener;
	private final Map<Integer, String> names = new HashMap<Integer, String>();
	private final Set<Integer> told = new HashSet<Integer>();

	/** Watches the lines of a battle and tells the listener the first colors of each robot. */
	public BattleColors(Listener listener) {
		this.listener = listener;
	}

	/** Takes a line printed by the battle: the ones that are not of a name or of colors tell nothing. */
	public void accept(String line) {
		try {
			if (line.startsWith("#N ")) {
				String body = line.substring(3);
				int space = body.indexOf(' ');
				this.names.put(Integer.parseInt(body.substring(0, space)), body.substring(space + 1));
			} else if (line.startsWith("#C ")) {
				String[] fields = line.substring(3).split(" ");
				int index = Integer.parseInt(fields[0]);
				if (this.names.containsKey(index) && this.told.add(index)) {
					this.listener.colored(this.names.get(index), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]));
				}
			}
		} catch (RuntimeException notAColor) {
			// A broken line tells nothing, as in the recording
		}
	}
}

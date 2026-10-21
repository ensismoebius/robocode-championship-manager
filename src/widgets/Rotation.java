package widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Picks the options in a random order without repeating any until all of them
 * were used, like a shuffled deck, and the first of a new deck is never the last
 * of the previous one. A plain random pick repeats too often.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public final class Rotation<T> {

	private final List<T> options;
	private final List<T> deck = new ArrayList<T>();
	private T last;

	/** A rotation over the given options, which are kept in their order. */
	@SafeVarargs
	public Rotation(T... options) {
		this.options = List.of(options);
	}

	/**
	 * The next option
	 */
	public synchronized T next(Random random) {
		if (this.deck.isEmpty()) {
			this.deck.addAll(this.options);
			Collections.shuffle(this.deck, random);
			// The card on the top is the next one: not the one just used
			if (this.deck.size() > 1 && this.deck.get(this.deck.size() - 1).equals(this.last)) {
				Collections.swap(this.deck, this.deck.size() - 1, random.nextInt(this.deck.size() - 1));
			}
		}
		this.last = this.deck.remove(this.deck.size() - 1);
		return this.last;
	}
}

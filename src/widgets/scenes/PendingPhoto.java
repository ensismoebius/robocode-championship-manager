package widgets.scenes;

import model.Opponent;

/** A photo waiting for its moment: the seconds left, then it is taken */
record PendingPhoto(double remaining, int round, boolean destruction, Opponent subject, double power) {
}

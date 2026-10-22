package widgets.scenes;

import model.Opponent;

/**
 * The whole flight of a bullet, from the muzzle of the gun to where it hits.
 *
 * @param born
 *            the turn (and fraction) when it left the shooter
 * @param duration
 *            turns until it arrives
 * @param hit
 *            the record of the hit, null if it flies until the records end
 */
/**
 * A photo of a key moment of the battle, as JPEG: a hit that hurts or a robot
 * destroyed
 *
 * @param round
 *            the round of the battle, from 1
 * @param destruction
 *            true when the subject was destroyed, false when it hit
 * @param subject
 *            the robot that fired the hit, or the robot destroyed
 * @param power
 *            the power of the hit, zero for a destruction
 * @param jpeg
 *            the picture of the 3D view
 */
public record BattlePhoto(int round, boolean destruction, Opponent subject, double power, byte[] jpeg) {
}

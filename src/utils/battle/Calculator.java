/**
 * 
 */
package utils.battle;

import model.OpponentGroup;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class Calculator {

	public static OpponentGroup[] createBattleGroups(int amountOfOpponets) {

		int groupsSize = calcGroupsSize(amountOfOpponets);
		int groupsAmount = amountOfOpponets / groupsSize;

		OpponentGroup[] groups = new OpponentGroup[groupsAmount];

		for (int i = 0; i < groupsAmount; i++) {
			groups[i] = new OpponentGroup(Calculator.generateCharacter(i), groupsSize);
		}

		return groups;
	}

	private static int calcGroupsSize(int amountOfOpponets) {

		if (amountOfOpponets <= 4) {
			return 2;
		}

		// Used to calculate the maxsize of the groups
		// The groupsize MUST be a power of two because
		// otherwise the battle diagram will be assimetric
		// And that is BAD
		double powerOfTwo = 2;

		// The max amount of opponents in a group
		double groupSize = Math.pow(2, powerOfTwo);

		// The max amount of opponents in a group previsioly calculated
		double previousGroupSize = Math.pow(2, powerOfTwo);

		// Loops until find the result
		while (true) {

			previousGroupSize = groupSize;

			groupSize = Math.pow(2, powerOfTwo);

			// The size factor MUST be greater than 2 but not too greater
			double groupSizeFactor = amountOfOpponets / groupSize;

			// If the next size factor are less than 2 return the previous group size
			if (groupSizeFactor < 2) {
				return (int) previousGroupSize;
			}

			powerOfTwo++;
		}
	}

	private static String generateCharacter(int number) {
		char generatedChar = (char) (65 + number);

		char[] arrChar = new char[1];
		arrChar[0] = generatedChar;

		return new String(arrChar);
	}

}

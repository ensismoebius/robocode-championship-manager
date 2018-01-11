package utils.battle;

import model.OpponentGroup;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class OpponentGroupFactory {

	public static OpponentGroup[] createBattleGroups(int amountOfOpponents, int groupSize) {

		int groupsAmount = amountOfOpponents / groupSize;

		OpponentGroup[] groups = new OpponentGroup[groupsAmount];

		for (int i = 0; i < groupsAmount; i++) {
			groups[i] = new OpponentGroup(OpponentGroupFactory.generateCharacter(i), groupSize);
		}

		return groups;
	}

	private static String generateCharacter(int number) {
		char generatedChar = (char) (65 + number);

		char[] arrChar = new char[1];
		arrChar[0] = generatedChar;

		return new String(arrChar);
	}

}

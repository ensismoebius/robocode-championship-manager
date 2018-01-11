package utils.battle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import model.OpponentGroup;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class OpponentGroupFactory {

	public static ObservableMap<String, model.OpponentGroup> createBattleGroups(int amountOfOpponents, int groupSize) {

		int groupsAmount = amountOfOpponents / groupSize;

		ObservableMap<String, model.OpponentGroup> groups = FXCollections.observableMap(null);

		for (int i = 0; i < groupsAmount; i++) {
			groups.put(OpponentGroupFactory.generateCharacter(i), new OpponentGroup(OpponentGroupFactory.generateCharacter(i), groupSize));
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

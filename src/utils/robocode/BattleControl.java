package utils.robocode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import model.Opponent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class BattleControl {

	public Opponent[] startsBattle(HashMap<String, Opponent> battleList, String robocodePath) {

		Opponent[] results = new Opponent[2];
		Opponent opponent1 = (Opponent) battleList.values().toArray()[0];
		Opponent opponent2 = (Opponent) battleList.values().toArray()[1];

		String robot1 = opponent1.getTeam() + "." + opponent1.getName();
		String robot2 = opponent2.getTeam() + "." + opponent2.getName();

		try {
			File f1 = new File(robocodePath + "/robots/" + robot1 + ".jar");
			f1.createNewFile();
			FileOutputStream fos1 = new FileOutputStream(f1);
			fos1.write(opponent1.getCompiledRobot(), 0, opponent1.getCompiledRobot().length);
			fos1.close();

			File f2 = new File(robocodePath + "/robots/" + robot2 + ".jar");
			f2.createNewFile();
			FileOutputStream fos2 = new FileOutputStream(f2);
			fos2.write(opponent2.getCompiledRobot(), 0, opponent2.getCompiledRobot().length);
			fos2.close();

		} catch (IOException e1) {
			new Alert(AlertType.ERROR, "Fail to save robots start it manually").show();
			return null;
		}

		Process process;
		try {

			// TODO make it flexible!
			process = new ProcessBuilder("RobocodeStarter/start.sh", robocodePath, "3", robot1, robot2).start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			while ((line = br.readLine()) != null) {

				int index = Integer.parseInt(line.split("\t")[0]) - 1;
				results[index] = (Opponent) battleList.values().toArray()[index];

				System.out.println(line);
			}
		} catch (IOException e) {
			new Alert(AlertType.ERROR, "Fail to start the battle").show();
			return null;
		}

		return results;
	}

}

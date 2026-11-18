import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tournament.ChampionshipService;
import tournament.DataArchive;
import widgets.GameDialog;
import widgets.GameWindow;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The data panel of the organizer: the report of the championship, the archive of the championship and the
 * robots, the backup of the database and the removal of the data. The files are written next to the database
 * file, see {@link DataArchive}.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class DataFlow {

	private final Main host;

	/** Asks the organizer for the backup to restore: null when there is none. The tests give it the file. */
	Supplier<Path> backupPicker = DataFlow::chooseBackup;

	/** Creates the data panel of the host window. */
	DataFlow(Main host) {
		this.host = host;
	}

	/** Shows the panel with what can be done with the data, until it is closed. */
	void open() {
		if (this.host.battleRunning) {
			this.host.showError("Aguarde a batalha terminar para mexer nos dados.");
			return;
		}
		Label summary = new Label(this.summary());
		summary.setWrapText(true);
		summary.getStyleClass().add("game-dialog-message");

		VBox actions = new VBox(12, summary);
		Button[] buttons = { this.action("Gerar relatório", "Uma página para ler ou imprimir, e a tabela de partidas", this::report),
				this.action("Arquivar", "Guarda o campeonato e os robôs em um arquivo .zip, e deixa apagar depois", this::archive),
				this.action("Backup", "Copia o banco de dados inteiro", this::backup),
				this.action("Restaurar backup", "Volta o banco de dados a um backup ou a um arquivo .zip", this::restore),
				this.action("Apagar dados", "Remove o campeonato ou tudo, depois de um backup automático", this::delete) };
		actions.getChildren().addAll(buttons);
		Button close = new Button("Fechar");

		VBox panel = GameDialog.panel("Dados", actions, close);
		close.setOnAction(event -> GameWindow.close(panel, null));
		for (Button button : buttons) {
			// The panel closes first: what the action asks goes over the screen, not over the panel
			button.setOnAction(event -> GameWindow.close(panel, button.getUserData()));
		}
		Object chosen = GameWindow.showModal(panel, close, close);
		if (chosen instanceof Runnable work) {
			work.run();
		}
	}

	/** A button of the panel with what it does below its name; its work runs when the panel is closed by it. */
	private Button action(String name, String what, Runnable work) {
		Button button = new Button(name + " — " + what);
		button.setMaxWidth(Double.MAX_VALUE);
		button.setUserData(work);
		return button;
	}

	/** What the organizer has now: the championship, the robots and where the files go. */
	private String summary() {
		String championship = this.host.championship == null ? "nenhum campeonato" : this.host.championship.getName() + " (" + this.host.championship.getResults().size() + " partidas jogadas)";
		return "Agora: " + championship + ", " + ChampionshipService.countOpponents() + " robôs cadastrados.\nOs arquivos ficam em " + DataArchive.folder("") + ".";
	}

	/** Writes the report of the current championship and tells where it is. */
	private void report() {
		if (this.host.championship == null) {
			this.host.showError("Nenhum campeonato criado ainda. Pressione Ctrl + G para sortear um.");
			return;
		}
		try {
			GameDialog.info("Relatório", "Relatório gerado em\n" + DataArchive.report(this.host.championship));
		} catch (IOException | RuntimeException failure) {
			this.fail("gerar o relatório", failure);
		}
	}

	/** Copies the database and tells where the copy is. */
	private void backup() {
		try {
			GameDialog.info("Backup", "Backup do banco de dados em\n" + DataArchive.backup());
		} catch (IOException | RuntimeException failure) {
			this.fail("fazer o backup", failure);
		}
	}

	/** Asks, writes the archive of the championship and the robots and, if asked, erases the data. */
	private void archive() {
		CheckBox erase = new CheckBox("Apagar o campeonato e os robôs depois de arquivar");
		erase.setWrapText(true);
		Label message = new Label("Guardar o campeonato e todos os robôs (código, jar, imagem, música) em um arquivo .zip?");
		message.setWrapText(true);
		message.getStyleClass().add("game-dialog-message");
		if (!GameDialog.confirm("Arquivar", new VBox(14, message, erase))) {
			return;
		}
		Path zip;
		try {
			zip = DataArchive.archive(this.host.championship);
		} catch (IOException | RuntimeException failure) {
			this.fail("arquivar", failure);
			return;
		}
		boolean erased = erase.isSelected() && this.erase(true);
		GameDialog.info("Arquivo", "Arquivado em\n" + zip + (erased ? "\n\nOs dados foram apagados." : ""));
	}

	/** Asks for a backup, restores it and shows the initial screen, which has the restored data. */
	private void restore() {
		Path source = this.backupPicker.get();
		if (source == null) {
			return;
		}
		if (!GameDialog.confirm("Restaurar backup", "Trocar TODOS os dados atuais (campeonato e robôs) pelos de\n" + source.getFileName() + "?\n\nUm backup dos dados atuais é feito antes.")) {
			return;
		}
		try {
			Path safety = DataArchive.restore(source);
			this.host.selectedMatch = null;
			this.host.justPlayed = null;
			this.host.loadChampionship();
			this.host.resetAllScenes();
			this.host.showStartScreen();
			GameDialog.info("Restaurado", "Dados restaurados. O backup de antes está em\n" + safety);
		} catch (IOException | RuntimeException failure) {
			this.fail("restaurar o backup", failure);
		}
	}

	/** The file chooser, in the folder of the backups. */
	private static Path chooseBackup() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Escolha o backup (.db) ou o arquivo (.zip)");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backups e arquivos", "*.db", "*.zip"));
		File folder = DataArchive.folder("backups").toFile();
		if (folder.isDirectory()) {
			chooser.setInitialDirectory(folder);
		}
		File chosen = chooser.showOpenDialog(GameWindow.getWindow());
		return chosen == null ? null : chosen.toPath();
	}

	/** Asks what to erase, backs up the database and erases it. */
	private void delete() {
		RadioButton championships = new RadioButton("Só o campeonato: resultados e fotos (os robôs ficam)");
		RadioButton everything = new RadioButton("Tudo: o campeonato e todos os robôs");
		championships.setWrapText(true);
		everything.setWrapText(true);
		ToggleGroup scope = new ToggleGroup();
		championships.setToggleGroup(scope);
		everything.setToggleGroup(scope);
		championships.setSelected(true);
		Label message = new Label("O que apagar? Um backup do banco de dados é feito antes, e isso não tem volta.");
		message.setWrapText(true);
		message.getStyleClass().add("game-dialog-message");
		if (!GameDialog.confirm("Apagar dados", new VBox(14, message, championships, everything))) {
			return;
		}
		try {
			Path safety = DataArchive.backup();
			if (this.erase(everything.isSelected())) {
				GameDialog.info("Apagado", "Dados apagados. O backup de antes está em\n" + safety);
			}
		} catch (IOException | RuntimeException failure) {
			this.fail("apagar os dados", failure);
		}
	}

	/** Removes the data and shows the initial screen, which has no championship anymore. False when it failed. */
	private boolean erase(boolean robotsToo) {
		try {
			if (robotsToo) {
				DataArchive.deleteAll();
			} else {
				DataArchive.deleteChampionships();
			}
			this.host.selectedMatch = null;
			this.host.justPlayed = null;
			this.host.loadChampionship();
			this.host.resetAllScenes();
			this.host.showStartScreen();
			return true;
		} catch (RuntimeException failure) {
			this.fail("apagar os dados", failure);
			return false;
		}
	}

	/** Shows the failure of something the organizer asked for. */
	private void fail(String what, Exception failure) {
		failure.printStackTrace();
		this.host.showError("Falha ao " + what + ":\n" + Main.rootCauseMessage(failure));
	}
}

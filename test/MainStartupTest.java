import static org.junit.jupiter.api.Assertions.assertTrue;
import static support.DialogFields.button;
import static support.DialogFields.text;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.stage.Stage;
import support.FxSupport;
import widgets.GameWindow;

/** The program when it can not start: the database can not be opened, so the program ends. */
class MainStartupTest {

	@AfterAll
	static void restoreTheExit() {
		GameWindow.setExitAction(Platform::exit);
	}

	@Test
	void aDatabaseThatCanNotBeOpenedEndsTheProgram() throws IOException {
		Path robocode = Path.of("target", "test-home", "robocode-startup");
		Files.createDirectories(robocode.resolve("robots"));
		Path blocker = Path.of("target", "test-home", "startup-blocker");
		Files.createDirectories(blocker.getParent());
		Files.writeString(blocker, "um arquivo, não uma pasta");
		int port;
		try (ServerSocket free = new ServerSocket(0)) {
			port = free.getLocalPort();
		}
		AtomicBoolean ended = new AtomicBoolean();
		GameWindow.setExitAction(() -> ended.set(true));

		// The configuration is saved with a database inside a file, then the error is shown
		FxSupport.onNextDialogs(dialog -> {
			text(dialog, "Arquivo do banco de dados SQLite", blocker.resolve("robochamp.db").toString());
			text(dialog, "Diretório do robocode", robocode.toString());
			text(dialog, "Porta do servidor web de cadastro", String.valueOf(port));
			button(dialog, "Salvar").fire();
		}, dialog -> button(dialog, "OK").fire());
		Main program = FxSupport.run(Main::new);
		Stage stage = FxSupport.run(Stage::new);
		FxSupport.runVoid(() -> program.start(stage));

		assertTrue(ended.get(), "the program ends when its database can not be opened");
	}
}

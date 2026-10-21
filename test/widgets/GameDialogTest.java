package widgets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.scene.control.Label;
import support.DialogFields;
import support.FxSupport;

class GameDialogTest {

	@BeforeAll
	static void openTheWindow() {
		FxSupport.gameWindow();
	}

	@Test
	void aToastShowsItsMessageAtOnce() {
		FxSupport.runVoid(() -> GameDialog.toast("Mensagem de teste"));

		assertNotNull(FxSupport.run(() -> DialogFields.find(GameWindow.getWindow().getScene().getRoot(),
				node -> node instanceof Label label && "Mensagem de teste".equals(label.getText()))));
	}
}

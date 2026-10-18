package widgets.scenes;

import java.io.IOException;
import java.io.InputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import utils.configuration.Configuration;
import utils.network.Network;
import widgets.ArcadeBackground;
import widgets.Sound;
import widgets.Theme;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The title screen
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class StartScreen extends StackPane {

	private static final String[][] SHORTCUTS = { //
			{ "R", "Chaveamento" }, //
			{ "N", "Próxima partida" }, //
			{ "B", "Tela de versus" }, //
			{ "S", "Inicia a batalha" }, //
			{ "1 2", "Declara o vencedor" }, //
			{ "M", "Robôs da partida" }, //
			{ "^G", "Sorteia um campeonato" }, //
			{ "^Z", "Desfaz o último resultado" }, //
			{ "^S", "Abre/fecha as inscrições" }, //
			{ "X", "Exporta o código fonte" }, //
			{ "I", "Esta tela" } };

	private Sound music;

	public StartScreen() {
		super();

		// Center: logo and title
		ImageView robocodeLogo = new ImageView();
		try (InputStream logo = getClass().getResourceAsStream("robocodeLogo.png")) {
			robocodeLogo.setImage(new Image(logo));
		} catch (IOException e) {
			Theme.alert(AlertType.ERROR, e.getMessage()).showAndWait();
		}
		robocodeLogo.setPreserveRatio(true);
		robocodeLogo.setSmooth(true);
		// The logo has a black background, this blends it with the sky
		robocodeLogo.setBlendMode(BlendMode.LIGHTEN);

		Label title = new Label("CHAMPIONSHIP");
		title.getStyleClass().add("game-title");
		Theme.pulseGlow(title, Theme.ACCENT);

		Label subtitle = new Label("GERENCIADOR DE CAMPEONATOS");
		subtitle.getStyleClass().add("game-subtitle");

		Label key = new Label("R");
		key.getStyleClass().add("keycap");
		Label pressBefore = new Label("PRESSIONE");
		pressBefore.getStyleClass().add("press-start");
		Label pressAfter = new Label("PARA O CHAVEAMENTO");
		pressAfter.getStyleClass().add("press-start");
		HBox pressStart = new HBox(16, pressBefore, key, pressAfter);
		pressStart.setAlignment(Pos.CENTER);
		Theme.blink(pressStart);

		VBox titleBlock = new VBox(14, title, subtitle, new Region(), pressStart);
		titleBlock.setAlignment(Pos.CENTER);

		// The logo stays in the sky, the title on the grid
		Pane horizonLayout = new Pane(robocodeLogo, titleBlock) {
			@Override
			protected void layoutChildren() {
				double width = this.getWidth();
				double horizon = this.getHeight() * ArcadeBackground.HORIZON;

				// Fits between the top of the screen and the horizon
				robocodeLogo.setFitWidth(Math.min(560, width * 0.4));
				robocodeLogo.setFitHeight(Math.max(40, horizon * 0.42));
				double logoWidth = robocodeLogo.getLayoutBounds().getWidth();
				double logoHeight = robocodeLogo.getLayoutBounds().getHeight();
				robocodeLogo.relocate((width - logoWidth) / 2, horizon - logoHeight - 24);

				titleBlock.autosize();
				titleBlock.relocate((width - titleBlock.getWidth()) / 2, horizon + 28);
			}
		};
		horizonLayout.setMouseTransparent(true);

		// Top: controls and registration status
		GridPane shortcuts = new GridPane();
		shortcuts.getStyleClass().add("shortcuts");
		for (int i = 0; i < SHORTCUTS.length; i++) {
			Label cap = new Label(SHORTCUTS[i][0]);
			cap.getStyleClass().add("keycap");
			Label description = new Label(SHORTCUTS[i][1]);
			description.getStyleClass().add("shortcut-description");
			shortcuts.addRow(i, cap, description);
		}
		Label controlsTitle = new Label("CONTROLES");
		controlsTitle.getStyleClass().add("panel-title");
		VBox controls = new VBox(controlsTitle, shortcuts);
		controls.getStyleClass().add("panel");
		controls.setMaxHeight(Region.USE_PREF_SIZE);

		boolean open = Configuration.isEnabledRegistration();
		Label badge = new Label(open ? "● INSCRIÇÕES ABERTAS" : "■ INSCRIÇÕES ENCERRADAS");
		badge.getStyleClass().add(open ? "badge-open" : "badge-closed");

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(controls, spacer, badge);
		top.setAlignment(Pos.TOP_LEFT);

		// Bottom: where the teams register their robots
		Label caption = new Label("CADASTRE SEU ROBÔ EM");
		caption.getStyleClass().add("address-caption");
		Label address = new Label("http://" + Network.getLocalIpAddress() + ":" + Configuration.getWebserverPort());
		address.getStyleClass().add("address");
		address.setEffect(Theme.glow(Theme.ACCENT, 14));
		VBox registration = new VBox(caption, address);
		registration.getStyleClass().add("address-box");
		registration.setMaxWidth(Region.USE_PREF_SIZE);
		BorderPane.setAlignment(registration, Pos.CENTER);

		BorderPane layout = new BorderPane(null, top, null, open ? registration : null, null);
		layout.setPadding(new Insets(28));

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.getChildren().setAll(new ArcadeBackground(), horizonLayout, layout, scanlines);

		this.music = Sound.fromResource(getClass(), "versusMusic.mp3");
		if (this.music != null) {
			this.music.setVolume(0.15);
			this.music.setLoop(true);
		}
	}

	public void playBackgroundMusic() {
		if (this.music != null)
			this.music.play();
	}

	public void stop() {
		if (this.music != null)
			this.music.stop();
	}
}

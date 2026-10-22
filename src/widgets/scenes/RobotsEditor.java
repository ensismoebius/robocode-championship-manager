package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Author;
import model.Opponent;
import tournament.OpponentService;
import tournament.OpponentService.RobotData;
import widgets.ArcadeBackground;
import widgets.GameDialog;
import widgets.GameWindow;
import widgets.Sound;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The organizer's screen for the robots: the list of the registered robots, everything each one has (its names,
 * its login, its battle cry, its authors, its picture, its music, its .jar and its source code) with all of it
 * open to change, and the buttons that add a robot and remove one.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotsEditor extends StackPane {

	/** Robocode packager generates files like team.RobotName_1.0.jar */
	private static final Pattern JAR_FILE_NAME = Pattern.compile("^([^.]+)\\.(.+?)_[0-9][0-9.]*\\.jar$");

	private final Runnable onSaved;
	private final Runnable onBack;

	private final ListView<Opponent> robots = new ListView<Opponent>();
	private final Button newRobot = new Button("+ Novo");
	private final Button deleteRobot = new Button("Excluir");
	private final Label title = new Label();
	private final TextField team = new TextField();
	private final TextField name = new TextField();
	private final TextField login = new TextField();
	private final PasswordField password = new PasswordField();
	private final TextField battleCry = new TextField();
	private final Label battleCryCount = new Label();
	private final AuthorRows authors = new AuthorRows(this::markDirty);
	private final Button addAuthor = new Button("+ Adicionar pessoa");
	private final ImageView logo = new ImageView();
	private final Button chooseLogo = new Button("Trocar logotipo...");
	private final Button musicButton = new Button("▶ Ouvir");
	private final Button chooseMusic = new Button("Trocar música...");
	private final TextField musicStart = new TextField();
	private final Label musicInfo = new Label();
	private final Label jarInfo = new Label();
	private final Button chooseJar = new Button("Trocar .jar...");
	private final TextArea source = new TextArea();
	private final Label sourceInfo = new Label();
	private final Button loadSource = new Button("Carregar .java...");
	private final RobotPreview preview = new RobotPreview();
	private final Label error = new Label();
	private final Label saved = new Label();
	private final Button save = new Button("Salvar");

	/** The robot in the form: id 0 when it is a new one */
	private Opponent current;
	/** The files that were chosen and are not saved yet, null for the ones that stay as they are */
	private byte[] pendingImage;
	private byte[] pendingMusic;
	private byte[] pendingJar;
	private Sound music;
	private boolean playing;
	private boolean dirty;

	/**
	 * @param onSaved
	 *            runs after a robot is saved, added or removed, so the rest of the program reads the change
	 * @param onBack
	 *            goes back to the screen the organizer came from
	 */
	public RobotsEditor(Runnable onSaved, Runnable onBack) {
		super();
		this.onSaved = onSaved;
		this.onBack = onBack;

		Region scanlines = new Region();
		scanlines.getStyleClass().add("scanlines");
		scanlines.setMouseTransparent(true);

		this.configureControls();
		this.getChildren().setAll(new ArcadeBackground(), this.buildLayout(), scanlines);
		this.loadRobots(-1);
	}

	/** The title with the buttons, the list of robots at the left and the robot at the right. */
	private BorderPane buildLayout() {
		Label screenTitle = new Label("EQUIPES E ROBÔS");
		screenTitle.getStyleClass().add("game-title");
		screenTitle.setStyle("-fx-font-size: 20px;");
		Button back = new Button("Voltar");
		back.setOnAction(event -> this.goBack());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(16, screenTitle, spacer, back);
		top.setAlignment(Pos.CENTER_LEFT);
		top.setPadding(new Insets(8, 24, 2, 24));

		HBox body = new HBox(16, this.buildList(), this.buildForm());
		body.setPadding(new Insets(6, 24, 6, 24));

		// The result of the save and the button stay in view, whatever the size of the window
		Region gap = new Region();
		HBox.setHgrow(gap, Priority.ALWAYS);
		HBox actions = new HBox(12, this.error, this.saved, gap, this.save);
		actions.setAlignment(Pos.CENTER_LEFT);
		actions.setPadding(new Insets(0, 24, 10, 24));

		BorderPane layout = new BorderPane(body);
		layout.setTop(top);
		layout.setBottom(actions);
		return layout;
	}

	/** What the controls do and how they look. */
	private void configureControls() {
		this.battleCry.textProperty().addListener((observable, before, after) -> {
			this.battleCryCount.setText(after.trim().length() + "/" + Opponent.MAX_BATTLE_CRY_LENGTH);
			this.markDirty();
		});
		this.battleCry.setPromptText("Lema do time");
		for (TextField field : List.of(this.team, this.name, this.login, this.password, this.musicStart)) {
			field.textProperty().addListener((observable, before, after) -> this.markDirty());
		}
		this.source.textProperty().addListener((observable, before, after) -> {
			this.sourceInfo.setText(after.isEmpty() ? "Sem código fonte" : after.lines().count() + " linhas");
			this.markDirty();
		});
		this.team.setPromptText("Time");
		this.name.setPromptText("Robô");
		this.login.setPromptText("Login");
		this.musicStart.setPrefColumnCount(4);

		this.configureButtons();

		this.logo.setPreserveRatio(true);
		this.logo.setFitWidth(88);
		this.logo.setSmooth(true);
		this.source.setWrapText(false);
		this.source.setPrefRowCount(3);
		this.source.setMinHeight(70);
		this.source.getStyleClass().add("text-area");
		VBox.setVgrow(this.source, Priority.ALWAYS);

		this.title.getStyleClass().add("form-title");
		this.title.setStyle("-fx-font-size: 14px; -fx-padding: 0;");
		for (Label hint : List.of(this.battleCryCount, this.musicInfo, this.jarInfo, this.sourceInfo, this.saved)) {
			hint.getStyleClass().add("form-hint");
		}
		this.save.getStyleClass().add("primary");
		this.error.getStyleClass().add("error-text");
	}

	/** What each button does. */
	private void configureButtons() {
		this.addAuthor.setOnAction(event -> this.authors.add(new Author()));
		this.newRobot.setOnAction(event -> this.startNew());
		this.deleteRobot.setOnAction(event -> this.delete());
		this.chooseLogo.setOnAction(event -> this.pickLogo());
		this.chooseMusic.setOnAction(event -> this.pickMusic());
		this.chooseJar.setOnAction(event -> this.pickJar());
		this.loadSource.setOnAction(event -> this.pickSource());
		this.musicButton.setOnAction(event -> this.toggleMusic());
		this.save.setOnAction(event -> this.save());
	}

	/** The list of the robots at the left, with the buttons that add a robot and remove the one in the form. */
	private VBox buildList() {
		this.robots.setPrefWidth(220);
		this.robots.setCellFactory(list -> new ListCell<Opponent>() {
			@Override
			protected void updateItem(Opponent robot, boolean empty) {
				super.updateItem(robot, empty);
				setText(empty || robot == null ? null : robot.getFullName());
			}
		});
		this.robots.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> this.selectionChanged(previous, selected));
		HBox buttons = new HBox(6, this.newRobot, this.deleteRobot);
		VBox list = new VBox(6, this.label("Robôs cadastrados", "panel-title"), buttons, this.robots);
		VBox.setVgrow(this.robots, Priority.ALWAYS);
		return list;
	}

	/** The robot at the right, scrolling when it does not fit. */
	private ScrollPane buildForm() {
		Label identityHint = this.label("Time e robô: letras sem acento, números e _ (no robô, também pontos entre as partes), até 100 caracteres. O login é só deste time. Senha em branco mantém a atual.", "form-hint");
		identityHint.setWrapText(true);

		VBox form = new VBox(4,
				this.title,
				this.identity(), identityHint,
				this.captionRow(this.label("Lema do time", "form-label"), this.battleCryCount), this.battleCry,
				this.captionRow(this.label("Pessoas desenvolvedoras: nome, RM (apenas números) e e-mail", "form-label"), this.addAuthor), this.authors,
				this.files(),
				this.captionRow(this.label("Código fonte (.java)", "form-label"), new HBox(8, this.sourceInfo, this.loadSource)), this.source);
		// A dark panel behind the texts, so they are readable over the background of the arcade
		form.setStyle("-fx-background-color: rgba(11, 14, 20, 0.8); -fx-background-radius: 4;");
		form.setPadding(new Insets(8, 12, 8, 12));

		ScrollPane scroll = new ScrollPane(form);
		scroll.setFitToWidth(true);
		scroll.setFitToHeight(true);
		scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
		HBox.setHgrow(scroll, Priority.ALWAYS);
		return scroll;
	}

	/** The team, the robot, the login and the password, side by side. */
	private GridPane identity() {
		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(2);
		ColumnConstraints quarter = new ColumnConstraints();
		quarter.setPercentWidth(25);
		grid.getColumnConstraints().addAll(quarter, quarter, quarter, quarter);
		String[] captions = { "Time", "Robô", "Login", "Senha" };
		Node[] fields = { this.team, this.name, this.login, this.password };
		for (int column = 0; column < captions.length; column++) {
			grid.add(this.label(captions[column], "form-label"), column, 0);
			grid.add(fields[column], column, 1);
		}
		return grid;
	}

	/** The logo, the music and the .jar with the buttons that change them, and the robot in 3D at the right. */
	private HBox files() {
		HBox start = new HBox(6, this.label("Início (s)", "form-label"), this.musicStart);
		start.setAlignment(Pos.CENTER_LEFT);
		VBox logoBox = new VBox(4, this.label("Logotipo", "form-label"), this.logo, this.chooseLogo);
		VBox musicBox = new VBox(4, this.label("Música tema", "form-label"), this.musicButton, this.chooseMusic, start, this.musicInfo);
		VBox jarBox = new VBox(4, this.label("Robô compilado", "form-label"), this.jarInfo, this.chooseJar);

		// The robot in 3D takes the room that is left
		this.preview.setPrefSize(380, 210);
		this.preview.setMinSize(300, 180);
		Region gap = new Region();
		HBox.setHgrow(gap, Priority.ALWAYS);
		HBox files = new HBox(16, logoBox, musicBox, jarBox, gap, this.preview);
		files.setAlignment(Pos.TOP_LEFT);
		return files;
	}

	/** A caption at the left and something about it at the right, in a single line. */
	private HBox captionRow(Node caption, Node aside) {
		Region gap = new Region();
		HBox.setHgrow(gap, Priority.ALWAYS);
		HBox row = new HBox(8, caption, gap, aside);
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	/** A label with the given style class. */
	private Label label(String text, String styleClass) {
		Label label = new Label(text);
		label.getStyleClass().add(styleClass);
		return label;
	}

	/**
	 * Shows the robots of the database.
	 *
	 * @param selectId
	 *            the robot to show, or any other number for the first one; with no robot the form is for a new one
	 */
	private void loadRobots(long selectId) {
		List<Opponent> all = OpponentService.all();
		this.robots.getItems().setAll(all);
		if (all.isEmpty()) {
			this.show(new Opponent());
			return;
		}
		Opponent chosen = all.stream().filter(robot -> robot.getId() == selectId).findFirst().orElse(all.get(0));
		this.robots.getSelectionModel().select(chosen);
	}

	/** Asks before leaving a robot with changes that were not saved, then shows the selected one. */
	private void selectionChanged(Opponent previous, Opponent selected) {
		if (selected == null) {
			return;
		}
		if (!this.confirmDiscard()) {
			// Back to the robot being edited, after the list finishes the change
			Platform.runLater(() -> {
				if (previous == null) {
					this.robots.getSelectionModel().clearSelection();
				} else {
					this.robots.getSelectionModel().select(previous);
				}
			});
			return;
		}
		this.show(selected);
	}

	/** True when there is nothing to lose, or the organizer agrees to lose the changes that were not saved. */
	private boolean confirmDiscard() {
		return !this.dirty || GameDialog.confirm("Descartar alterações", "Há alterações que não foram salvas. Descartá-las?");
	}

	/** Fills the form with a robot (a new one when its id is 0) and drops the files that were chosen for the last one. */
	private void show(Opponent robot) {
		this.stopMusic();
		this.current = robot;
		boolean isNew = robot.getId() == 0;
		this.pendingImage = null;
		this.pendingMusic = null;
		this.pendingJar = null;

		this.title.setText(isNew ? "Novo robô" : "Robô " + robot.getFullName());
		this.team.setText(robot.getTeam() == null ? "" : robot.getTeam());
		this.name.setText(robot.getName() == null ? "" : robot.getName());
		this.login.setText(robot.getAuthors().isEmpty() ? "" : robot.getAuthors().get(0).getLogin());
		this.password.clear();
		this.password.setPromptText(isNew ? "Obrigatória" : "Nova (vazio mantém)");
		this.battleCry.setText(robot.getBattleCry() == null ? "" : robot.getBattleCry());
		this.battleCryCount.setText(this.battleCry.getText().trim().length() + "/" + Opponent.MAX_BATTLE_CRY_LENGTH);

		this.authors.show(robot.getAuthors());

		this.showFiles(robot);
		this.preview.show(robot);
		this.deleteRobot.setDisable(isNew);

		this.dirty = false;
		this.error.setText("");
		this.saved.setText("");
	}

	/** The picture, the music, the .jar and the source of the robot. */
	private void showFiles(Opponent robot) {
		this.logo.setImage(robot.getImage() == null ? null : new Image(new ByteArrayInputStream(robot.getImage())));
		this.musicStart.setText(String.valueOf(robot.getMusicStart()));
		this.music = robot.getMusic() == null ? null : Sound.fromBytes(robot.getMusic(), robot.getFullName());
		this.musicButton.setDisable(this.music == null);
		this.musicInfo.setText(robot.getMusic() == null ? "Sem música" : "Atual: " + robot.getMusic().length / 1024 + " KB");
		this.jarInfo.setText(robot.getCompiledRobot() == null ? "Sem arquivo .jar" : "Atual: " + robot.getCompiledRobot().length / 1024 + " KB");
		this.source.setText(robot.getSourceCode() == null ? "" : robot.getSourceCode());
	}

	/** Starts a new robot: an empty form, with no robot selected in the list. */
	private void startNew() {
		if (!this.confirmDiscard()) {
			return;
		}
		this.robots.getSelectionModel().clearSelection();
		this.show(new Opponent());
		this.team.requestFocus();
	}

	/** Remembers that the form has changes that were not saved. */
	private void markDirty() {
		if (this.current != null) {
			this.dirty = true;
		}
	}

	// ---------------------------------------------------------------- files

	/** Asks for a file, null when the organizer gives up. */
	private File pickFile(String caption, String description, String... extensions) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(caption);
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
		return chooser.showOpenDialog(GameWindow.getWindow());
	}

	/** The bytes of a file, null (with the reason shown) when it cannot be read. */
	private byte[] read(File file) {
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException ioException) {
			this.error.setText("Não foi possível ler " + file.getName() + ": " + ioException.getMessage());
			return null;
		}
	}

	/** Chooses a new logo, shown at once. */
	private void pickLogo() {
		File file = this.pickFile("Logotipo do time (até 640 x 480 pixels e 1 MB)", "Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif");
		byte[] bytes = file == null ? null : this.read(file);
		if (bytes != null) {
			this.pendingImage = bytes;
			this.logo.setImage(new Image(new ByteArrayInputStream(bytes)));
			this.markDirty();
		}
	}

	/** Chooses a new music, which can be played at once. */
	private void pickMusic() {
		File file = this.pickFile("Música tema do time (.mp3 até 1 MB)", "Música mp3", "*.mp3");
		byte[] bytes = file == null ? null : this.read(file);
		if (bytes != null) {
			this.stopMusic();
			this.pendingMusic = bytes;
			this.music = Sound.fromBytes(bytes, file.getName());
			this.musicButton.setDisable(this.music == null);
			this.musicInfo.setText("Nova: " + file.getName() + " (" + bytes.length / 1024 + " KB)");
			this.markDirty();
		}
	}

	/** Chooses a new .jar. When its name is the one of the packager, the team and the robot take its names. */
	private void pickJar() {
		File file = this.pickFile("Robô compilado (.jar do empacotador do Robocode)", "Arquivo jar", "*.jar");
		byte[] bytes = file == null ? null : this.read(file);
		if (bytes != null) {
			this.pendingJar = bytes;
			this.jarInfo.setText("Novo: " + file.getName() + " (" + bytes.length / 1024 + " KB)");
			Matcher matcher = JAR_FILE_NAME.matcher(file.getName());
			if (matcher.matches()) {
				this.team.setText(matcher.group(1));
				this.name.setText(matcher.group(2));
			}
			this.markDirty();
		}
	}

	/** Loads the text of a .java file into the source. */
	private void pickSource() {
		File file = this.pickFile("Código fonte do robô (.java até 1 MB)", "Código Java", "*.java");
		byte[] bytes = file == null ? null : this.read(file);
		if (bytes != null) {
			this.source.setText(new String(bytes, StandardCharsets.UTF_8));
		}
	}

	// -------------------------------------------------------------- actions

	/** What the form holds, or null (with the reason shown) when a number is not a number. */
	private RobotData collect() {
		double start;
		try {
			String typed = this.musicStart.getText().trim().replace(',', '.');
			start = typed.isEmpty() ? 0 : Double.parseDouble(typed);
		} catch (NumberFormatException numberFormatException) {
			this.error.setText("O início da música deve ser um número de segundos");
			return null;
		}
		return new RobotData(this.team.getText(), this.name.getText(), this.login.getText(), this.password.getText(), this.battleCry.getText(), start, this.source.getText(),
				this.pendingImage, this.pendingMusic, this.pendingJar, this.authors.data());
	}

	/** Saves the form: the robot is checked, added or changed, and shown again as it was saved. */
	private void save() {
		RobotData data = this.collect();
		if (data == null) {
			return;
		}
		Opponent robot;
		try {
			robot = OpponentService.save(this.current.getId(), data);
		} catch (IllegalArgumentException | IllegalStateException problem) {
			this.saved.setText("");
			this.error.setText(problem.getMessage());
			return;
		}
		this.afterChange(robot.getId(), "Robô salvo com sucesso!");
	}

	/** Removes the robot, after asking. */
	private void delete() {
		if (this.current == null || this.current.getId() == 0) {
			return;
		}
		if (!GameDialog.confirm("Excluir robô", "Excluir " + this.current.getFullName() + " e tudo o que a equipe enviou? Isso não pode ser desfeito.")) {
			return;
		}
		try {
			OpponentService.delete(this.current.getId());
		} catch (IllegalArgumentException | IllegalStateException problem) {
			this.saved.setText("");
			this.error.setText(problem.getMessage());
			return;
		}
		this.afterChange(-1, "Robô excluído.");
	}

	/** Tells the program, reads the list again and shows the robot (the first one if it is gone) with the message. */
	private void afterChange(long selectId, String message) {
		this.onSaved.run();
		this.dirty = false;
		this.loadRobots(selectId);
		this.error.setText("");
		this.saved.setText(message);
	}

	/** Goes back, after asking about the changes that were not saved. */
	private void goBack() {
		if (!this.confirmDiscard()) {
			return;
		}
		this.stop();
		this.onBack.run();
	}

	/** Plays the music of the robot, or stops it when it is playing. */
	private void toggleMusic() {
		if (this.music == null) {
			return;
		}
		if (this.playing) {
			this.stopMusic();
		} else {
			this.music.play();
			this.playing = true;
			this.musicButton.setText("■ Parar");
		}
	}

	/** Stops the music of the robot and shows the button to play it again. */
	private void stopMusic() {
		if (this.music != null) {
			this.music.stop();
		}
		this.playing = false;
		this.musicButton.setText("▶ Ouvir");
	}

	/** Stops the music and the turning of the robot, when the screen is left. */
	public void stop() {
		this.stopMusic();
		this.preview.stop();
	}
}

package widgets.scenes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Author;
import model.Opponent;
import tournament.OpponentService;
import tournament.OpponentService.AuthorData;
import widgets.ArcadeBackground;
import widgets.GameDialog;
import widgets.Sound;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The organizer's screen for the robots: the list of the registered robots, what each one has (its picture, its
 * battle cry, its authors, its music and its source code) and the texts that can be changed.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotsEditor extends StackPane {

	private final Runnable onSaved;
	private final Runnable onBack;

	private final ListView<Opponent> robots = new ListView<Opponent>();
	private final Label robotName = new Label();
	private final Label login = new Label();
	private final TextField battleCry = new TextField();
	private final Label battleCryCount = new Label();
	private final VBox authorBox = new VBox(8);
	private final List<AuthorRow> authorRows = new ArrayList<AuthorRow>();
	private final ImageView logo = new ImageView();
	private final Label jarInfo = new Label();
	private final Button musicButton = new Button("▶ Ouvir música");
	private final TextArea source = new TextArea();
	private final Label sourceInfo = new Label();
	private final Label error = new Label();
	private final Label saved = new Label();
	private final Button save = new Button("Salvar");

	private Opponent current;
	private Sound music;
	private boolean playing;
	private boolean dirty;

	/**
	 * @param onSaved
	 *            runs after a robot is saved, so the rest of the program reads the new texts
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

		this.getChildren().setAll(new ArcadeBackground(), this.buildLayout(), scanlines);
		this.loadRobots();
	}

	/** The title with the buttons, the list of robots at the left and the robot at the right. */
	private BorderPane buildLayout() {
		Label title = new Label("EQUIPES E ROBÔS");
		title.getStyleClass().add("game-title");
		Button back = new Button("Voltar");
		back.setOnAction(event -> this.goBack());
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(16, title, spacer, back);
		top.setAlignment(Pos.CENTER_LEFT);
		top.setPadding(new Insets(20, 30, 10, 30));

		VBox list = this.buildList();

		Button addAuthor = this.configureControls();

		HBox files = new HBox(16, this.logo, new VBox(8, this.label("Logotipo", "form-label"), this.jarInfo, this.musicButton));
		files.setAlignment(Pos.CENTER_LEFT);

		HBox body = new HBox(24, list, this.buildForm(files, addAuthor));
		body.setPadding(new Insets(10, 30, 30, 30));

		BorderPane layout = new BorderPane(body);
		layout.setTop(top);
		return layout;
	}

	/**
	 * What the controls do and how they look
	 *
	 * @return the button that adds an author
	 */
	private Button configureControls() {
		this.battleCry.textProperty().addListener((observable, before, after) -> {
			this.battleCryCount.setText(after.trim().length() + "/" + Opponent.MAX_BATTLE_CRY_LENGTH);
			this.markDirty();
		});
		this.battleCry.setPromptText("Lema do time");

		Button addAuthor = new Button("+ Adicionar pessoa");
		addAuthor.setOnAction(event -> this.addAuthor(new Author()));

		this.logo.setPreserveRatio(true);
		this.logo.setFitWidth(120);
		this.logo.setSmooth(true);
		this.musicButton.setOnAction(event -> this.toggleMusic());
		this.source.setEditable(false);
		this.source.setWrapText(false);
		this.source.setPrefRowCount(12);
		this.source.getStyleClass().add("text-area");

		this.save.getStyleClass().add("primary");
		this.save.setOnAction(event -> this.save());
		this.error.getStyleClass().add("error-text");
		this.saved.getStyleClass().add("form-hint");
		return addAuthor;
	}

	/** The list of the robots at the left, with the name of each robot. */
	private VBox buildList() {
		this.robots.setPrefWidth(280);
		this.robots.setCellFactory(list -> new ListCell<Opponent>() {
			@Override
			protected void updateItem(Opponent robot, boolean empty) {
				super.updateItem(robot, empty);
				setText(empty || robot == null ? null : robot.getFullName());
			}
		});
		this.robots.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> this.selectionChanged(previous, selected));
		VBox list = new VBox(10, this.label("Robôs cadastrados", "panel-title"), this.robots);
		VBox.setVgrow(this.robots, Priority.ALWAYS);
		return list;
	}

	/** The robot at the right: its texts, its authors, its files and its source, scrolling when it does not fit. */
	private ScrollPane buildForm(HBox files, Button addAuthor) {
		VBox form = new VBox(12,
				this.robotName,
				this.label("Login do time: ", "form-hint", this.login),
				this.label("Lema do time", "form-label"), this.battleCry, this.battleCryCount,
				this.label("Pessoas desenvolvedoras: nome, RM (apenas números) e e-mail", "form-label"), this.authorBox, addAuthor,
				this.label("Arquivos", "form-label"), files,
				this.label("Código fonte (somente leitura)", "form-label"), this.sourceInfo, this.source,
				this.error, this.saved, this.save);
		form.setPadding(new Insets(0, 12, 20, 0));
		this.robotName.getStyleClass().add("showcase-name");
		this.jarInfo.getStyleClass().add("form-hint");
		this.sourceInfo.getStyleClass().add("form-hint");

		ScrollPane scroll = new ScrollPane(form);
		scroll.setFitToWidth(true);
		scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
		HBox.setHgrow(scroll, Priority.ALWAYS);
		return scroll;
	}

	/** A label with the given style class. */
	private Label label(String text, String styleClass) {
		Label label = new Label(text);
		label.getStyleClass().add(styleClass);
		return label;
	}

	/** A label with the given style class followed by another label. */
	private HBox label(String text, String styleClass, Label value) {
		value.getStyleClass().add(styleClass);
		return new HBox(this.label(text, styleClass), value);
	}

	/** Shows the robots of the database and the first one. */
	private void loadRobots() {
		List<Opponent> all = OpponentService.all();
		this.robots.getItems().setAll(all);
		if (all.isEmpty()) {
			this.robotName.setText("Nenhum robô cadastrado");
			this.setFormDisabled(true);
			return;
		}
		this.robots.getSelectionModel().select(0);
	}

	/** Asks before leaving a robot with changes that were not saved, then shows the selected one. */
	private void selectionChanged(Opponent previous, Opponent selected) {
		if (selected == null) {
			return;
		}
		if (this.dirty && !GameDialog.confirm("Descartar alterações", "Há alterações que não foram salvas. Descartá-las?")) {
			// Back to the robot being edited, after the list finishes the change
			Platform.runLater(() -> this.robots.getSelectionModel().select(previous));
			return;
		}
		this.show(selected);
	}

	/** Fills the form with a robot: its texts, its authors, its files, which the organizer can see but not change. */
	private void show(Opponent robot) {
		this.stopMusic();
		this.current = robot;
		this.setFormDisabled(false);

		this.robotName.setText("Robô " + robot.getFullName());
		this.login.setText(robot.getAuthors().isEmpty() ? "-" : robot.getAuthors().get(0).getLogin());
		this.battleCry.setText(robot.getBattleCry() == null ? "" : robot.getBattleCry());
		this.battleCryCount.setText(this.battleCry.getText().trim().length() + "/" + Opponent.MAX_BATTLE_CRY_LENGTH);

		this.authorRows.clear();
		this.authorBox.getChildren().clear();
		for (Author author : robot.getAuthors()) {
			this.addAuthor(author);
		}
		if (robot.getAuthors().isEmpty()) {
			this.addAuthor(new Author());
		}

		this.logo.setImage(robot.getImage() == null ? null : new Image(new ByteArrayInputStream(robot.getImage())));
		this.jarInfo.setText(robot.getCompiledRobot() == null ? "Sem arquivo .jar" : "Arquivo .jar: " + robot.getCompiledRobot().length / 1024 + " KB");

		String code = robot.getSourceCode() == null ? "" : robot.getSourceCode();
		this.source.setText(code);
		this.sourceInfo.setText(code.isEmpty() ? "Sem código fonte" : code.lines().count() + " linhas");

		this.music = robot.getMusic() == null ? null : Sound.fromBytes(robot.getMusic(), robot.getFullName());
		this.musicButton.setDisable(this.music == null);
		this.musicButton.setText("▶ Ouvir música");

		this.dirty = false;
		this.error.setText("");
		this.saved.setText("");
	}

	/** Adds a row for an author, with its name, its registration and its mail. */
	private void addAuthor(Author author) {
		AuthorRow row = new AuthorRow(author);
		this.authorRows.add(row);
		this.authorBox.getChildren().add(row.box);
		this.updateRemoveButtons();
		this.markDirty();
	}

	/** A robot needs at least one author, so the last row cannot be removed. */
	private void updateRemoveButtons() {
		for (AuthorRow row : this.authorRows) {
			row.remove.setDisable(this.authorRows.size() <= 1);
		}
	}

	/** Enables or disables the editing of the form, when there is no robot to show. */
	private void setFormDisabled(boolean disabled) {
		this.battleCry.setDisable(disabled);
		this.authorBox.setDisable(disabled);
		this.save.setDisable(disabled);
		this.musicButton.setDisable(disabled || this.music == null);
	}

	/** Remembers that the form has changes that were not saved. */
	private void markDirty() {
		if (this.current != null) {
			this.dirty = true;
		}
	}

	/** Saves the form: the robot is checked, saved and shown again as it was saved. */
	private void save() {
		List<AuthorData> authors = new ArrayList<AuthorData>();
		for (AuthorRow row : this.authorRows) {
			authors.add(new AuthorData(row.id, row.name.getText(), row.registration.getText(), row.mail.getText()));
		}
		Opponent robot;
		try {
			robot = OpponentService.update(this.current.getId(), this.battleCry.getText(), authors);
		} catch (IllegalArgumentException | IllegalStateException problem) {
			this.saved.setText("");
			this.error.setText(problem.getMessage());
			return;
		}

		this.onSaved.run();
		// The list is read again, and the saved robot is selected: its form shows what was saved
		this.dirty = false;
		this.robots.getItems().setAll(OpponentService.all());
		for (Opponent item : this.robots.getItems()) {
			if (item.getId() == robot.getId()) {
				this.robots.getSelectionModel().select(item);
			}
		}
		this.error.setText("");
		this.saved.setText("Robô salvo com sucesso!");
	}

	/** Goes back, after asking about the changes that were not saved. */
	private void goBack() {
		if (this.dirty && !GameDialog.confirm("Descartar alterações", "Há alterações que não foram salvas. Descartá-las?")) {
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
			this.musicButton.setText("■ Parar música");
		}
	}

	/** Stops the music of the robot and shows the button to play it again. */
	private void stopMusic() {
		if (this.music != null) {
			this.music.stop();
		}
		this.playing = false;
		this.musicButton.setText("▶ Ouvir música");
	}

	/** Stops the music, when the screen is left. */
	public void stop() {
		this.stopMusic();
	}

	/** One author of the form: the fields of the author and the button that removes it. */
	private final class AuthorRow {

		/** The id of the author, 0 for an author added now */
		final long id;
		final TextField name = new TextField();
		final TextField registration = new TextField();
		final TextField mail = new TextField();
		final Button remove = new Button("Remover");
		final HBox box;

		AuthorRow(Author author) {
			this.id = author.getId();
			this.name.setText(author.getName() == null ? "" : author.getName());
			this.name.setPromptText("Nome");
			this.registration.setText(author.getRegistration());
			this.registration.setPromptText("RM");
			this.registration.setPrefWidth(110);
			this.mail.setText(author.getMail() == null ? "" : author.getMail());
			this.mail.setPromptText("E-mail");
			HBox.setHgrow(this.name, Priority.ALWAYS);
			HBox.setHgrow(this.mail, Priority.ALWAYS);

			this.box = new HBox(8, this.name, this.registration, this.mail, this.remove);
			this.box.setAlignment(Pos.CENTER_LEFT);

			this.name.textProperty().addListener((observable, before, after) -> RobotsEditor.this.markDirty());
			this.registration.textProperty().addListener((observable, before, after) -> RobotsEditor.this.markDirty());
			this.mail.textProperty().addListener((observable, before, after) -> RobotsEditor.this.markDirty());
			this.remove.setOnAction(event -> {
				RobotsEditor.this.authorRows.remove(this);
				RobotsEditor.this.authorBox.getChildren().remove(this.box);
				RobotsEditor.this.updateRemoveButtons();
				RobotsEditor.this.markDirty();
			});
		}
	}
}

package widgets.scenes;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Opponent;
import widgets.AuthorsTable;
import widgets.MediaControl;
import widgets.OpponentsTable;
import widgets.windows.AddAuthor;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class EditRobots extends Scene {

	private TextField name;
	private TextField team;
	private TextField level;
	private TextField warcry;
	private ImageView image;
	private URI imagePath;
	private TextArea sourceCode;
	private TextField compiledRobot;

	private MediaControl mediaControl;

	private Button save;
	private Button addImage;

	private Button addAuthor;

	private FileChooser chooser;
	private AuthorsTable authorsTable;
	private OpponentsTable opponentsTable;

	private VBox vbox;
	private HBox hbox;

	private ObservableList<Opponent> opponentsList;

	public EditRobots(ObservableList<Opponent> opponentsList) {
		super(new HBox());

		this.opponentsList = opponentsList;

		this.mediaControl = new MediaControl(null);
		this.chooser = new FileChooser();
		this.chooser.setTitle("Escolha um arquivo");

		this.vbox = new VBox(10);
		this.vbox.minWidthProperty().bind(this.widthProperty().divide(2));

		this.hbox = new HBox(10);
		this.hbox.minWidthProperty().bind(this.widthProperty());

		this.addAuthor = new Button("+ Autor");
		this.addAuthor.setOnMouseClicked(event -> {
			new AddAuthor(this.authorsTable.getItems());
		});

		this.save = new Button("Salvar");
		this.save.setOnMouseClicked(event -> {
			try {
				Opponent robo = new Opponent(this.name.getText(), this.imagePath);
				robo.setLevel(Integer.parseInt(this.level.getText()));
				robo.setTeam(this.team.getText());
				robo.setAuthors(authorsTable.getItems());
				robo.setBattleCry(this.warcry.getText());
				robo.setSourceCode(this.sourceCode.getText());
				robo.setCompiledRobot(new URI(compiledRobot.getText()));
				robo.setMusic(this.mediaControl.getSelectedFile().toURI());
				robo.setMusicStart(this.mediaControl.getCurrentTime());

				EditRobots.this.opponentsList.add(robo);

				this.authorsTable.getItems().clear();
				this.name.clear();
				this.team.clear();
				this.level.clear();
				this.warcry.clear();
				this.image.setImage(null);
				this.sourceCode.clear();
				this.compiledRobot.clear();
				this.mediaControl.reset();

			} catch (Exception e) {
				e.printStackTrace();
				new Alert(AlertType.ERROR, "Falha ao adicionar o robô").showAndWait();
			}

		});

		this.addImage = new Button("Adicionar imagem");
		this.addImage.setOnMouseClicked(event -> {
			File lodedImage = chooser.showOpenDialog(null);

			if (lodedImage.exists()) try {
				image.setImage(new Image(lodedImage.toURI().toURL().toString()));
				imagePath = lodedImage.toURI();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		this.image = new ImageView();
		this.image.setPreserveRatio(true);
		this.image.fitWidthProperty().set(200);
		this.image.fitHeightProperty().set(200);

		this.name = new TextField();
		this.name.setEditable(false);
		this.name.setPromptText("Nome do robô");

		this.team = new TextField();
		this.team.setEditable(false);
		this.team.setPromptText("Nome do time");

		this.warcry = new TextField();
		this.warcry.setPromptText("Lema");

		this.level = new TextField();
		this.level.setPromptText("Nivel");

		this.sourceCode = new TextArea();
		this.sourceCode.setWrapText(false);
		this.sourceCode.setPrefHeight(100);
		this.sourceCode.setMinWidth(320);
		this.sourceCode.setPromptText("Clique para escolher o código fonte");
		this.sourceCode.setOnMouseClicked(event -> {
			File source = chooser.showOpenDialog(null);

			if (source.exists()) try {
				FileInputStream fis = new FileInputStream(source);
				byte[] bytes = new byte[(int) source.length()];
				fis.read(bytes, 0, (int) source.length());
				String s = new String(bytes);
				fis.close();
				sourceCode.setText(s);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		this.compiledRobot = new TextField();
		this.compiledRobot.setPromptText("Clique para escolher o robô compilado");
		this.compiledRobot.setOnMouseClicked(event -> {
			File robot = chooser.showOpenDialog(null);

			if (robot.exists()) try {
				this.compiledRobot.setText(robot.toURI().toURL().toString());
				this.team.setText(robot.getName().split("\\.")[0]);
				this.name.setText(robot.getName().split("\\.")[1].split("_")[0]);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		this.authorsTable = new AuthorsTable();
		this.authorsTable.setPrefHeight(150);

		this.opponentsTable = new OpponentsTable(opponentsList);
		this.opponentsTable.prefWidthProperty().bind(this.widthProperty().divide(2));

		this.vbox.getChildren().addAll(compiledRobot, name, team, warcry, level, sourceCode, mediaControl, authorsTable, addAuthor, image, addImage, save);

		this.hbox.getChildren().addAll(this.vbox, this.opponentsTable);
		this.hbox.prefHeightProperty().bind(this.heightProperty());

		this.setRoot(this.hbox);
	}

}

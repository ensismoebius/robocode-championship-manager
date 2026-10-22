package widgets.scenes;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Author;
import tournament.OpponentService.AuthorData;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The authors of a robot as rows of the form: the name, the registration and the mail of each one, and the
 * button that removes it. A robot always keeps at least one.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class AuthorRows extends VBox {

	private final List<Row> rows = new ArrayList<Row>();
	private final Runnable onChange;

	/**
	 * @param onChange
	 *            runs when something in the rows changes, so the form knows it has changes to save
	 */
	AuthorRows(Runnable onChange) {
		super(8);
		this.onChange = onChange;
	}

	/** Shows a row for each author, or an empty row when there is none. */
	void show(List<Author> authors) {
		this.rows.clear();
		this.getChildren().clear();
		for (Author author : authors) {
			this.add(author);
		}
		if (authors.isEmpty()) {
			this.add(new Author());
		}
	}

	/** Adds a row for an author. */
	void add(Author author) {
		Row row = new Row(author);
		this.rows.add(row);
		this.getChildren().add(row.box);
		this.updateRemoveButtons();
		this.onChange.run();
	}

	/** What the rows hold, with the id of the authors that already exist. */
	List<AuthorData> data() {
		List<AuthorData> authors = new ArrayList<AuthorData>();
		for (Row row : this.rows) {
			authors.add(new AuthorData(row.id, row.name.getText(), row.registration.getText(), row.mail.getText()));
		}
		return authors;
	}

	/** A robot needs at least one author, so the last row cannot be removed. */
	private void updateRemoveButtons() {
		for (Row row : this.rows) {
			row.remove.setDisable(this.rows.size() <= 1);
		}
	}

	/** One author of the form: the fields of the author and the button that removes it. */
	private final class Row {

		/** The id of the author, 0 for an author added now */
		final long id;
		final TextField name = new TextField();
		final TextField registration = new TextField();
		final TextField mail = new TextField();
		final Button remove = new Button("Remover");
		final HBox box;

		Row(Author author) {
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

			this.name.textProperty().addListener((observable, before, after) -> AuthorRows.this.onChange.run());
			this.registration.textProperty().addListener((observable, before, after) -> AuthorRows.this.onChange.run());
			this.mail.textProperty().addListener((observable, before, after) -> AuthorRows.this.onChange.run());
			this.remove.setOnAction(event -> {
				AuthorRows.this.rows.remove(this);
				AuthorRows.this.getChildren().remove(this.box);
				AuthorRows.this.updateRemoveButtons();
				AuthorRows.this.onChange.run();
			});
		}
	}
}

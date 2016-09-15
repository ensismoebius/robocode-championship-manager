package widgets;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import model.Author;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class AuthorsTable extends TableView<Author> {

	private TableColumn<Author, String> name;
	private TableColumn<Author, String> observation;

	@SuppressWarnings("unchecked")
	public AuthorsTable() {

		this.setEditable(false);

		this.name = new TableColumn<Author, String>("Nome");
		this.observation = new TableColumn<Author, String>("Observação");

		this.getColumns().addAll(this.name, this.observation);

		this.name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Author, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Author, String> arg0) {
				return new SimpleStringProperty(arg0.getValue().getName());
			}
		});

		this.observation.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Author, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<Author, String> arg0) {
				return new SimpleStringProperty(arg0.getValue().getObservations());
			}
		});

	}
}
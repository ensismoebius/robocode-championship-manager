package widgets;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import model.Opponent;
/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class OpponentsTable extends TableView<Opponent> {

	private TableColumn<Opponent, String> name;
	private TableColumn<Opponent, String> battleCry;

	@SuppressWarnings("unchecked")
	public OpponentsTable(ObservableMap<String,Opponent> opponentsList2) {

		this.setEditable(false);
		this.setItems(opponentsList2);

		this.name = new TableColumn<Opponent, String>("Nome");
		this.battleCry = new TableColumn<Opponent, String>("Lema");

		this.getColumns().addAll(this.name, this.battleCry);

		this.name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Opponent, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Opponent, String> arg0) {
				return new SimpleStringProperty(arg0.getValue().getName());
			}
		});

		this.battleCry.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Opponent, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Opponent, String> arg0) {
				return new SimpleStringProperty(arg0.getValue().getBattleCry());
			}
		});

	}
}
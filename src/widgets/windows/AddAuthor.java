package widgets.windows;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Author;

public class AddAuthor extends Stage {

	private TextField name;
	private TextField observation;
	private Button addButton;

	public AddAuthor(ObservableList<Author> list) {
		this.name = new TextField();
		this.name.setPromptText("Nome");

		this.observation = new TextField();
		this.observation.setPromptText("Obs");

		this.addButton = new Button("Salvar");
		this.addButton.setOnMouseClicked(event -> {

			Author a = new Author(); 
			a.setName(this.name.getText());
			a.setObservations(this.observation.getText());

			list.add(a);

		});
		
		VBox vb = new VBox(10);
		
		vb.getChildren().addAll(name, observation, addButton);
		
		this.setScene(new Scene(vb, 320, 240));
		this.show();
		
		
	}

}

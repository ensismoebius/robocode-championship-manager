package widgets;

import com.sun.xml.messaging.saaj.util.ByteInputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotBasicViewer extends HBox {

	private boolean marked = false;
	private ImageView image;

	private int maximumWidth = 300;
	private Label name;

	public RobotBasicViewer(model.Opponent oponent) {
		super(10);
		
		this.name = new Label(oponent.getName());

		this.image = new ImageView();
		this.image.setImage(new Image(new ByteInputStream(oponent.getImage(), oponent.getImage().length)));
		this.image.setPreserveRatio(true);

		this.name.setAlignment(Pos.BASELINE_CENTER);
		HBox.setHgrow(name, Priority.ALWAYS);
		HBox.setMargin(name, new Insets(10));
		
		this.getChildren().add(this.name);
		this.getChildren().add(this.image);

		this.image.setFitHeight(this.maximumWidth);
		this.image.setFitWidth(this.maximumWidth);

		this.applyNonSelectedStyle();

		this.setOnTouchPressed(event -> {
			markAsSelected();
		});
		this.setOnMouseClicked(event -> {
			markAsSelected();
		});
	}

	private void applySelectedStyle() {
		
		this.setStyle(""
				+ "-fx-border-color: #00ff00;"
				+ "-fx-border-width: 8px;"
				+ "-fx-font-size: 2em;"
				+ "-fx-background-color: #000;");
		
		this.name.setTextFill(Color.YELLOWGREEN);
		this.name.setTextAlignment(TextAlignment.RIGHT);
		this.name.setRotate(90);
	}

	private void applyNonSelectedStyle(){
		this.setStyle(""
				+ "-fx-background-color: #000;"
				+ "-fx-font-size: 2em;"
				+ "-fx-background-color: #000;");
		
		this.name.setTextFill(Color.YELLOWGREEN);
		this.name.setTextAlignment(TextAlignment.RIGHT);
		this.name.setRotate(90);
		
	}

	public int getMaximumWidth() {
		return maximumWidth;
	}

	public void setMaximumWidth(int maximumWidth) {
		this.maximumWidth = maximumWidth;
	}

	/**
	 * 
	 */
	private void markAsSelected() {
		if (this.marked) {
			this.image.scaleXProperty().set(1.0);
			this.image.scaleYProperty().set(1.0);
			this.applyNonSelectedStyle();
			this.marked = false;
			return;
		}
		this.image.scaleXProperty().set(1.0);
		this.image.scaleYProperty().set(1.0);
		this.applySelectedStyle();
		this.marked = true;
	}

}

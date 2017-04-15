package widgets;

import com.sun.xml.messaging.saaj.util.ByteInputStream;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import model.Opponent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class RobotBasicViewer extends HBox {

	private boolean marked = false;
	private ImageView image;

	private int desireWidth = 300;
	private Label name;

	private IRobotMarkedEvent markedEvent;
	private Opponent oponent;

	public RobotBasicViewer(model.Opponent oponent) {
		super(10);

		this.oponent = oponent;

		this.setMinHeight(this.desireWidth);

		this.name = new Label(oponent.getName());
		this.setAlignment(Pos.CENTER);
		this.name.setTextFill(Color.YELLOWGREEN);
		this.name.setTextAlignment(TextAlignment.RIGHT);
		this.name.setRotate(90);

		this.image = new ImageView();
		this.image.setImage(new Image(new ByteInputStream(oponent.getImage(), oponent.getImage().length)));
		this.image.setPreserveRatio(true);

		this.getChildren().add(this.name);
		this.getChildren().add(this.image);

		this.image.setFitHeight(this.desireWidth);
		this.image.setFitWidth(this.desireWidth);

		this.applyNonSelectedStyle();

		this.setOnTouchPressed(event -> {
			mark();
		});
		this.setOnMouseClicked(event -> {
			mark();
		});
	}

	public void setRobotMarkedEventListener(IRobotMarkedEvent markedEvent) {
		this.markedEvent = markedEvent;
	}

	private void applySelectedStyle() {

		this.setStyle(
				"-fx-border-color: #00ff00; -fx-border-width: 8px; -fx-font-size: 2em; -fx-background-color: #000;");
	}

	private void applyNonSelectedStyle() {
		this.setStyle("" + "-fx-font-size: 2em;" + "-fx-background-color: #000;");
	}

	public int getMaximumWidth() {
		return desireWidth;
	}

	public void setMaximumWidth(int maximumWidth) {
		this.desireWidth = maximumWidth;
	}

	public DoubleBinding xCoords() {
		return this.layoutXProperty().add(this.widthProperty().divide(2));
	}

	public DoubleBinding yCoords() {
		return this.layoutYProperty().add(this.heightProperty().divide(2));
	}

	/**
	 * 
	 */
	private void mark() {

		if (this.marked) {
			this.image.scaleXProperty().set(1.0);
			this.image.scaleYProperty().set(1.0);
			this.applyNonSelectedStyle();
			this.markedEvent.robotMarked(false, this.oponent);
			this.marked = false;
			return;
		}
		this.image.scaleXProperty().set(1.0);
		this.image.scaleYProperty().set(1.0);
		this.applySelectedStyle();
		this.markedEvent.robotMarked(true, this.oponent);
		this.marked = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.scene.Node#toString()
	 */
	@Override
	public String toString() {
		return this.name.getText() + " - " + this.oponent.getLevels().toString();
	}

}

package widgets.scenes;

import java.io.ByteArrayInputStream;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import model.Opponent;

/**
 * The pieces of the podium: the picture of a robot, and the big pixel letters of a title
 */
final class PodiumPieces {

	/**
	 * The robot picture, a fit of the height of the screen
	 */
	static ImageView image(Region host, Opponent opponent, double heightFactor, double widthFactor, boolean reflection) {
		ImageView view = new ImageView();
		view.setSmooth(true);
		view.setPreserveRatio(true);
		view.fitHeightProperty().bind(host.heightProperty().multiply(heightFactor));
		view.fitWidthProperty().bind(host.widthProperty().multiply(widthFactor));
		if (reflection) {
			view.setEffect(new Reflection(0, 0.3, 0.35, 0));
		}
		if (opponent != null && opponent.getImage() != null) {
			view.setImage(new Image(new ByteArrayInputStream(opponent.getImage())));
		}
		return view;
	}

	/**
	 * Big pixel letters. The font has no tilde on the A, so it is drawn over it.
	 *
	 * @param heightFactor
	 *            the size of the letters, from the height of the screen
	 */
	static Node title(Region host, String text, double heightFactor) {
		HBox letters = new HBox();
		letters.setAlignment(Pos.CENTER);
		for (char column : text.toCharArray()) {
			Label letter = new Label(String.valueOf(column == 'Ã' ? 'A' : column));
			letter.getStyleClass().add("podium-title");
			letter.styleProperty().bind(host.heightProperty().multiply(heightFactor).asString("-fx-font-size: %.0fpx;"));
			if (column == 'Ã') {
				Label tilde = new Label("~");
				tilde.getStyleClass().add("podium-title");
				tilde.styleProperty().bind(host.heightProperty().multiply(heightFactor * 0.8).asString("-fx-font-size: %.0fpx;"));
				tilde.translateYProperty().bind(host.heightProperty().multiply(-heightFactor * 0.62));
				tilde.setMouseTransparent(true);
				StackPane both = new StackPane(letter, tilde);
				both.setAlignment(Pos.CENTER);
				letters.getChildren().add(both);
			} else {
				letters.getChildren().add(letter);
			}
		}
		letters.setMouseTransparent(true);
		return letters;
	}
}

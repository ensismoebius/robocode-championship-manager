package widgets.scenes;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Opponent;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * The podium on the screen: three steps, 2 1 3, with the robots that were revealed standing on them,
 * a spotlight on each and the place at the top. It is part of the {@link Podium} screen.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class PodiumStage extends Pane {

	private final Podium host;

	private final Rectangle[] steps = new Rectangle[4];
	private final Label[] numbers = new Label[4];
	private final VBox[] figures = new VBox[4];
	private final ImageView[] images = new ImageView[4];
	private final Polygon[] spots = new Polygon[4];
	private final Label banner = new Label();
	private final Group[] trophies = new Group[4];
	/** How big each trophy is drawn, for the height of the screen: the gold one the biggest */
	private final double[] trophyScales = { 0, 0.00068, 0.00056, 0.00048 };

	PodiumStage(Podium host) {
		this.host = host;
		this.setMouseTransparent(true);
		Color[] colors = { null, Podium.GOLD, Podium.SILVER, Podium.BRONZE };
		for (int place = 1; place <= 3; place++) {
			Color color = colors[place];
			Polygon spot = new Polygon();
			spot.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.35)),
					new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.03))));
			spot.setBlendMode(BlendMode.ADD);
			spot.setOpacity(0);
			this.spots[place] = spot;

			Rectangle step = new Rectangle();
			step.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, color.interpolate(Color.WHITE, 0.25)), new Stop(1, color.interpolate(Color.BLACK, 0.55))));
			step.setStroke(color.interpolate(Color.WHITE, 0.5));
			step.setStrokeWidth(3);
			step.setEffect(new DropShadow(26, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.55)));
			this.steps[place] = step;

			Label number = new Label(String.valueOf(place));
			number.getStyleClass().add("podium-number");
			number.setTextFill(color.interpolate(Color.BLACK, 0.55));
			number.styleProperty().bind(host.heightProperty().multiply(place == 1 ? 0.14 : place == 2 ? 0.1 : 0.075).asString("-fx-font-size: %.0fpx;"));
			this.numbers[place] = number;

			Opponent opponent = host.places[place];
			if (opponent != null) {
				ImageView image = host.image(opponent, place == 1 ? 0.31 : place == 2 ? 0.27 : 0.24, Podium.COLUMN_WIDTH, false);
				image.setEffect(new DropShadow(30, color));
				this.images[place] = image;
				Label name = new Label(opponent.getName().toUpperCase());
				name.getStyleClass().add("podium-name");
				name.setWrapText(true);
				name.setAlignment(Pos.CENTER);
				name.setTextFill(Color.WHITE);
				// The letters shrink to fit the column: a long name never reaches the next one
				double heightFactor = place == 1 ? 0.038 : 0.032;
				name.styleProperty().bind(Bindings.createStringBinding(() -> String.format("-fx-font-size: %.0fpx;", Math.max(8, Math.min(host.getHeight() * heightFactor, host.getWidth() * Podium.COLUMN_WIDTH / Math.max(1, opponent.getName().length() * 0.9)))), host.heightProperty(), host.widthProperty()));
				String teamText = "TIME " + opponent.getTeam().toUpperCase();
				Label team = new Label(teamText);
				team.getStyleClass().add("podium-team");
				team.setAlignment(Pos.CENTER);
				team.styleProperty().bind(Bindings.createStringBinding(() -> String.format("-fx-font-size: %.0fpx;", Math.max(8, Math.min(host.getHeight() * 0.026, host.getWidth() * Podium.COLUMN_WIDTH / Math.max(1, teamText.length() * 0.9)))), host.heightProperty(), host.widthProperty()));
				VBox figure = new VBox(4, name, team, image);
				// The whole figure, names and picture, fits its column
				figure.prefWidthProperty().bind(host.widthProperty().multiply(Podium.COLUMN_WIDTH));
				figure.minWidthProperty().bind(figure.prefWidthProperty());
				figure.maxWidthProperty().bind(figure.prefWidthProperty());
				figure.setAlignment(Pos.BOTTOM_CENTER);
				figure.setVisible(false);
				this.figures[place] = figure;
			}
		}
		// A trophy for everyone on the podium: gold, silver and bronze
		for (int place = 1; place <= 3; place++) {
			if (this.figures[place] == null) {
				continue;
			}
			Group trophy = Podium.trophy(colors[place]);
			trophy.scaleXProperty().bind(host.heightProperty().multiply(this.trophyScales[place]));
			trophy.scaleYProperty().bind(host.heightProperty().multiply(this.trophyScales[place]));
			trophy.setVisible(false);
			this.trophies[place] = trophy;
		}

		this.banner.getStyleClass().add("podium-banner");
		this.banner.setVisible(false);
		this.banner.styleProperty().bind(host.heightProperty().multiply(0.07).asString("-fx-font-size: %.0fpx;"));

		for (int place = 1; place <= 3; place++) {
			this.getChildren().add(this.spots[place]);
		}
		for (int place = 1; place <= 3; place++) {
			this.getChildren().addAll(this.steps[place], this.numbers[place]);
		}
		// The trophies behind the names: drawn first, the robots and their names over them
		for (int place = 1; place <= 3; place++) {
			if (this.trophies[place] != null) {
				this.getChildren().add(this.trophies[place]);
			}
		}
		for (int place = 1; place <= 3; place++) {
			if (this.figures[place] != null) {
				this.getChildren().add(this.figures[place]);
			}
		}
		this.getChildren().add(this.banner);
	}

	/** Where the center of the column of the place is, from 0 to 1 */
	double columnCenter(int place) {
		return place == 1 ? 0.5 : place == 2 ? 0.5 - Podium.COLUMN_SPACING : 0.5 + Podium.COLUMN_SPACING;
	}

	private double stepHeight(int place) {
		return this.getHeight() * (place == 1 ? 0.26 : place == 2 ? 0.19 : 0.13);
	}

	@Override
	protected void layoutChildren() {
		double width = this.getWidth();
		double height = this.getHeight();
		double baseline = height * 0.9;
		double stepWidth = width * Podium.COLUMN_WIDTH;
		for (int place = 1; place <= 3; place++) {
			double cx = this.columnCenter(place) * width;
			double top = baseline - this.stepHeight(place);
			this.steps[place].setX(cx - stepWidth / 2);
			this.steps[place].setY(top);
			this.steps[place].setWidth(stepWidth);
			this.steps[place].setHeight(this.stepHeight(place));
			Label number = this.numbers[place];
			number.autosize();
			number.relocate(cx - number.getWidth() / 2, top + (this.stepHeight(place) - number.getHeight()) / 2);
			this.spots[place].getPoints().setAll(cx - stepWidth * 0.1, 0.0, cx + stepWidth * 0.1, 0.0, cx + stepWidth * 0.75, top, cx - stepWidth * 0.75, top);
			VBox figure = this.figures[place];
			if (figure != null) {
				figure.autosize();
				figure.relocate(cx - figure.getWidth() / 2, top - figure.getHeight());
			}
		}
		for (int place = 1; place <= 3; place++) {
			Group trophy = this.trophies[place];
			if (trophy == null) {
				continue;
			}
			double top = baseline - this.stepHeight(place);
			double figureHeight = this.figures[place].getHeight();
			// The trophy is scaled around its center (160 wide and 95 down to the middle before
			// the scale): it stands over the name of its place
			double half = 95 * this.trophyScales[place] * height;
			trophy.relocate(this.columnCenter(place) * width - 80, top - figureHeight - half - height * 0.04 - 95);
		}
		this.banner.autosize();
		this.banner.relocate((width - this.banner.getWidth()) / 2, height * 0.01);
	}

	/**
	 * The place at the top, in its color
	 */
	void setBanner(int place) {
		Color color = place == 1 ? Podium.GOLD : place == 2 ? Podium.SILVER : Podium.BRONZE;
		this.banner.setText(place + "º LUGAR");
		this.banner.setTextFill(color);
		DropShadow glow = new DropShadow(30, color);
		glow.setSpread(0.3);
		this.banner.setEffect(glow);
		this.banner.setVisible(true);
		this.banner.setOpacity(1);
		this.layout();
	}

	void show(int place) {
		this.setBanner(place);
		host.animate(Podium.slam(this.banner, 0, 550)).play();
		Color color = place == 1 ? Podium.GOLD : place == 2 ? Podium.SILVER : Podium.BRONZE;
		this.spots[place].setOpacity(0);
		host.animate(new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(this.spots[place].opacityProperty(), 1)))).play();
		VBox figure = this.figures[place];
		if (figure != null) {
			figure.setVisible(true);
			figure.setOpacity(0);
			figure.setTranslateY(-this.getHeight());
			// Falls on the step with a bounce
			TranslateTransition fall = new TranslateTransition(Duration.millis(950), figure);
			fall.setToY(0);
			fall.setInterpolator(Podium.BOUNCE);
			FadeTransition fade = new FadeTransition(Duration.millis(150), figure);
			fade.setToValue(1);
			host.animate(new ParallelTransition(fall, fade)).play();
			if (this.images[place] != null) {
				DropShadow glow = new DropShadow(46, color);
				this.images[place].setEffect(glow);
				Timeline pulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 20)), new KeyFrame(Duration.millis(700), new KeyValue(glow.radiusProperty(), 60)));
				pulse.setAutoReverse(true);
				pulse.setCycleCount(Animation.INDEFINITE);
				host.animate(pulse).play();
			}
		}
		Group trophy = this.trophies[place];
		if (trophy != null) {
			// The trophy of the place comes down over the robot
			trophy.setVisible(true);
			trophy.setOpacity(0);
			trophy.setTranslateY(-this.getHeight());
			TranslateTransition fall = new TranslateTransition(Duration.millis(1100), trophy);
			fall.setToY(0);
			fall.setInterpolator(Podium.BOUNCE);
			fall.setDelay(Duration.millis(450));
			FadeTransition fade = new FadeTransition(Duration.millis(150), trophy);
			fade.setToValue(1);
			fade.setDelay(Duration.millis(450));
			host.animate(new ParallelTransition(fall, fade)).play();
			host.sparkles(trophy);
		}
		// The step lights up as the robot lands
		Rectangle step = this.steps[place];
		host.animate(new SequentialTransition(new PauseTransition(Duration.millis(700)), new Timeline(new KeyFrame(Duration.millis(120), new KeyValue(step.opacityProperty(), 0.55)),
				new KeyFrame(Duration.millis(400), new KeyValue(step.opacityProperty(), 1))))).play();
	}

	void hide(int place) {
		if (this.figures[place] != null) {
			this.figures[place].setVisible(false);
		}
		this.spots[place].setOpacity(0);
		if (this.trophies[place] != null) {
			this.trophies[place].setVisible(false);
		}
		if (place == 1) {
			host.fireworks.setRain(0);
			host.fireworks.setAutoFireworks(0);
		}
	}
}

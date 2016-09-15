package widgets;

import java.io.ByteArrayInputStream;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Oponent extends Canvas {

	private model.Opponent oponent;

	private boolean top;

	private int initialX;

	private int initialY;

	private int level;

	private boolean last;

	public static final int canvasWidth = 250;

	public static final int canvasHeight = 200;

	public static final int squareHeight = 150;

	public static final int squareWidth = 200;

	public Oponent(model.Opponent oponent, boolean top, int level, boolean last) {
		super(Oponent.canvasWidth, Oponent.canvasHeight * Math.pow(2, level));

		this.top = top;
		this.level = level;
		this.oponent = oponent;
		this.initialX = this.getInitialX();
		this.last = last;
		this.initialY = this.getInitialY();

		this.drawLines();
	}

	public boolean isLast() {
		return this.last;
	}

	public int getInitialX() {
		return 0;
	}

	public int getInitialY() {
		int y = (int) ((Oponent.canvasHeight * Math.pow(2, this.level) / 2) - 25);
		y -= Oponent.squareHeight / 2;
		return y;
	}

	private void drawLines() {

		int direction = this.top ? 1 : -1;

		GraphicsContext gc = this.getGraphicsContext2D();

		gc.setFill(Color.BLACK);
		gc.setStroke(Color.YELLOWGREEN);

		// Guide lines comment it!
		// gc.strokeRect(0, 0, this.getWidth(), this.getHeight());

		gc.setLineWidth(5);
		gc.fillRect(initialX, initialY, Oponent.squareWidth, Oponent.squareHeight);

		ByteArrayInputStream is = new ByteArrayInputStream(this.oponent.getImage());

		Image img = new Image(is, Oponent.squareWidth - 45, Oponent.squareHeight, true, true);
		gc.drawImage(img, initialX + 35, initialY + 5);
		gc.strokeRect(initialX, initialY, Oponent.squareWidth, Oponent.squareHeight);

		gc.setLineWidth(1);

		gc.setFont(new Font(Oponent.canvasHeight / 10));
		gc.rotate(90);
		gc.fillText(this.oponent.getName(), initialY + 5, -10);
		gc.strokeText(this.oponent.getName(), initialY + 5, -10);
		// gc.strokeText(this.oponent.getName(), 5, initialY +
		// Oponent.squareHeight - 50);
		gc.rotate(-90);

		// gc.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 20));
		// gc.fillText(this.oponent.getBattleCry(), 30, initialY +
		// Oponent.squareHeight - 20);
		// gc.strokeText(this.oponent.getBattleCry(), 30, initialY +
		// Oponent.squareHeight - 20);

		gc.setLineWidth(3);

		initialX += Oponent.squareWidth;
		initialY += Oponent.squareHeight / 2;

		if (this.last) return;

		// h
		gc.strokeLine(initialX, initialY, initialX + 50, initialY);

		initialX += 50;

		gc.strokeLine(initialX, initialY, initialX, (this.getHeight() - 2) * direction);

		initialY = (int) (this.getHeight() - 2) * direction;

		gc.strokeLine(initialX, initialY, initialX + 50, initialY);
	}
}

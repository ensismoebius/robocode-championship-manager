/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package widgets;

import java.io.File;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
public class MediaControl extends BorderPane {

	private MediaPlayer mp;
	private MediaView mediaView;
	private final boolean repeat = false;
	private boolean stopRequested = false;
	private boolean atEndOfMedia = false;
	private Duration duration;
	private Slider timeSlider;
	private Label playTime;
	private Slider volumeSlider;
	private HBox mediaBar;

	private FileChooser chooser;
	private Button chooseFileButton;

	private File selectedFile;
	private Duration currentTime;

	public MediaControl(final MediaPlayer mp) {

		this.chooser = new FileChooser();

		if (mp == null)
			this.constructButton();
		else
			this.constructPlayer(mp);

	}

	public double getCurrentTime() {
		return this.currentTime.toSeconds();
	}

	public File getSelectedFile() {
		return this.selectedFile;
	}

	public void reset() {
		this.mp = null;
		this.mediaBar = null;
		this.mediaView = null;
		this.constructButton();
	}

	private void constructButton() {
		this.getChildren().clear();

		this.chooseFileButton = new Button("Escolher musica");
		this.chooseFileButton.setOnMouseClicked(event -> {
			File musicFile = this.chooser.showOpenDialog(null);
			if (musicFile.exists()) {
				this.constructPlayer(new MediaPlayer(new Media(musicFile.toURI().toString())));
				this.selectedFile = musicFile;
			}

		});
		this.setTop(this.chooseFileButton);

	}

	private void constructPlayer(final MediaPlayer p) {

		this.getChildren().clear();

		this.mp = p;
		setStyle("-fx-background-color: #bfc2c7;");
		mediaView = new MediaView(p);
		Pane mvPane = new Pane() {
		};
		mvPane.getChildren().add(mediaView);
		mvPane.setStyle("-fx-background-color: black;");
		setCenter(mvPane);

		mediaBar = new HBox();
		mediaBar.setAlignment(Pos.CENTER);
		mediaBar.setPadding(new Insets(5, 10, 5, 10));
		BorderPane.setAlignment(mediaBar, Pos.CENTER);

		final Button playButton = new Button(">");

		playButton.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				Status status = p.getStatus();

				if (status == Status.UNKNOWN || status == Status.HALTED) {
					// don't do anything in these states
					return;
				}

				if (status == Status.PAUSED || status == Status.READY || status == Status.STOPPED) {
					// rewind the movie if we're sitting at the end
					if (atEndOfMedia) {
						p.seek(p.getStartTime());
						atEndOfMedia = false;
					}
					p.play();
				} else {
					p.pause();
				}
			}
		});
		p.currentTimeProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				updateValues();
			}
		});

		p.setOnPlaying(new Runnable() {
			public void run() {
				if (stopRequested) {
					p.pause();
					stopRequested = false;
				} else {
					playButton.setText("||");
				}
			}
		});

		p.setOnPaused(new Runnable() {
			public void run() {
				System.out.println("onPaused");
				playButton.setText(">");
			}
		});

		p.setOnReady(new Runnable() {
			public void run() {
				duration = p.getMedia().getDuration();
				updateValues();
			}
		});

		p.setCycleCount(repeat ? MediaPlayer.INDEFINITE : 1);
		p.setOnEndOfMedia(new Runnable() {
			public void run() {
				if (!repeat) {
					playButton.setText(">");
					stopRequested = true;
					atEndOfMedia = true;
				}
			}
		});

		mediaBar.getChildren().add(playButton);
		// Add spacer
		Label spacer = new Label("   ");
		mediaBar.getChildren().add(spacer);

		// Add Time label
		Label timeLabel = new Label("Time: ");
		mediaBar.getChildren().add(timeLabel);

		// Add time slider
		timeSlider = new Slider();
		HBox.setHgrow(timeSlider, Priority.ALWAYS);
		timeSlider.setMinWidth(50);
		timeSlider.setMaxWidth(Double.MAX_VALUE);
		timeSlider.valueProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				if (timeSlider.isValueChanging()) {
					// multiply duration by percentage calculated by slider
					// position
					p.seek(duration.multiply(timeSlider.getValue() / 100.0));
				}
			}
		});
		mediaBar.getChildren().add(timeSlider);

		// Add Play label
		playTime = new Label();
		playTime.setPrefWidth(130);
		playTime.setMinWidth(50);
		mediaBar.getChildren().add(playTime);

		// Add the volume label
		Label volumeLabel = new Label("Vol: ");
		mediaBar.getChildren().add(volumeLabel);

		// Add Volume slider
		volumeSlider = new Slider();
		volumeSlider.setPrefWidth(70);
		volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
		volumeSlider.setMinWidth(30);
		volumeSlider.valueProperty().addListener(new InvalidationListener() {
			public void invalidated(Observable ov) {
				if (volumeSlider.isValueChanging()) {
					p.setVolume(volumeSlider.getValue() / 100.0);
				}
			}
		});
		mediaBar.getChildren().add(volumeSlider);

		setBottom(mediaBar);
	}

	protected void updateValues() {
		if (playTime != null && timeSlider != null && volumeSlider != null)
			Platform.runLater(new Runnable() {

				public void run() {
					currentTime = mp.getCurrentTime();
					playTime.setText(formatTime(currentTime, duration));
					timeSlider.setDisable(duration.isUnknown());
					if (!timeSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !timeSlider.isValueChanging()) {
						timeSlider.setValue(duration.toMillis() * 100.0);
					}
					if (!volumeSlider.isValueChanging()) {
						volumeSlider.setValue((int) Math.round(mp.getVolume() * 100));
					}
				}
			});
	}

	private static String formatTime(Duration elapsed, Duration d) {
		int intElapsed = (int) Math.floor(elapsed.toSeconds());
		int elapsedHours = intElapsed / (60 * 60);
		if (elapsedHours > 0) {
			intElapsed -= elapsedHours * 60 * 60;
		}
		int elapsedMinutes = intElapsed / 60;
		int elapsedSeconds = intElapsed - elapsedHours * 60 * 60 - elapsedMinutes * 60;

		if (d.greaterThan(Duration.ZERO)) {
			int intDuration = (int) Math.floor(d.toSeconds());
			int durationHours = intDuration / (60 * 60);
			if (durationHours > 0) {
				intDuration -= durationHours * 60 * 60;
			}
			int durationMinutes = intDuration / 60;
			int durationSeconds = intDuration - durationHours * 60 * 60 - durationMinutes * 60;
			if (durationHours > 0) {
				return String.format("%d:%02d:%02d/%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds, durationHours, durationMinutes, durationSeconds);
			} else {
				return String.format("%02d:%02d/%02d:%02d", elapsedMinutes, elapsedSeconds, durationMinutes, durationSeconds);
			}
		} else {
			if (elapsedHours > 0) {
				return String.format("%d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
			} else {
				return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
			}
		}
	}
}
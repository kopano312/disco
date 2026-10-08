package com.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Application.launch(ModernMediaPlayer.class, args);
    }

    public static class ModernMediaPlayer extends Application {

        private MediaPlayer mediaPlayer;
        private MediaView mediaView;
        private ListView<File> playlistView;
        private ObservableList<File> playlist;
        private Label nowPlayingLabel;
        private Label timeLabel;
        private Slider progressSlider;
        private Slider volumeSlider;
        private Button playPauseBtn;
        private Button muteBtn;
        private Stage primaryStage;
        private boolean isMuted = false;
        private double volumeBeforeMute = 0.5;

        
        private static final String BG_DARK = "#121212";
        private static final String BG_MEDIUM = "#1E1E1E";
        private static final String BG_LIGHT = "#2D2D2D";
        private static final String ACCENT = "#BB86FC";
        private static final String ACCENT_HOVER = "#9A67EA";
        private static final String TEXT_PRIMARY = "#FFFFFF";
        private static final String TEXT_SECONDARY = "#B3B3B3";
        private static final String BORDER_COLOR = "#333333";

        @Override
        public void start(Stage stage) {
            this.primaryStage = stage;
            stage.setTitle("DISCO");

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: " + BG_DARK + ";");
            root.setFocusTraversable(true);

            root.setTop(createTopBar());
            root.setCenter(createCenterArea());
            root.setRight(createPlaylistPanel());

            Scene scene = new Scene(root, 1100, 700);
            scene.setFill(Color.web(BG_DARK));

            // Attach keyboard handler to the SCENE so it always receives key events
            setupKeyboardControls(scene);

            stage.setScene(scene);
            stage.show();

            
            root.requestFocus();
        }

        
        private VBox createTopBar() {
            VBox topBox = new VBox(5);
            topBox.setPadding(new Insets(15, 20, 10, 20));
            topBox.setStyle("-fx-background-color: " + BG_MEDIUM +
                    "; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");

            HBox titleBox = new HBox(10);
            titleBox.setAlignment(Pos.CENTER_LEFT);

            
            try (InputStream is = getClass().getResourceAsStream("/logo.png")) {
                if (is != null) {
                    ImageView logo = new ImageView(new Image(is));
                    logo.setFitWidth(32);
                    logo.setFitHeight(32);
                    logo.setPreserveRatio(true);
                    titleBox.getChildren().add(logo);
                }
            } catch (Exception ignored) { }

            Label appTitle = new Label("DISCO");
            appTitle.setStyle("-fx-text-fill: " + TEXT_PRIMARY +
                    "; -fx-font-size: 18px; -fx-font-weight: bold;");

            titleBox.getChildren().add(appTitle);

            nowPlayingLabel = new Label("No media loaded");
            nowPlayingLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");

            topBox.getChildren().addAll(titleBox, nowPlayingLabel);
            return topBox;
        }

    
        private VBox createCenterArea() {
            VBox centerBox = new VBox(10);
            centerBox.setAlignment(Pos.CENTER);
            centerBox.setPadding(new Insets(10, 20, 10, 20));

            StackPane mediaContainer = new StackPane();
            mediaContainer.setStyle("-fx-background-color: " + BG_MEDIUM +
                    "; -fx-background-radius: 8;");
            mediaContainer.setMinHeight(300);

            mediaView = new MediaView();
            mediaView.setPreserveRatio(true);
            mediaView.fitWidthProperty().bind(mediaContainer.widthProperty().subtract(20));
            mediaView.fitHeightProperty().bind(mediaContainer.heightProperty().subtract(20));

            Label placeholder = new Label("No media loaded\n\nAdd files from the playlist");
            placeholder.setStyle("-fx-text-fill: " + TEXT_SECONDARY +
                    "; -fx-font-size: 16px; -fx-text-alignment: center;");

            mediaContainer.getChildren().addAll(mediaView, placeholder);
            mediaView.mediaPlayerProperty().addListener((obs, old, newVal) ->
                    placeholder.setVisible(newVal == null));

            centerBox.getChildren().add(mediaContainer);

            // Progress slider
            progressSlider = new Slider();
            progressSlider.setStyle(
                    "-fx-control-inner-background: " + BG_LIGHT + "; " +
                    "-fx-accent: " + ACCENT + "; -fx-background-color: transparent;");
            progressSlider.setMin(0);
            progressSlider.setMax(100);
            progressSlider.setDisable(true);
            progressSlider.setFocusTraversable(false);

            timeLabel = new Label("00:00 / 00:00");
            timeLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");

            HBox progressBox = new HBox(10, progressSlider, timeLabel);
            progressBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(progressSlider, Priority.ALWAYS);
            centerBox.getChildren().add(progressBox);

            centerBox.getChildren().add(createControlButtons());
            return centerBox;
        }

        private HBox createControlButtons() {
            HBox box = new HBox(12);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(5, 0, 10, 0));

            // Plain text labels instead of emoji icons
            Button prevBtn = createControlButton("Prev", "Previous (P)");
            playPauseBtn = createControlButton("Play", "Play/Pause (Space)");
            Button stopBtn = createControlButton("Stop", "Stop (S)");
            Button nextBtn = createControlButton("Next", "Next (N)");

            Label volLabel = new Label("Vol");
            volLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");

            volumeSlider = new Slider(0, 1, 0.5);
            volumeSlider.setPrefWidth(100);
            volumeSlider.setStyle("-fx-control-inner-background: " + BG_LIGHT +
                    "; -fx-accent: " + ACCENT + ";");
            volumeSlider.setFocusTraversable(false);
            volumeSlider.valueProperty().addListener((obs, old, newVal) -> {
                if (mediaPlayer != null && !isMuted) {
                    mediaPlayer.setVolume(newVal.doubleValue());
                }
            });

            muteBtn = createControlButton("Mute", "Mute (M)");
            muteBtn.setMinWidth(70);

            playPauseBtn.setOnAction(e -> togglePlayPause());
            prevBtn.setOnAction(e -> playPrevious());
            stopBtn.setOnAction(e -> stopPlayback());
            nextBtn.setOnAction(e -> playNext());
            muteBtn.setOnAction(e -> toggleMute());

            box.getChildren().addAll(prevBtn, playPauseBtn, stopBtn, nextBtn,
                    new Separator(Orientation.VERTICAL),
                    volLabel, volumeSlider, muteBtn);
            return box;
        }

        private Button createControlButton(String text, String tooltip) {
            Button btn = new Button(text);
            btn.setTooltip(new Tooltip(tooltip));
            btn.setFocusTraversable(false); // important: stop Space from triggering button
            String base = "-fx-background-color: " + BG_LIGHT + "; -fx-text-fill: " + TEXT_PRIMARY +
                    "; -fx-font-size: 14px; -fx-min-width: 60px; -fx-min-height: 40px;" +
                    " -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: " + BORDER_COLOR +
                    "; -fx-border-radius: 8;";
            String hover = "-fx-background-color: " + ACCENT + "; -fx-text-fill: " + BG_DARK +
                    "; -fx-font-size: 14px; -fx-min-width: 60px; -fx-min-height: 40px;" +
                    " -fx-background-radius: 8; -fx-cursor: hand; -fx-border-color: " + ACCENT +
                    "; -fx-border-radius: 8;";
            btn.setStyle(base);
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e -> btn.setStyle(base));
            return btn;
        }

        
        private VBox createPlaylistPanel() {
            VBox box = new VBox(10);
            box.setPadding(new Insets(15));
            box.setPrefWidth(320);
            box.setMinWidth(280);
            box.setStyle("-fx-background-color: " + BG_MEDIUM + "; -fx-border-color: " +
                    BORDER_COLOR + "; -fx-border-width: 0 0 0 1;");

            Label playlistTitle = new Label("Playlist");
            playlistTitle.setStyle("-fx-text-fill: " + TEXT_PRIMARY +
                    "; -fx-font-size: 16px; -fx-font-weight: bold;");

            playlistView = new ListView<>();
            playlist = FXCollections.observableArrayList();
            playlistView.setItems(playlist);
            playlistView.setFocusTraversable(false);
            playlistView.setStyle(
                    "-fx-background-color: " + BG_DARK + "; -fx-control-inner-background: " + BG_DARK +
                    "; -fx-border-color: " + BORDER_COLOR +
                    "; -fx-border-radius: 6; -fx-background-radius: 6;");

            playlistView.setCellFactory(lv -> new ListCell<File>() {
                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: " + BG_DARK + ";");
                    } else {
                        setText(item.getName());
                        setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-background-color: " +
                                BG_DARK + "; -fx-padding: 8; -fx-font-size: 13px;");
                    }
                }
            });

            playlistView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
                if (newVal != null) loadMedia(newVal);
            });

            VBox.setVgrow(playlistView, Priority.ALWAYS);

            Button addBtn = new Button("Add");
            addBtn.setFocusTraversable(false);
            addBtn.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: " + BG_DARK +
                    "; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
            addBtn.setOnMouseEntered(e -> addBtn.setStyle(
                    "-fx-background-color: " + ACCENT_HOVER + "; -fx-text-fill: " + BG_DARK +
                    "; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;"));
            addBtn.setOnMouseExited(e -> addBtn.setStyle(
                    "-fx-background-color: " + ACCENT + "; -fx-text-fill: " + BG_DARK +
                    "; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;"));
            addBtn.setOnAction(e -> addFiles());

            Button removeBtn = new Button("Remove");
            removeBtn.setFocusTraversable(false);
            removeBtn.setStyle("-fx-background-color: " + BG_LIGHT + "; -fx-text-fill: " + TEXT_PRIMARY +
                    "; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;" +
                    " -fx-border-color: " + BORDER_COLOR + "; -fx-border-radius: 6;");
            removeBtn.setOnAction(e -> removeFile());

            HBox.setHgrow(addBtn, Priority.ALWAYS);
            HBox.setHgrow(removeBtn, Priority.ALWAYS);
            addBtn.setMaxWidth(Double.MAX_VALUE);
            removeBtn.setMaxWidth(Double.MAX_VALUE);

            HBox btnBox = new HBox(8, addBtn, removeBtn);
            btnBox.setAlignment(Pos.CENTER);

            Label hintLabel = new Label(
                    "Space: Play/Pause | S: Stop\nN: Next | P: Previous\nUp/Down: Volume | M: Mute");
            hintLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY +
                    "; -fx-font-size: 10px; -fx-text-alignment: center;");
            hintLabel.setWrapText(true);

            box.getChildren().addAll(playlistTitle, playlistView, btnBox, hintLabel);
            return box;
        }

        
        private void setupKeyboardControls(Scene scene) {
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case SPACE -> { togglePlayPause(); event.consume(); }
                    case S     -> { stopPlayback();    event.consume(); }
                    case N     -> { playNext();        event.consume(); }
                    case P     -> { playPrevious();    event.consume(); }
                    case UP    -> { increaseVolume();  event.consume(); }
                    case DOWN  -> { decreaseVolume();  event.consume(); }
                    case M     -> { toggleMute();      event.consume(); }
                    default    -> { }
                }
            });
        }

        
        private void addFiles() {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Media Files");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Media Files",
                            "*.mp3", "*.mp4", "*.wav", "*.m4a", "*.aac", "*.flac",
                            "*.avi", "*.mkv", "*.mov"),
                    new FileChooser.ExtensionFilter("Audio Files",
                            "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac"),
                    new FileChooser.ExtensionFilter("Video Files",
                            "*.mp4", "*.avi", "*.mkv", "*.mov"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
            if (files != null && !files.isEmpty()) {
                for (File file : files) {
                    if (!playlist.contains(file)) playlist.add(file);
                }
                if (mediaPlayer == null && !playlist.isEmpty()) {
                    playlistView.getSelectionModel().select(0);
                }
            }
            primaryStage.getScene().getRoot().requestFocus();
        }

        private void removeFile() {
            File selected = playlistView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            int index = playlist.indexOf(selected);
            playlist.remove(selected);

            if (mediaPlayer != null && selected.equals(getCurrentFile())) {
                if (playlist.isEmpty()) {
                    stopPlayback();
                    disposePlayer();
                    nowPlayingLabel.setText("No media loaded");
                    mediaView.setMediaPlayer(null);
                    playPauseBtn.setText("Play");
                } else {
                    int next = Math.min(index, playlist.size() - 1);
                    playlistView.getSelectionModel().select(next);
                }
            }
            primaryStage.getScene().getRoot().requestFocus();
        }

        private File getCurrentFile() {
            return playlistView.getSelectionModel().getSelectedItem();
        }

        
        private void loadMedia(File file) {
            try {
                disposePlayer();

                Media media = new Media(file.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.setVolume(volumeSlider.getValue());

                nowPlayingLabel.setText("Now Playing: " + file.getName());
                updatePlayPauseButton(true);

                mediaPlayer.currentTimeProperty().addListener((obs, old, newVal) -> {
                    if (mediaPlayer.getTotalDuration() != null &&
                            mediaPlayer.getTotalDuration().greaterThan(Duration.ZERO)) {
                        double progress = newVal.toMillis() /
                                mediaPlayer.getTotalDuration().toMillis() * 100;
                        progressSlider.setValue(progress);
                    }
                    updateTimeLabel();
                });

                mediaPlayer.totalDurationProperty().addListener((obs, old, newVal) -> {
                    if (newVal != null) {
                        progressSlider.setDisable(false);
                        updateTimeLabel();
                    }
                });

                mediaPlayer.setOnEndOfMedia(this::playNext);

                progressSlider.setOnMouseReleased(e -> {
                    if (mediaPlayer.getTotalDuration() != null) {
                        double seekTime = progressSlider.getValue() / 100.0 *
                                mediaPlayer.getTotalDuration().toMillis();
                        mediaPlayer.seek(Duration.millis(seekTime));
                    }
                });

                mediaPlayer.statusProperty().addListener((obs, old, newStatus) -> {
                    if (newStatus == MediaPlayer.Status.PLAYING) updatePlayPauseButton(true);
                    else if (newStatus == MediaPlayer.Status.PAUSED) updatePlayPauseButton(false);
                });

                mediaPlayer.play();
                progressSlider.setDisable(false);

            } catch (Exception e) {
                showError("Error loading media",
                        "Could not load: " + file.getName() + "\n" + e.getMessage());
            }
        }

        private void updatePlayPauseButton(boolean playing) {
            if (playPauseBtn != null) playPauseBtn.setText(playing ? "Pause" : "Play");
        }

        private void updateTimeLabel() {
            if (mediaPlayer != null && mediaPlayer.getCurrentTime() != null &&
                    mediaPlayer.getTotalDuration() != null) {
                timeLabel.setText(formatTime(mediaPlayer.getCurrentTime()) + " / " +
                        formatTime(mediaPlayer.getTotalDuration()));
            }
        }

        private String formatTime(Duration duration) {
            if (duration == null || duration.isUnknown()) return "00:00";
            int totalSeconds = (int) duration.toSeconds();
            return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        }

        
        private void togglePlayPause() {
            if (mediaPlayer == null) return;
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                updatePlayPauseButton(false);
            } else {
                mediaPlayer.play();
                updatePlayPauseButton(true);
            }
        }

        private void stopPlayback() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.seek(Duration.ZERO);
                progressSlider.setValue(0);
                updatePlayPauseButton(false);
                updateTimeLabel();
            }
        }

        private void playNext() {
            if (playlist.isEmpty()) return;
            int currentIndex = playlistView.getSelectionModel().getSelectedIndex();
            int nextIndex = (currentIndex + 1) % playlist.size();
            playlistView.getSelectionModel().select(nextIndex);
        }

        private void playPrevious() {
            if (playlist.isEmpty()) return;
            int currentIndex = playlistView.getSelectionModel().getSelectedIndex();
            int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
            playlistView.getSelectionModel().select(prevIndex);
        }

        private void increaseVolume() {
            double newVol = Math.min(1.0, volumeSlider.getValue() + 0.05);
            volumeSlider.setValue(newVol);
            if (mediaPlayer != null) mediaPlayer.setVolume(newVol);
        }

        private void decreaseVolume() {
            double newVol = Math.max(0.0, volumeSlider.getValue() - 0.05);
            volumeSlider.setValue(newVol);
            if (mediaPlayer != null) mediaPlayer.setVolume(newVol);
        }

        private void toggleMute() {
            if (mediaPlayer == null) return;
            if (isMuted) {
                mediaPlayer.setVolume(volumeBeforeMute);
                volumeSlider.setValue(volumeBeforeMute);
                isMuted = false;
            } else {
                volumeBeforeMute = mediaPlayer.getVolume();
                mediaPlayer.setVolume(0);
                volumeSlider.setValue(0);
                isMuted = true;
            }
        }

        private void disposePlayer() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        }

        private void showError(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }

        @Override
        public void stop() {
            disposePlayer();
        }
    }
}
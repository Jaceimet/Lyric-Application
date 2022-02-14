package application;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

import javafx.event.ActionEvent;

import javafx.scene.control.Slider;

import javafx.scene.control.Label;

import javafx.scene.control.TextArea;

import javafx.scene.layout.BorderPane;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.VBox;

import javafx.scene.layout.Pane;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import java.util.Scanner;

//import org.controlsfx.dialog.ExceptionDialog;


import javafx.stage.FileChooser;
import javafx.util.Duration;
import application.LyricsData;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;

public class LyricsAppController {
	@FXML
	private BorderPane borderPane;
	@FXML
	private Button openButton;
	@FXML
	private Button viewButton;
	@FXML
	private Button editButton;
	@FXML
	private Button playPauseButton;
	@FXML
	private Button stopButton;
	@FXML
	private Slider timeSlider;
	@FXML
	private Label timeLabel;
	@FXML
	private TextArea textArea;
	@FXML
	private Pane viewPane;
	@FXML
	private VBox viewBox;
	private boolean playing = false;
	
	private File fileLyrics;
	
	private File fileMusic;
	
	private MediaPlayer mediaPlayer;
		
	private AnimationTimer timer;
	
	private int time;
	
	private double middleY = 175.0D;
	  
	private double labelHeight = 30.0D;
	  
	private FileChooser.ExtensionFilter lrcFilter = 
			new FileChooser.ExtensionFilter("Lyrics or MP3 Files", 
					new String[] {"*.lrc", "*.mp3"});
	
    private StringBuilder builder = new StringBuilder();


    private LyricsData lyricsData = new LyricsData();

	// Event Listener on Button[#openButton].onAction
	@FXML
	public void openButtonPressed(ActionEvent event) {
	    FileChooser fileChooser = new FileChooser();               
	    fileChooser.setTitle("Open Lyrics File");
	    fileChooser.setInitialDirectory(new File("."));
	    fileChooser.getExtensionFilters().add(this.lrcFilter);
	    
	    File file = fileChooser.showOpenDialog(
	    		borderPane.getScene().getWindow());
		if (file != null) {
			String path = file.getPath();
			path = path.substring(0, path.length()-4);
			this.fileLyrics = new File(String.valueOf(path)+ ".lrc");
			this.fileMusic = new File(String.valueOf(path)+".mp3");
		} 
		if (this.fileLyrics != null && this.fileLyrics.exists())
			readFile(fileLyrics);
		if (this.fileMusic != null && this.fileMusic.exists())
			playMusic();
	}
	// Event Listener on Button[#viewButton].onAction
	@FXML
	public void viewButtonPressed(ActionEvent event) {
		viewPane.setVisible(true);
		textArea.setVisible(false);
	}
	// Event Listener on Button[#editButton].onAction
	@FXML
	public void editButtonPressed(ActionEvent event) {
		viewPane.setVisible(false);
		textArea.setVisible(true);
	}
	// Event Listener on Button[#playPauseButton].onAction
	@FXML
	public void playPauseButtonPressed(ActionEvent event) {
	      playing = !playing;
	      
	      if (playing) {
	    	 mediaPlayer.seek(Duration.millis(time));
	         playPauseButton.setText("Pause");
	         mediaPlayer.play();
	         timer.start();
	      }
	      else {
	         playPauseButton.setText("Play");
	         mediaPlayer.pause();
	         timer.stop();
	      }
	      showTime();
	}
	// Event Listener on Button[#stopButton].onAction
	@FXML
	public void stopButtonPressed(ActionEvent event) {
		playing = false;
		playPauseButton.setText("Play");
		if(mediaPlayer != null) {
			mediaPlayer.stop();
			timer.stop();
		}
		time = 0;
		showTime();
	}
	
	public void initialize() {
		this.timer = new AnimationTimer() {

			@Override
			public void handle(long now) {
				if (mediaPlayer != null)
					time = (int) mediaPlayer.getCurrentTime().toMillis();
				showTime();
			}
		};
		timeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (!playing) {
				time = newValue.intValue();
				showTime();
			}
		});
	    this.viewPane.setVisible(true);
	    this.textArea.setVisible(false);
		
	}
	
	public void playMusic() {
		
		Media media = new Media(fileMusic.toURI().toString());
		if(mediaPlayer != null)
			mediaPlayer.dispose();
		mediaPlayer = new MediaPlayer(media);

	    Duration du = mediaPlayer.getTotalDuration();
	    if(du != Duration.UNKNOWN && du != Duration.INDEFINITE && du != Duration.ZERO)
	        timeSlider.setMax(du.toMillis());
	        time = 0;
	        showTime();
	    
		mediaPlayer.setOnEndOfMedia(new Runnable() {
			public void run() {
				playing = false;
				playPauseButton.setText("Play");
				mediaPlayer.seek(Duration.ZERO);
				mediaPlayer.pause();
				timer.stop();
				time = 0;
				showTime();
			}
		});
		
//		mediaPlayer.setOnError(new Runnable() {
//			public void run() {
//				ExceptionDialog dialog = new ExceptionDialog(mediaPlayer.getError());
//				dialog.showAndWait();
//			}
//		});
		
	}
		
    private void showTime() {
    	timeLabel.setText(getTimeString(time)); //getTimeString developed in Assign13
    	timeSlider.setValue(time);
		double width = this.viewPane.getWidth();
		double height = this.viewPane.getHeight();
		this.viewPane.setClip(new Rectangle(width, height));
					
		middleY = height / 2;
		this.viewBox.setLayoutY(middleY);
		
    	double yOffset = this.lyricsData.getYOffset(this.time);
        this.viewBox.setLayoutY(this.middleY - yOffset * this.labelHeight);
        int index = (int)yOffset;
        int percent = (int)((yOffset - index) * 100.0D);
        String style = String.format("linear-gradient(to right, magenta, magenta %d%%, white %d%%, white);", 
        		new Object[] { Integer.valueOf(percent), Integer.valueOf(percent + 1) });
        ((Node)this.viewBox.getChildren().get(index)).setStyle("-fx-text-fill: " + style);
        if (index > 0)
          ((Node)this.viewBox.getChildren().get(index - 1)).setStyle("-fx-text-fill: cyan;");
    }
	
	public void readFile(File fileLyric) {     
		try(Scanner input = new Scanner(fileLyric)) {
             while (input.hasNext()) {
                builder.append(String.format("%s %n", input.nextLine()));            	
            }        	
		    textArea.setText(builder.toString());
		       

		    lyricsData.parseLyrics(textArea.getText());
		    viewBox.getChildren().clear();
		    for(int i = 0; i < lyricsData.getSize(); i++)
		    {
		    	Label line = new Label(lyricsData.getText(i));
		    	line.prefWidthProperty().bind(viewPane.widthProperty());
		    	viewBox.getChildren().add(line);
		    }
		    
		   
		}
		catch (IOException | NoSuchElementException e) {
			((Throwable) e).printStackTrace();
		}
	}
	

	  private String getTimeString(int millisec) {
		    int min = millisec / 60000;
		    int sec = millisec % 60000 / 1000;
		    int hsec = millisec % 1000 / 10;
		    return String.format("%02d:%02d.%02d", new Object[] { 
		    		Integer.valueOf(min), Integer.valueOf(sec), 
		    		Integer.valueOf(hsec) });
		  }
	
}

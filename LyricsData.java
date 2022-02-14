package application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import application.LyricsData;
//import application.LyricsData.Line;

public class LyricsData {
	
	
	public static final int ORIGINAL_ORDER = 0;
	public static final int SINGLE_STAMP = 1;
	public static final int MULTIPLE_STAMP = 2;

	private List<Line> originalLines;
	private List<Line> sortedLines;
	private List<Line> mergedLines;

	private int defaultTimeInterval = 2000;

	private String title;
	private String artist;
	private String album;
	private int offSet;

	private int size;
	private boolean edited;

	// constructors
	public LyricsData() {
		this.originalLines = new ArrayList<>();
		this.sortedLines = new ArrayList<>();
		this.mergedLines = new ArrayList<>();
	}

	private class Line {

		private int timeStamp;

		String text;

		public Line(int timeStamp, String text) {
			this.timeStamp = timeStamp;
			this.text = text;
		}

	}

	public static String getTimeString(int millisec) {
		int min = millisec / 60000;
		int sec = (millisec % 60000) / 1000;
		int hsec = (millisec % 1000) / 10;
		return String.format("%02d:%02d.%02d", min, sec, hsec);
	}

	public static int parseTimeString(String timeString) {
		int colon = timeString.indexOf(':');
		int min = (colon > 0 ? Integer.parseInt(timeString.substring(0, colon)) : 0);
		double sec = Double.parseDouble(timeString.substring(colon + 1));
		int millisec = (int) ((min * 60 + sec) * 1000);
		return millisec;
	}

	  public void parseLyrics(String string) {
		    this.title = this.artist = this.album = null;
		    this.offSet = this.size = 0;
		    this.originalLines.clear();
		    this.sortedLines.clear();
		    this.mergedLines.clear();
		    int t = 0;
		    Line line = null;
		    String[] tokens = string.split("\r\n|\n");
		    byte b;
		    int i;
		    String[] arrayOfString1;
		    for (i = (arrayOfString1 = tokens).length, b = 0; b < i; ) {
		      String token = arrayOfString1[b];
		      if (token.charAt(0) == '[' && Character.isDigit(token.charAt(1))) {
		        String text = token.substring(token.lastIndexOf(']') + 1);
		        for (int pos = 0; pos > -1; pos = token.indexOf('[', pos + 1)) {
		          t = parseTimeString(token.substring(pos + 1, token.indexOf(']', pos + 1)));
		          line = new Line(t, text);
		          this.originalLines.add(line);
		        } 
		      } else if (token.charAt(0) == '[' && Character.isLetter(token.charAt(1))) {
		        int begin = token.indexOf(':') + 1;
		        int end = token.indexOf(']');
		        if (token.startsWith("[ti")) {
		          this.title = token.substring(begin, end);
		        } else if (token.startsWith("[ar")) {
		          this.artist = token.substring(begin, end);
		        } else if (token.startsWith("[al")) {
		          this.album = token.substring(begin, end);
		        } else if (token.startsWith("[offset")) {
		          try {
		            this.offSet = Integer.parseInt(token.substring(begin, end).trim());
		          } catch (Exception exception) {}
		        } 
		      } else {
		        if (this.originalLines.size() > 0)
		          t += this.defaultTimeInterval; 
		        line = new Line(t, token);
		        this.originalLines.add(line);
		      } 
		      b++;
		    } 
		    this.sortedLines = (List<Line>)this.originalLines.stream()
		      .sorted(Comparator.comparing(l -> Integer.valueOf(l.timeStamp)))
		      .collect(Collectors.toList());
		    Map<String, List<Line>> groupedByText = (Map<String, List<Line>>)this.sortedLines.stream()
		      .collect(Collectors.groupingBy(l -> l.text));
		    this.mergedLines = (List<Line>)groupedByText.values().stream()
		      .sorted(Comparator.comparing(g -> Integer.valueOf(((Line)g.get(0)).timeStamp)))
		      .flatMap(Collection::stream)
		      .collect(Collectors.toList());
		    this.size = this.sortedLines.size();
		    this.edited = true;
		  }

	public String toString() {
		return toString(0);
	}

	public String toString(int format) {

		StringBuffer buffer = new StringBuffer();
		if (this.title != null)
			buffer.append("[ti:" + this.title + "]\r\n");
		if (this.artist != null)
			buffer.append("[ar:" + this.artist + "]\r\n");
		if (this.album != null)
			buffer.append("[al:" + this.album + "]\r\n");
		if (this.offSet != 0)
			buffer.append("[offset:" + this.offSet + "]\r\n");
		List<Line> outputLines = this.originalLines;
		switch (format) {
		case 0:
			outputLines = this.originalLines;
			break;
		case 1:
			outputLines = this.sortedLines;
			break;
		case 2:
			outputLines = this.mergedLines;
			break;
		}
		for (int i = 0; i < this.size;) {
			Line firstLine = outputLines.get(i);
			buffer.append("[" + getTimeString(firstLine.timeStamp) + "]");
			while (++i < this.size) {
				Line nextLine = outputLines.get(i);
				if ((format == 0 && firstLine.text != nextLine.text) || format == 1
						|| (format == 2 && !firstLine.text.equals(nextLine.text)))
					break;
				buffer.append("[" + getTimeString(nextLine.timeStamp) + "]");
			}
			buffer.append(firstLine.text);
			if (i < this.size)
				buffer.append("\r\n");
		}
		return buffer.toString();

	}

	private class SortComparator implements Comparator<Line> {
		public int compare(LyricsData.Line line1, LyricsData.Line line2) {
			return line1.timeStamp - line2.timeStamp;
		}
	}

	private class MergeComparator implements Comparator<Line> {
		public int compare(LyricsData.Line line1, LyricsData.Line line2) {
			if (line1.text.equals(line2.text))
				return line1.timeStamp - line2.timeStamp;
			return minTimeStamp(line1) - minTimeStamp(line2);
		}

		private int minTimeStamp(LyricsData.Line line) {
			String text = line.text;
			for (int i = 0; i < LyricsData.this.size; i++) {
				if (text.equals((LyricsData.this.sortedLines.get(i)).text))
					return (LyricsData.this.sortedLines.get(i).timeStamp);
			}
			return line.timeStamp;
		}
	}

	public int getSize() {
		return this.size;
	}

	public List<Line> getSortedLines() {
		return this.sortedLines;
	}

	public double getYOffset(int time) {
		for (int i = 0; i < this.size; i++) {
			double stamp = ((Line) this.sortedLines.get(i)).timeStamp;
			if (time <= stamp) {
				if (i == 0)
					return (stamp > 0.0D) ? (time / stamp) : 0.0D;
				double prevStamp = ((Line) this.sortedLines.get(i - 1)).timeStamp;
				double ratio = (prevStamp < stamp) ? ((time - prevStamp) / (stamp - prevStamp)) : 0.0D;
				return (i - 1) + ratio;
			}
		}
		return this.size - 0.01D;
	}

	public int getTimeStamp(int index) {
		return (index < 0) ? 0 : ((index < this.size) ? ((Line) this.sortedLines.get(index)).timeStamp
						: (((Line) this.sortedLines.get(this.size - 1)).timeStamp
								+ (index - this.size + 1) * this.defaultTimeInterval));
	}

	public void setTimeStamp(int index, int time) {
		if (index >= 0 && index < this.size) {
			((Line) this.sortedLines.get(index)).timeStamp = time;

		}
	}

	public String getText(int index) {
		return (index >= 0 && index < this.size) ? ((Line) this.sortedLines.get(index)).text : "";
	}

	public int getOffset() {
		return this.offSet;
	}

	public void setOffset(int offSet) {
		this.offSet = offSet;
	}

	public boolean isInOrder() {
		for (int i = 0; i < this.size - 1; i++) {
			if (((Line) this.sortedLines.get(i)).timeStamp >= ((Line) this.sortedLines.get(i + 1)).timeStamp)
				return false;
		}
		return true;
	}

}

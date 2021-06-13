package org.concordcarlisle.adrianwang.finalProjectVisualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.io.File;

import java.util.LinkedList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;


public class Visualizer {

	public int scaledMin;
	public int scaledMax;
	
	private int sampleMin;
	private int sampleMax;
	
	public int[][] scaledSamples; 
	
	private LinkedList<int[][]> spliced;
	private LinkedList<int[][]> transformed;
	
	private int transAbsMax;
	
	//TODO: do something with this (draw progress bar)
	private double soundDurationMillis;
	
	private AudioInputStream ais;
	
	private File file = new File("vibes.wav");
	
	private final int polarGraphRadius = 200;
	private final int polarSemiRange = 50;
	
	public Visualizer() {
		try {
		    ais = AudioSystem.getAudioInputStream(file);
		    
		    int frameLength = (int) ais.getFrameLength();
		    int frameSize = (int) ais.getFormat().getFrameSize();
		    byte[] eightBitByteArray = new byte[frameLength * frameSize];
		
		    int result = ais.read(eightBitByteArray);
		
		    int channels = ais.getFormat().getChannels();
		    int[][] samples = new int[channels][frameLength];
		
		    int sampleIndex = 0;
		    for (int t = 0; t < eightBitByteArray.length;) {
		        for (int channel = 0; channel < channels; channel++) {
		            int low = (int) eightBitByteArray[t];
		            t++;
		            int high = (int) eightBitByteArray[t];
		            t++;
		            int sample = getSixteenBitSample(high, low);
		            samples[channel][sampleIndex] = sample;
		        }
		        sampleIndex++;
		    }
		    
		    
		    System.out.println("framerate: " + ais.getFormat().getFrameRate());
		    soundDurationMillis = (frameLength + 0.0) / ais.getFormat().getFrameRate() * 1000;
		    
		    // -----------------------------
		    
			scaledSamples = new int[samples.length][GUI.GRAPH_WIDTH];
			
			scaleSamples(samples, scaledSamples);
		    
		    for (int sample : scaledSamples[0]) {

		        scaledMax = Math.max(scaledMax, sample);
		        scaledMin = Math.min(scaledMin, sample);

		        
		    }
		    
		    for(int[] channel : samples) {
			    for(int sample : channel) {
			    	sampleMax = Math.max(sampleMax, sample);
			    	sampleMin = Math.min(sampleMin, sample);
			    }
		    }
		    System.out.println("Sample Max: " + sampleMax);
		    
		    System.out.println(scaledSamples[0].length);
		    
		    // testing splice -----------------------------------------------------------------
		    
		    spliced = new LinkedList<>();
		    spliceSamples(samples, spliced);
		   // System.out.println("scaledsamples length: " + scaledSamples[0].length);
		    //System.out.println(GUI.GRAPH_WIDTH / 10 * samples[0].length / 17);
		    
		    //print test del later
		    String firstRow = "[";
		    int count = 0;
		    for(int i = 0; i < spliced.peek()[0].length; i++) {
		    	
		    	firstRow += spliced.peek()[0][i] + ", ";
		    	count++;
		    }
		    System.out.println(firstRow);
		    System.out.println("firstRow length: " + count);
		    System.out.println("spliced framerate: " + spliced.size());
		    // print testing  for splice ------------------------------------------
		    System.out.println(samples[0].length);
		    System.out.println(soundDurationMillis);
		    System.out.println((double)samples[0].length * GUI.FRAME_RATE / soundDurationMillis); // length of int[][] rep. each frame
		    System.out.println(soundDurationMillis / GUI.FRAME_RATE); // total # of frames
		    
		    
		    
		    // testing fourier transform
		   // transformed = new LinkedList<>(toFourier(spliced));    
		    
		} catch (Exception exp) {
		
		    exp.printStackTrace();
		
		} finally {

		    try {
		        ais.close();
		    } catch (Exception e) {
		    	
		    }
		    
		}
	}

	//...

	private int getSixteenBitSample(int high, int low) {
	    return (high << 8) + (low & 0x00ff);
	} 
	
	private void scaleSamples(int[][] samples, int[][] scaled) {
		
		 for(int r = 0; r < scaled.length; r++) {
	    	for(double cScaled = 0; cScaled < scaled[0].length; cScaled += 1.0) {
	    		double cSamples = cScaled * (samples[0].length / scaled[0].length);
	    		if(cSamples >= samples[0].length) cSamples = samples[0].length - 1;
	    		scaled[r][(int)cScaled] = samples[r][(int)cSamples];
	    	}
		 }
		 
	}
	
	// recursive function that splices the original sample data into a LinkedList of segmented int[][],
	// 			each representing a single frame
	private void spliceSamples(int[][] samples, LinkedList<int[][]> splicedOutput) {
		/* 
		 
		 
			given total duration in ms:
			17 ms between each frame
			each spliced arraylist should contain 17 ms of info
		 	
		 	each int[][] should be of (17 / soundDuration) * samples[0].length -> ~41 (home.wav) * samples length = 749.7
		
		 */
		double segmentLength = samples[0].length * GUI.FRAME_RATE * 1.075 / soundDurationMillis; // multiplier 1.075 on points <= 2, 1.06 for lines
																									// 1.08 on hollow circle = 3
		spliceSamples(samples, splicedOutput, 0, (int)segmentLength);
		
		System.out.println(splicedOutput.size());
		System.out.println("done splicing");
		
	}
	
	private void spliceSamples(int[][] samples, LinkedList<int[][]> splicedOutput, int startColIndex, int segmentLength) {
		int endColIndex = startColIndex + segmentLength - 1;
		boolean isFinal = false;
		if(endColIndex >= samples[0].length) {
			isFinal = true;
			endColIndex = samples[0].length - 1;
			segmentLength = endColIndex - startColIndex + 1;
		}
		if(startColIndex >= samples[0].length) {
			return;
		}
		int[][] segment = new int[samples.length][segmentLength];
		
		for(int r = 0; r < samples.length; r++) {
			int index = 0;
			for(int c = startColIndex; c < endColIndex; c++) {
				segment[r][index] = samples[r][c];
				index++;
			}
		}
		
		splicedOutput.add(segment);
		
		if(!isFinal) {
			spliceSamples(samples, splicedOutput, endColIndex + 1, segmentLength);
			
		}
		
		
	}
	
	public void graphNext(Graphics g) {
		// TODO: attempting to fix lag (doesn't work right now)
//		double playTimeMillis = System.currentTimeMillis() - startTimeMillis;
		
		
//		if(Math.abs((playTimeMillis / startTimeMillis) - (soundDurationMillis / GUI.FRAME_RATE - spliced.size()) / (soundDurationMillis / GUI.FRAME_RATE)) > 0.1) {
//			while(Math.abs((playTimeMillis / startTimeMillis) - (soundDurationMillis / GUI.FRAME_RATE - spliced.size()) / (soundDurationMillis / GUI.FRAME_RATE)) > 0.1) {
//				spliced.poll();
//			}
//		}
		int[][] nextSegment = spliced.poll().clone();
		//g.setColor(Color.CYAN);
		for(int r = 0; r < nextSegment.length; r++) {
			g.setColor(new Color(Color.HSBtoRGB((float)(0.9 + 0.1 * r), 1, 1)));
			
			for(int c = 0; c < nextSegment[0].length - 1; c++) {
				double y = (GUI.HEIGHT) / 2 - ((double)nextSegment[r][c] / (sampleMax) * GUI.GRAPH_HEIGHT);
//				double y1 = (GUI.HEIGHT) / 2 - ((double)nextSegment[r][c+1] / (scaledMax) * GUI.GRAPH_HEIGHT);
				
				double x = (double)c / (nextSegment[0].length - 1) * GUI.GRAPH_WIDTH;
//				double x1 = (double)(c+1) / (nextSegment[0].length - 1) * GUI.GRAPH_WIDTH;
				g.drawOval((int)x, (int)y, 3, 3);
			}
		}
	}
	
//	private void toPolar(LinkedList<int[][]> target) {
//		/*
//		 
//		 
//		for each int[][] current: 
//		
//			angle theta = column index / (current[0].length - 1) * 2pi
//		
//			radius = default radius - ((double)nextSegment[r][c+1] / (scaledMax) * GUI.GRAPH_HEIGHT)
//		
//			new x = r * cos(theta)
//			new y = r * sin(theta) --- needs to be calculated elsewhere
//		
//		*/
//		
//		for(int[][] segment : target) {
//			for(int r = 0; r < segment.length; r++) {
//				for(int c = 0; c < segment[0].length; c++) {
//					double radius = (double)polarGraphRadius  - ((double)segment[r][c] / (scaledMax) * polarSemiRange);
//					double angleRad = c / (segment[0].length - 1) * 2 * Math.PI;
//					segment[r][c] = (int)(radius * Math.sin(angleRad));
//				}
//			}
//		}
//		
//		System.out.println("translated spliced queue to polar vals");
//	}
	
	private LinkedList<int[][]> toFourier(LinkedList<int[][]> target) {
		LinkedList<int[][]> result = new LinkedList<>();
		
		transAbsMax = 0;
		
		for(int i = 0; i < target.size(); i++) {
			
			int[][] curr = target.get(i).clone();
			int[][] segment = new int[curr.length][curr[0].length];
			for(int r = 0; r < curr.length; r++) {
				int avg = 0;
				for(int c = 0; c < curr[0].length; c++) {
					avg += curr[r][c];
				}
				avg /= curr[0].length;
				
				for(int c = 0; c < curr[0].length; c++) {
					//double t = (i * curr[0].length + c + 1) / (target.size() * curr[0].length) * soundDurationMillis;
					
					//segment[r][c] = avg * curr[r][c] * Math.exp(-2 * Math.PI * ais.getFormat().getFrameRate())
					
					// Tk += (T[l])*np.exp((-2j*np.pi*k*l)/N)
					double tC = 0;
					for(int k = 0; k < curr[0].length; k++) {
						tC += curr[r][k] * Math.exp(-2 * Math.PI * c * k);
					}
					segment[r][c] = (int)tC;
					
					if(Math.abs(tC) > Math.abs(transAbsMax)) transAbsMax = (int)tC;
					
				}
				
			}
			
			result.add(segment);
		}
		System.out.println("done transforming");
		return result;
	}
	
	
	
	public void graphPolar(Graphics g) {
		//y values are already set, no need to change
		int[][] nextSegment = spliced.poll().clone();
		
		
		
		for(int r = 0; r < nextSegment.length; r++) {
			g.setColor(new Color(Color.HSBtoRGB((float)(0.5 + 0.1 * r), 1, 1)));
			
			for(int c = 0; c < nextSegment[0].length - 1; c++) {
				double radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / (sampleMax) * polarSemiRange);
				int size = 3;
//				if(radius < polarGraphRadius) {
//					radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / Math.abs(sampleMin) * 200);
//					
//				}
				double angleRad = (double)c / (nextSegment[0].length - 1) * Math.PI;
				
				double y = GUI.HEIGHT / 2 - radius * Math.cos(angleRad);
				double x = GUI.WIDTH / 2 + radius * Math.sin(angleRad);
				
				double x1 = GUI.WIDTH / 2 - radius * Math.sin(angleRad);
				
				//System.out.println("(" + x + ", " + y + ")");
				g.drawOval((int)x, (int)y, size, size);
				g.drawOval((int)x1, (int)y, size, size);

			}
		}
	}
	
	public void graphPolarTransformed(Graphics g) {
		//y values are already set, no need to change
		int[][] nextSegment = transformed.poll().clone();
		
		
		
		for(int r = 0; r < nextSegment.length; r++) {
			g.setColor(new Color(Color.HSBtoRGB((float)(0.5 + 0.1 * r), 1, 1)));
			
			for(int c = 0; c < nextSegment[0].length - 1; c++) {
				double radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / (transAbsMax) * polarSemiRange);
				int size = 3;
//				if(radius < polarGraphRadius) {
//					radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / Math.abs(sampleMin) * 200);
//					
//				}
				double angleRad = (double)c / (nextSegment[0].length - 1) * Math.PI;
				
				double y = GUI.HEIGHT / 2 - radius * Math.cos(angleRad);
				double x = GUI.WIDTH / 2 + radius * Math.sin(angleRad);
				
				double x1 = GUI.WIDTH / 2 - radius * Math.sin(angleRad);
				
				//System.out.println("(" + x + ", " + y + ")");
				g.drawOval((int)x, (int)y, size, size);
				g.drawOval((int)x1, (int)y, size, size);

			}
		}
	}
	
	public void graph(Graphics g) {
		//g.setColor(Color.cyan);
		//g.clearRect(0, 0, GUI.GRAPH_WIDTH, GUI.GRAPH_HEIGHT);
		for(int r = 0; r < scaledSamples.length; r++) {
			g.setColor(new Color(Color.HSBtoRGB((float)(0.5 + 0.1 * r), 1, 1)));
			
			for(int x = 0; x < scaledSamples[0].length - 1; x++) {
				double y = (GUI.HEIGHT) / 2 - ((double)scaledSamples[r][x] / (scaledMax) * GUI.GRAPH_HEIGHT);
				double y1 = (GUI.HEIGHT) / 2 - ((double)scaledSamples[r][x+1] / (scaledMax) * GUI.GRAPH_HEIGHT);
				
				g.drawLine(x, (int)y, x+1, (int)y1);
			}
		}
		
		
	}
	
	public void drawMarker(Graphics g) {
		//g.clearRect(0, 0, GUI.GRAPH_WIDTH, GUI.GRAPH_HEIGHT);
		g.setColor(new Color(0, 255, 0, 10));
		
		double playTimeMillis = System.currentTimeMillis() - startTimeMillis;
		int x = (int)(playTimeMillis / soundDurationMillis * GUI.GRAPH_WIDTH);
		g.drawLine(x, 0, x, GUI.HEIGHT);
		//System.out.println(scaledSamples[0].length);
	}
	
	double startTimeMillis;
	
	public void playAudio() {
		
		System.out.println("playing audio");
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip();
			clip.open(ais);
			clip.start();
			startTimeMillis = System.currentTimeMillis();
		} catch(Exception ex) {
			
		}
	    	
		
	    
	}
	
	@Override
	public String toString() {
		String result = "";
		for(int r = 0; r < scaledSamples.length; r++) {
			for(int c = 0; c < scaledSamples[0].length; c++) {
				result += scaledSamples[r][c] + ", ";
			}
			if(r < scaledSamples.length - 1) result += "\n";
		}
		return result;
	}

	
	
}






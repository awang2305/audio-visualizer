package org.concordcarlisle.adrianwang.finalProjectVisualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;

import java.util.LinkedList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;


public class Visualizer {

	
	private int sampleMin;
	private int sampleMax;		// max and min of all values of raw input data
	
	public int scaledMin;
	public int scaledMax;		// max and min of scaledSamples
	
	public int[][] scaledSamples; 		// for testing scaleSamples
	
	private LinkedList<int[][]> spliced;		// stored data frame-by-frame of flat amplitude graphs
	private LinkedList<Complex[][]> transformed;	// stored data frame-by-frame of fourier transform graphs
	
	private double transRealMax;
	private double transImagMax;		// max of the real, imaginary, and hypotenuse of transformed datapoints
	private double transAbsMax;
	
	private double soundDurationMillis; // duration in ms of soundtrack
	
	private AudioInputStream ais;
	
	private File file; // audio input file
	
	private int polarGraphRadius = 100;   // default radius of polar graph
	private int polarSemiRange = polarGraphRadius / 4; 	// range of polar graph amplitude
	
	public Visualizer(File file) {
		
		if(GUI.MODE.equals("POLAR")) {
			polarGraphRadius = 200;
			polarSemiRange = 50;
		}
		
		this.file = file;
		try {
			
			// READING AND STORING RAW AUDIO INPUT DATA
			
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
		    
		    // SCALE TESTING
		    
			scaledSamples = new int[samples.length][1024 * 215];
			
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
		    
		    // SPLICING
		    
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
		    
		    
		    
		    // FOURIER TRANSFORM
		    
		    if(GUI.MODE.equals("FOURIER")) {
			    transformed = new LinkedList<>();
			    toFourier(spliced, transformed);
		    }
		    
		} catch (Exception exp) {
		
		    exp.printStackTrace();
		
		} finally {

		    try {
		        ais.close();
		    } catch (Exception e) {
		    	
		    }
		    
		}
	}

	// getting 16 bit sample data
	private int getSixteenBitSample(int high, int low) {
	    return (high << 8) + (low & 0x00ff);
	} 
	
	// scales a 2d array down to the specified size of int[][] scaled
	private void scaleSamples(int[][] samples, int[][] scaled) {
		
		 for(int r = 0; r < scaled.length; r++) {
	    	for(double cScaled = 0; cScaled < scaled[0].length; cScaled += 1.0) {
	    		double cSamples = cScaled * (samples[0].length / scaled[0].length);
	    		if(cSamples >= samples[0].length) cSamples = samples[0].length - 1;
	    		scaled[r][(int)cScaled] = samples[r][(int)cSamples];
	    	}
		 }
		 
	}
	
	// recursive function that splices the original sample data into a LinkedList of segmented int[][], each representing a single frame
	private void spliceSamples(int[][] samples, LinkedList<int[][]> splicedOutput) {
		/* 
		 
		 
			given total duration in ms:
			17 ms between each frame
			each spliced arraylist should contain 17 ms of info
		 	
		 	each int[][] should be of (17 / soundDuration) * samples[0].length -> ~41 (home.wav) * samples length = 749.7
		
		 */
		double segmentLength = samples[0].length * GUI.FRAME_RATE * 1.1 / soundDurationMillis; // multiplier 1.075 on points <= 2, 1.06 for lines
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
	
	// creates a flat, cartesian, graph from amplitude sample data
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

	
	
	
	
	// graphs polar visual of amplitude sample data
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
	
	
	
	// graphs visual of fourier transform of sample data
	public void graphPolarTransformed(Graphics g) {
		//y values are already set, no need to change
		Complex[][] nextSegment = transformed.poll().clone();
		
		
		
		for(int r = 0; r < nextSegment.length; r++) {
			Color graphColor = new Color(Color.HSBtoRGB((float)(0.5 + 0.1 * r), 1, 1));
			g.setColor(graphColor);
			
			for(int c = 0; c < nextSegment[0].length - 1; c++) {
//				double radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / (transMax) * polarSemiRange);
				
////				if(radius < polarGraphRadius) {
////					radius = (double)polarGraphRadius + ((double)nextSegment[r][c] / Math.abs(sampleMin) * 200);
////					
////				}
//				double angleRad = (double)c / (nextSegment[0].length - 1) * Math.PI;
//				

//				

				
				//System.out.println("(" + x + ", " + y + ")");
				
//				double x = GUI.WIDTH / 2 + (nextSegment[r][c].re() / transRealMax) * (GUI.WIDTH / 2);
//				double y = GUI.HEIGHT / 2 - (nextSegment[r][c].im() / transImagMax) * (GUI.HEIGHT / 2);
				
				double angleRad = Math.atan(nextSegment[r][c].im() / nextSegment[r][c].re());
				double radius = (double)polarGraphRadius + ((double)nextSegment[r][c].abs() / transAbsMax * polarSemiRange);
				double y = GUI.HEIGHT / 2 - radius * Math.sin(angleRad);
				double x = GUI.WIDTH / 2 + radius * Math.cos(angleRad);
				double x1 = GUI.WIDTH / 2 - radius * Math.cos(angleRad);

				double radiusNext = (double)polarGraphRadius + ((double)nextSegment[r][c + 1].abs() / transAbsMax * polarSemiRange);
				double angleNext = angleRad +  Math.PI / nextSegment[0].length * 2; 
				
				

				//int size = (int)(3*radius / polarGraphRadius);

				int size = 3;
//				Graphics2D g2d = (Graphics2D)g.create();
//	            g2d.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//				g2d.drawLine((int)x, (int)y, (int)xNext, (int)yNext);
//				g2d.drawLine((int)x1, (int)y, (int)x1Next, (int)yNext);
				if((radius - polarGraphRadius)/ polarSemiRange <= 0.005) {
					g.setColor(new Color(graphColor.getRed(), graphColor.getGreen(), graphColor.getBlue(), 40));
					
				} else {
					radius = (double)polarGraphRadius + ((double)nextSegment[r][c].abs() / transAbsMax * polarSemiRange) / 2;
					double yNext = GUI.HEIGHT / 2 - radius * Math.sin(angleNext);
					double xNext = GUI.WIDTH / 2 + radius * Math.cos(angleNext);
					double x1Next = GUI.WIDTH / 2 - radius * Math.cos(angleNext);
					g.drawOval((int)xNext, (int)yNext, size, size);
					g.drawOval((int)x1Next, (int)yNext, size, size);


				}
				
				g.drawOval((int)x, (int)y, size, size);
				g.drawOval((int)x1, (int)y, size, size);

			}
		}
	}
	
	// derives the fourier transform from amplitude sample data
	private void toFourier(LinkedList<int[][]> target, LinkedList<Complex[][]> result) {
		//LinkedList<int[][]> result = new LinkedList<>();
		
		transRealMax = Double.MIN_VALUE;
		transImagMax = Double.MIN_VALUE;
		transAbsMax = Double.MIN_VALUE;
		
		for(int i = 0; i < target.size(); i++) {
			int[][] scaled = new int[target.get(i).length][512];
			scaleSamples(target.get(i), scaled);
			target.remove(i);
			target.add(i, scaled);
		}
		
		for(int i = 0; i < target.size(); i++) {
			
			int[][] curr = target.get(i).clone();
			Complex[][] segment = new Complex[curr.length][curr[0].length];
			for(int r = 0; r < curr.length; r++) {
				for(int c = 0; c < curr[0].length; c++) {
					segment[r][c] = new Complex(curr[r][c], 0);
				}
			}
			for(int r = 0; r < curr.length; r++) {
//				int avg = 0;
//				for(int c = 0; c < curr[0].length; c++) {
//					avg += Math.abs(curr[r][c]);
//				}
//				avg /= curr[0].length;
				
//				for(int c = 0; c < curr[0].length; c++) {
//					//double t = (i * curr[0].length + c + 1) / (target.size() * curr[0].length) * soundDurationMillis;
//					
//					//Math: avg * f(t) * e^(-2 * PI * i * f * t)
////					
////					Complex coord = new Complex(0, -2 * Math.PI / curr[0].length * c);
////					coord = coord.exp();
////					coord = coord.scale(avg * curr[r][c]);
////					
////					segment[r][c] = coord;
//					
//					//segment[r][c] = (int)(avg * curr[r][c] * Math.exp(-2 * Math.PI / curr[0].length * c));
//					//System.out.println(segment[r][c]);
////					 Tk += (T[l])*np.exp((-2j*np.pi*k*l)/N)
//					
//					// Code from stackoverflow:
//					Complex tC = new Complex(0, 0);
//					for(int k = 0; k < curr[0].length; k++) {
//						tC = tC.plus(new Complex(0, -2 * Math.PI * c * k).scale(1.0 / curr[0].length).exp().scale(curr[r][k]));
//						
//						//tC += curr[r][k] * Math.exp(-2 * Math.PI * c * k);
//					}
//					
//					segment[r][c] = tC;
////					
//					if(segment[r][c].re() > transRealMax) transRealMax = (int)segment[r][c].re();
//					if(segment[r][c].im() > transImagMax) transImagMax = (int)segment[r][c].im();
//					
//					if(segment[r][c].abs() > transAbsMax) {
//						
//						transAbsMax = segment[r][c].abs();
//						transRealMax = segment[r][c].re();
//						transImagMax = segment[r][c].im();
//					}
//					
//
//					//segment[r][c] *= c;
//					
//				}
				
				segment[r] = FFT.fft(segment[r]);
			}
			for(int r = 0; r < segment.length; r++) {
				for(int c = 0; c < segment.length; c++) {
					if(segment[r][c].re() > transRealMax) transRealMax = (int)segment[r][c].re();
					if(segment[r][c].im() > transImagMax) transImagMax = (int)segment[r][c].im();
					
					if(segment[r][c].abs() > transAbsMax) {
						
						transAbsMax = segment[r][c].abs();
						transRealMax = segment[r][c].re();
						transImagMax = segment[r][c].im();
					}
				}
			}
			result.add(segment);
		}
		
		System.out.println("done transforming");
	}
	
	// graphs entire sample data
	public void graph(Graphics g) {
		
		for(int r = 0; r < scaledSamples.length; r++) {
			g.setColor(new Color(Color.HSBtoRGB((float)(0.5 + 0.1 * r), 1, 1)));
			
			for(int x = 0; x < scaledSamples[0].length - 1; x++) {
				double y = (GUI.HEIGHT) / 2 - ((double)scaledSamples[r][x] / (scaledMax) * GUI.GRAPH_HEIGHT);
				double y1 = (GUI.HEIGHT) / 2 - ((double)scaledSamples[r][x+1] / (scaledMax) * GUI.GRAPH_HEIGHT);
				
				g.drawLine(x, (int)y, x+1, (int)y1);
			}
		}
		
		
	}
	
	// draws vertical progress marker from 0 -> GUI.WIDTH and spans 0 -> GUI.HEIGHT
	public void drawMarker(Graphics g) {
		g.setColor(new Color(0, 255, 0, 10));
		
		double playTimeMillis = System.currentTimeMillis() - startTimeMillis;
		int x = (int)(playTimeMillis / soundDurationMillis * GUI.GRAPH_WIDTH);
		
		Graphics2D g2d = (Graphics2D)g.create();
		g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(x, 0, x, GUI.HEIGHT);
	}
	
	double startTimeMillis;
	// plays soundtrack from media file
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






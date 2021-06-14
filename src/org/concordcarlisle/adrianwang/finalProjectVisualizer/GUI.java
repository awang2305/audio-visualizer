package org.concordcarlisle.adrianwang.finalProjectVisualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GUI extends JFrame implements ActionListener{
	
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 720;
	public static final int HEIGHT = 720;
	
	public static final int GRAPH_WIDTH = 1280;
	public static final int GRAPH_HEIGHT = 250;
	
	public static final int FRAME_RATE = 32;
	
	private final Color bgColor = Color.DARK_GRAY;

	private Visualizer vis;
	
	private Timer t;
	
	private boolean started;
	
	// graphing mode
	public static String MODE;
	
	// audio input file
	private File audioFile;
	
	
	public GUI() {
		
		started = false;
		
		// starting user interface
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// displaying JOptionPane for media input selection
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Choose an audio input");
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Audio Files (*.wav, *.midi)", "wav", "midi");
		fileChooser.setFileFilter(filter);
		
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
		    audioFile = fileChooser.getSelectedFile();
		} else {
			System.exit(0);
		}
		
		
		//displaying JOptionPane for graph mode selection
		String[] options = new String[] { "FOURIER", "POLAR", "FLAT"};
		
		int modeSelect = JOptionPane.showOptionDialog(this, "Select a graphing method:", "Mode Select", JOptionPane.YES_NO_CANCEL_OPTION, 
				JOptionPane.PLAIN_MESSAGE, null, options, 0);
		
		if(modeSelect == JOptionPane.YES_OPTION) MODE = "FOURIER";
		else if(modeSelect == JOptionPane.NO_OPTION) MODE = "POLAR";
		else if(modeSelect == JOptionPane.CANCEL_OPTION) MODE = "FLAT";
		else System.exit(0);
		
		if(MODE.equals("FOURIER")) {
			
			int w = JOptionPane.showOptionDialog(this, "Warning: the program may take up to 30 seconds to calculate the fourier transform.",
					"", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
			
			if(w == JOptionPane.CANCEL_OPTION) System.exit(0);
		}
		
		// starting the visualizer
		this.startVis(audioFile);
	}
	
	private void startVis(File file) {
		started = true;
		vis = new Visualizer(file);
		
		//init JFrame
		this.setSize(WIDTH, HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.setBackground(bgColor);
		
		this.setVisible(true);
		
		

		startTimer();
		vis.playAudio(); // plays soundtrack from media file
	}
	private void startTimer() {
		t = new Timer((int)(FRAME_RATE / 1.075), this);
		t.setActionCommand("TICK");
		t.start();
		
	}
	
	@Override
	public void paint(Graphics g) {
		
		if(started) {
			g.clearRect(0, 0, WIDTH, HEIGHT);
			
			if(MODE.equals("FLAT")) vis.graphNext(g);
			if(MODE.equals("POLAR")) vis.graphPolar(g);
			if(MODE.equals("FOURIER")) vis.graphPolarTransformed(g);
			
		}
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("TICK")) {
			this.repaint();
		}
		
	}
}

package org.concordcarlisle.adrianwang.finalProjectVisualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.Timer;

public class GUI extends JFrame implements ActionListener{
	
	private static final long serialVersionUID = 1L;
	
	public static final int WIDTH = 1280;
	public static final int HEIGHT = 720;
	
	public static final int GRAPH_WIDTH = 1280;
	public static final int GRAPH_HEIGHT = 250;
	
	public static final int FRAME_RATE = 33;

	private Visualizer vis;
	
	private Timer t;
	
	public GUI() {
		
		//System.out.println(vis);
		vis = new Visualizer();
		
		//init JFrame
		
		this.setSize(WIDTH, HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Swing");
		
		this.setBackground(Color.DARK_GRAY);
		
		this.setVisible(true);
		
		

		startTimer();
		vis.playAudio();
		//vis.graph(this.getGraphics());

	}
	
	private void startTimer() {
		t = new Timer(FRAME_RATE, this);
		t.setActionCommand("TICK");
		t.start();
		
	}
	
	@Override
	public void paint(Graphics g) {
		g.clearRect(0, 0, WIDTH, HEIGHT);
		//vis.drawMarker(g);
		//vis.graphNext(g);
		vis.graphPolar(g);
		//vis.graphPolarTransformed(g);
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("TICK")) {
			this.repaint();
		}
		
	}
}

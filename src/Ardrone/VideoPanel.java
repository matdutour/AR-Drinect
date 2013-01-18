package Ardrone;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class VideoPanel extends JPanel{
	private static final long serialVersionUID = -7635284252404123776L;
	private BufferedImage image=null;
	
	public VideoPanel ()
	{
		super();
		this.setPreferredSize(new Dimension(650,400));
	}
	
	public void setImage(BufferedImage image){
		this.image=image;
	}
	
	public void paint(Graphics g){
		g.setColor(Color.white);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		if(image!=null)
			g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
	}
}

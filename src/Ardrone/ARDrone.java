package Ardrone;
/*
Author: MAPGPS on https://projects.ardrone.org
Initial: 2010.09.20
Updated: 2010.09.24

UI_BIT:
00010001010101000000000000000000
   |   | | | |        || | ||||+--0: Button turn to left
   |   | | | |        || | |||+---1: Button altitude down (ah - ab)
   |   | | | |        || | ||+----2: Button turn to right
   |   | | | |        || | |+-----3: Button altitude up (ah - ab)
   |   | | | |        || | +------4: Button - z-axis (r1 - l1)
   |   | | | |        || +--------6: Button + z-axis (r1 - l1)
   |   | | | |        |+----------8: Button emergency reset all
   |   | | | |        +-----------9: Button Takeoff / Landing
   |   | | | +-------------------18: y-axis trim +1 (Trim increase at +/- 1??/s)
   |   | | +---------------------20: x-axis trim +1 (Trim increase at +/- 1??/s)
   |   | +-----------------------22: z-axis trim +1 (Trim increase at +/- 1??/s)
   |   +-------------------------24: x-axis +1
   +-----------------------------28: y-axis +1

AT*REF=<sequence>,<UI>
AT*PCMD=<sequence>,<enable>,<pitch>,<roll>,<gaz>,<yaw>
	(float)0.01 = (int)1008981770		(float)-0.01 = (int)-1138501878
	(float)0.05 = (int)1028443341		(float)-0.05 = (int)-1119040307
	(float)0.1  = (int)1036831949		(float)-0.1  = (int)-1110651699
	(float)0.2  = (int)1045220557		(float)-0.2  = (int)-1102263091
	(float)0.5  = (int)1056964608		(float)-0.5  = (int)-1090519040
AT*ANIM=<sequence>,<animation>,<duration>
AT*CONFIG=<sequence>,\"<name>\",\"<value>\"

altitude max2m:	java ARDrone 192.168.1.1 AT*CONFIG=1,\"control:altitude_max\",\"2000\"
Takeoff:	java ARDrone 192.168.1.1 AT*REF=101,290718208
Landing:	java ARDrone 192.168.1.1 AT*REF=102,290717696
Hovering:	java ARDrone 192.168.1.1 AT*PCMD=201,1,0,0,0,0
gaz 0.1:	java ARDrone 192.168.1.1 AT*PCMD=301,1,0,0,1036831949,0
gaz -0.1:	java ARDrone 192.168.1.1 AT*PCMD=302,1,0,0,-1110651699,0
pitch 0.1:	java ARDrone 192.168.1.1 AT*PCMD=303,1,1036831949,0,0,0
pitch -0.1:	java ARDrone 192.168.1.1 AT*PCMD=304,1,-1110651699,0,0,0
yaw 0.1:	java ARDrone 192.168.1.1 AT*PCMD=305,1,0,0,0,1036831949
yaw -0.1:	java ARDrone 192.168.1.1 AT*PCMD=306,1,0,0,0,-1110651699
roll 0.1:	java ARDrone 192.168.1.1 AT*PCMD=307,1,0,1036831949,0,0
roll -0.1:	java ARDrone 192.168.1.1 AT*PCMD=308,1,0,-1110651699,0,0
pitch -30 deg:	java ARDrone 192.168.1.1 AT*ANIM=401,0,1000
pitch 30 deg:	java ARDrone 192.168.1.1 AT*ANIM=402,1,1000
*/

import java.net.*;
import java.util.*;

public class ARDrone {
	// L'ip est publique : on peut la changer entre deux connexions ...
	public String ip = "192.168.1.1";
	public Navdata navdata = new Navdata();
	public String dernierCmd = "";
	private InetAddress inet_addr;
	private DatagramSocket socketCmd;
	public DatagramSocket socketNavdata;
	private Socket socketVideo;
	public int seq = 1;
	public VideoPanel vpanel;

	public ARDrone (VideoPanel vpanel) throws Exception
	{
		this.vpanel=vpanel;
		System.out.println("Lancement ARDrone ...");
	}
	
	public void init () throws Exception
	{
		String cmd;
		
		// On réécrit l'ip dans un objet InetAddress
		StringTokenizer st = new StringTokenizer(ip, ".");
		byte[] ip_bytes = new byte[4];
		for (int i = 0; i < 4; i++)
		{
			ip_bytes[i] = (byte)Integer.parseInt(st.nextToken());
		}
		inet_addr = InetAddress.getByAddress(ip_bytes);

		// Init sockets
		socketCmd = new DatagramSocket(5556);
		socketCmd.setSoTimeout(3000);
		socketNavdata = new DatagramSocket(5554);
		socketNavdata.setSoTimeout(3000);
		socketVideo = new Socket(inet_addr, 5555);
		socketVideo.setSoTimeout(3000);

		// Initialisation de la rï¿½ception des navdata (cf p.41 du pdf)
		byte[] buffer = {01, 00, 00, 00};
		DatagramPacket tickle = new DatagramPacket(buffer, buffer.length, inet_addr, 5554);

		cmd = "AT*CONFIG="+seq+",\"general:navdata_demo\",\"TRUE\"";
		seq++;
		send(cmd);
		
		cmd = "AT*FTRIM="+(seq++);
		send(cmd);
		
		socketNavdata.send(tickle);
		
		cmd = "AT*PMODE="+(seq++)+",2";
		send(cmd);
		
		cmd = "AT*MISC="+(seq++)+",20,2000,3000";
		send(cmd);
		
		cmd = "AT*FTRIM="+(seq++);
		send(cmd);
		
		/*cmd = "AT*CTRL="+seq+",5,0";
		seq++;
		send(cmd);*/

		// Init du thread watchdog + rï¿½cup navdata
		ThreadNavdata Tn = new ThreadNavdata("TNavdata", this);
		Tn.start();
		
		socketVideo.getOutputStream().write(buffer);
		
		cmd = "AT*CONFIG="+(seq++)+",\"general:video_enable\",\"TRUE\"";
		send(cmd);
		
		socketVideo.getOutputStream().write(buffer);
		
		cmd = "AT*CONFIG="+(seq++)+",\"video:bitrate_ctrl_mode\",\"0\"";
		send(cmd);
		
		ThreadVideo Tv = new ThreadVideo(socketVideo, vpanel);
		Tv.start();
		
		// Horizontal trim
		//cmd = "AT*FTRIM="+(seq++);
		//send(cmd);
		
		//Runtime.getRuntime().exec("C:/ffmpeg/bin/ffplay tcp://192.168.1.1:5555");
		System.out.println("\tARDrone pret.");
	}
	
	public void send(String str) throws Exception // On pourrait passer le port en argument ...
	{
		byte[] buffer = (str + "\r").getBytes();
		DatagramPacket packet = new DatagramPacket(buffer,buffer.length,inet_addr,5556);
		socketCmd.send(packet);
		this.dernierCmd = str;
	}

	public void emergency() throws Exception
	{
		while (!this.navdata.emergency())
		{
			String str = "AT*REF=" + (seq++) + ",290717952";
			send(str);
			Thread.sleep(5);
		}
		System.out.println("\t["+seq+"] Atterissage d'urgence reussi !");
	}

	public void decoller() throws Exception
	{
		while (!this.navdata.flying())
		{
			String str = "AT*REF=" + (seq++) + ",290718208";
			send(str);
			Thread.sleep(5);
		}
		System.out.println("\t["+seq+"] Decollage réussi !");
	}

	public void atterrir() throws Exception
	{
		while (this.navdata.flying())
		{
			String str = "AT*REF=" + (seq++) + ",290717696";
			send(str);
			Thread.sleep(5);
		}
		System.out.println("\t["+seq+"] Atterrissage russi !");
	}

	/* Dï¿½placement : angles phi (horizontal latï¿½ral), theta (horizontal avant/arriï¿½re) et vitesses z (verticale), psi (angulaire verticale)
	 * Les arguments sont des pourcentages de l'angle (la vitesse) maximal ([-1 ; 1]) */
	public void deplacement(float phi, float theta, float vspeed, float aspeed) throws Exception
	{
		Boolean CombinedYaw = false;
		int phi2 = (phi>-0.1 && phi<0.1) ? 0 : Float.floatToIntBits(phi);
		int theta2 = (theta>-0.1 && theta<0.1) ? 0 : Float.floatToIntBits(theta);
		int vspeed2 = (vspeed>-0.1 && vspeed<0.1) ? 0 : Float.floatToIntBits(vspeed);
		int aspeed2 = (aspeed>-0.1 && aspeed<0.1) ? 0 : Float.floatToIntBits(aspeed);
		String str = "AT*PCMD=" + seq + "," + (CombinedYaw ? 3 : 1) + ","+phi2+","+theta2+","+vspeed2+","+aspeed2;
		seq++;
		send(str);
	}
	
	public void stationnaire() throws Exception
	{
		String s = "AT*PCMD="+ (seq++) +",1,0,0,0,0";
		send(s);
	}
	
	public boolean enVol ()
	{
		return this.navdata.flying();
	}

}

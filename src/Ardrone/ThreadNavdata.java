package Ardrone;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadNavdata extends Thread {
    private ARDrone drone;

    public ThreadNavdata(String name, ARDrone ardrone)
    {
    	super(name);
        this.drone = ardrone;
    }

    @Override
    public void run()
    {
	while(true)
	{
	    // Reset Watchdog
	    String str = "AT*COMWDG="+this.drone.seq;
	    drone.seq++;
	    try {
	    	drone.send(str);
	    } catch (Exception ex) {
	    	Logger.getLogger(ThreadNavdata.class.getName()).log(Level.SEVERE, null, ex);
	    }

	    // Receive navdata
	    byte[] buffer = new byte[1024];
	    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    //System.out.println("Tentative de réception navdata "+this.i+" :");
	    try {
			this.drone.socketNavdata.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    //System.out.println("\t"+String.format("%02X", data[1]));
	    this.drone.navdata.setData(ByteBuffer.wrap(packet.getData(), 0, packet.getLength()));
	    //this.drone.navdata.printState();

	    try {
	    	sleep(10);
	    } catch (InterruptedException ex) {
		Logger.getLogger(ThreadNavdata.class.getName()).log(Level.SEVERE, null, ex);
	    }
	}
    }
}

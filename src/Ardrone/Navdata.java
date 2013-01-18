package Ardrone;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Navdata {
	private ByteBuffer data;
	private int state;
	private ByteBuffer option;
	public boolean recu;
	
	public Navdata()
	{
		this.recu = false;
	}
	
	public void setData (ByteBuffer data)
	{
		data.order(ByteOrder.LITTLE_ENDIAN);
		this.data = data;
		int head = this.data.getInt();
		//assert(head == 0x55667788);
		this.state = this.data.getInt();
		int sequence = this.data.getInt();
		int vision = this.data.getInt();
		
		while(this.data.position() < this.data.limit())
		{
			int tag = this.data.getShort() & 0xFFFF;
			int payloadSize = (this.data.getShort() & 0xFFFF)-4;
			if (tag == 0)
			{
				this.option = this.data.slice().order(ByteOrder.LITTLE_ENDIAN);
				this.option.limit(payloadSize);
			}
			this.data.position(this.data.position()+payloadSize);
		}
		this.recu = true;
	}
	
	public boolean flying()
	{
		return ((this.state & 1) == 1) ? true : false;
	}
	
	public boolean emergency()
	{
		return ((this.state  >> 31) & 1) == 1 ? true : false;
	}
	
	public boolean flagCmd()
	{
		return ((this.state >> 6) & 1) == 1 ? true : false;
	}
	
	public int getBattery ()
	{
		return this.option.getInt(4);
	}
	
	public float getTheta ()
	{
		return this.option.getFloat(8)/1000;
	}
	
	public float getPhi ()
	{
		return this.option.getFloat(12)/1000;
	}
	
	public float getPsi ()
	{
		return this.option.getFloat(16)/1000;
	}
	
	public int getAlt ()
	{
		return this.option.getInt(20)/1000;
	}
	
	public float getVx ()
	{
		return this.option.getFloat(24);
	}
	
	public float getVy ()
	{
		return this.option.getFloat(28);
	}
	
	public String getState ()
	{
		String s = "";
		
		for (int i = 0; i<32; i++)
		{
			s += ((this.state >> i) & 1);
			if (i%8 == 7 && i!=31) s += " ";
		}
		return s;
	}
}

package controle;

import org.OpenNI.SkeletonJoint;

import Ardrone.ARDrone;
import UsersViewer3D.Joint3D;
import UsersViewer3D.SkelsManager;

public class Controle {
	private double xOrigin = 0;
	private double yOrigin;
	private double zOrigin;
	private double zOriginRef;
	private double tetaOriginR;
	private double tetaOriginL;
	public double x;
	public double y;
	public double z;
	public double omegaX;
	public double omegaY;
	public double omegaZ;
	public boolean initialized=false;
	private ARDrone drone;
	private int timer=0;
	
	public Controle(ARDrone drone){
		this.drone=drone;
		
	}
	
	public void init(SkelsManager skels){
		if(skels.getMember(SkeletonJoint.LEFT_HAND)!=null && skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos()!=null && skels.getMember(SkeletonJoint.RIGHT_SHOULDER)!=null && skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos()!=null && skels.getMember(SkeletonJoint.LEFT_SHOULDER)!=null && skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos()!=null && !drone.enVol())
		{
			xOrigin=skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getX();
			yOrigin=skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getY();
			
			
			tetaOriginR=skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getZ();
			
			tetaOriginL=skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getZ();
			zOriginRef=(skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getZ()-tetaOriginR);
			zOrigin=skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getZ()+zOriginRef/2;
			System.out.println("Initialise");
			initialized=true;
		}
	}
	
	public void update(SkelsManager skels){
		if(skels.getMember(SkeletonJoint.LEFT_HAND)==null || skels.getMember(SkeletonJoint.RIGHT_HAND)==null || skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos()==null ||skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos()==null || !initialized || skels.getMember(SkeletonJoint.LEFT_SHOULDER)==null || skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos()==null || skels.getMember(SkeletonJoint.RIGHT_SHOULDER)==null || skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos()==null || skels.getMember(SkeletonJoint.HEAD)==null || skels.getMember(SkeletonJoint.HEAD).getKinectPos()==null)
			 {}
		else {
			if((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getX()-xOrigin)/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX())>1 || (skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getX()-xOrigin)/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX())<-1)
				x=-signe((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getX()-xOrigin)/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()));
			else
				x=-(skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getX()-xOrigin)/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX());
			
			if((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getY()-yOrigin)/(2*(skels.getMember(SkeletonJoint.HEAD).getKinectPos().getY()-skels.getMember(SkeletonJoint.NECK).getKinectPos().getY()))>1 || (skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getY()-yOrigin)/(2*(skels.getMember(SkeletonJoint.HEAD).getKinectPos().getY()-skels.getMember(SkeletonJoint.NECK).getKinectPos().getY()))<-1)
				y=signe((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getY()-yOrigin)/(2*(skels.getMember(SkeletonJoint.HEAD).getKinectPos().getY()-skels.getMember(SkeletonJoint.NECK).getKinectPos().getY())));
			else
				y=(skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getY()-yOrigin)/(2*(skels.getMember(SkeletonJoint.HEAD).getKinectPos().getY()-skels.getMember(SkeletonJoint.NECK).getKinectPos().getY()));
			
			if((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getZ()-zOrigin)/zOriginRef>1 || (skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getZ()-zOrigin)/zOriginRef<-1)
				z=signe((skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getZ()-zOrigin)/zOriginRef);
			else
				z=(skels.getMember(SkeletonJoint.LEFT_HAND).getKinectPos().getZ()-zOrigin)/zOriginRef*4;
			
			if((skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getX())/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX())>1 || (skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getX())/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX())<-1)
				omegaY=signe((skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getX())/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()));
			else
				omegaY=(skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getX())/(skels.getMember(SkeletonJoint.LEFT_SHOULDER).getKinectPos().getX()-skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getX());	
			try {
					if(drone.enVol())
					{
						timer+=1;
						if(timer>100)
							drone.deplacement((float)(x/5), (float)(-z/5), (float)(y/5), (float)(omegaY/4));
					}
				} 
			
			catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
		if(skels.getMember(SkeletonJoint.RIGHT_HAND)!=null && skels.getMember(SkeletonJoint.RIGHT_SHOULDER)!=null && skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos()!=null && skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos()!=null && initialized)
		{
			if(skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getY()>skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getY() && drone.enVol() && timer>100)
			{
				System.out.println("Atterrisage");
				try {
					drone.atterrir();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(skels.getMember(SkeletonJoint.RIGHT_HAND).getKinectPos().getY()>skels.getMember(SkeletonJoint.RIGHT_SHOULDER).getKinectPos().getY() && !drone.enVol())
			{
				System.out.println("Decole");
				try {
					drone.decoller();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	private double signe(double d)
	{
		if(d>0)
			return 1;
		else return -1;
	}
}

package UsersViewer3D;

// SkelsManager.java
// Andrew Davison, October 2011, ad@fivedots.psu.ac.th

/* Manages the 3D skeletons in the scene.

   SkelsManager sets up 7 'observers' (listeners) so that 
   when a new user is detected in the scene, a standard pose for that 
   user is detected, the user skeleton is calibrated in the pose, and then the
   skeleton is tracked. 

   The start of tracking adds a 3D skeleton entry to userSkels3D, and attaches
   its scene graph to the 3D scene.

   The skeleton can be made invisible when the user exits, and visible
   again when they return to the Kinect's FOV. If the user is lost then the
   skeleton is deleted from the scene graoh and from userSkels3D.

   Each call to update() updates the 3D skeleton for each user
   via its Skeleton3D object
*/

import java.io.*;
import java.util.*;

import org.OpenNI.*;

import javax.media.j3d.*;



public class SkelsManager
{
  // OpenNI
  private UserGenerator userGen;

  // capabilities used by UserGenerator
  private SkeletonCapability skelCap;
        // to output skeletal data, including the location of the joints
  private PoseDetectionCapability poseDetectionCap;
        // to recognize when the user is in a specific position

  private String calibPose = null;

  // Java3D
  public HashMap<Integer, Skeleton3D> userSkels3D;
    // maps user IDs --> a 3D skeleton

  private BranchGroup sceneBG;    // the scene graph



  public SkelsManager(UserGenerator userGen, BranchGroup sceneBG)
  {
    this.userGen = userGen;
    this.sceneBG = sceneBG;

    configure();

    userSkels3D = new HashMap<Integer, Skeleton3D>();
  } // end of SkelsManager()



  private void configure()
  /* create pose and skeleton detection capabilities for the user generator, 
     and set up observers (listeners)   */
  {
    try {
      // setup UserGenerator pose and skeleton detection capabilities;
      // should really check these using ProductionNode.isCapabilitySupported()
      poseDetectionCap = userGen.getPoseDetectionCapability();

      skelCap = userGen.getSkeletonCapability();
      calibPose = skelCap.getSkeletonCalibrationPose();   // the 'psi' pose
      skelCap.setSkeletonProfile(SkeletonProfile.ALL);
             // other possible values: UPPER_BODY, LOWER_BODY, HEAD_HANDS


      // set up 7 observers
      userGen.getNewUserEvent().addObserver(new NewUserObserver());  // new user found
      userGen.getLostUserEvent().addObserver(new LostUserObserver());  // lost a user
      userGen.getUserExitEvent().addObserver(new ExitUserObserver());        // user has exited (but may re-enter)
      userGen.getUserReenterEvent().addObserver(new ReEnterUserObserver());  // user has re-entered

      poseDetectionCap.getPoseDetectedEvent().addObserver( new PoseDetectedObserver());    // for when a pose is detected

      skelCap.getCalibrationStartEvent().addObserver( new CalibrationStartObserver());    // calibration is starting
      skelCap.getCalibrationCompleteEvent().addObserver( new CalibrationCompleteObserver());   
             // for when skeleton calibration is completed, and tracking starts
    } 
    catch (Exception e) {
      System.out.println(e);
      System.exit(1);
    }
  }  // end of configure()


public Joint3D getMember(SkeletonJoint sj){
	try {   
	      int[] userIDs = userGen.getUsers();     // there may be many users in the scene
	      for (int i = 0; i < userIDs.length; i++) {
	        int userID = userIDs[i];
	        if (skelCap.isSkeletonCalibrating(userID))
	          continue;    // test to avoid occassional crashes with isSkeletonTracking()
	        if (skelCap.isSkeletonTracking(userID))
	        {
	          return userSkels3D.get(userID).getMember(sj);
	        }
	      }
	    }
	    catch (StatusException e) 
	    {  System.out.println(e); }
	return null;
}

public Joint3D[] getLeftHandAndShoulder(){
	try {   
	      int[] userIDs = userGen.getUsers();     // there may be many users in the scene
	      for (int i = 0; i < userIDs.length; i++) {
	        int userID = userIDs[i];
	        if (skelCap.isSkeletonCalibrating(userID))
	          continue;    // test to avoid occassional crashes with isSkeletonTracking()
	        if (skelCap.isSkeletonTracking(userID))
	        {
	          return userSkels3D.get(userID).getLeftHandAndShoulder();
	        }
	      }
	    }
	    catch (StatusException e) 
	    {  System.out.println(e); }
	return null;
}

  public void update()
  // update skeleton of each user being tracked
  {
    try {   
      int[] userIDs = userGen.getUsers();     // there may be many users in the scene
      for (int i = 0; i < userIDs.length; i++) {
        int userID = userIDs[i];
        if (skelCap.isSkeletonCalibrating(userID))
          continue;    // test to avoid occassional crashes with isSkeletonTracking()
        if (skelCap.isSkeletonTracking(userID))
          userSkels3D.get(userID).update();
      }
    }
    catch (StatusException e) 
    {  System.out.println(e); }
  }  // end of update()


  // ----------------- 7 observers -----------------------
  /*   user detection --> pose detection --> skeleton calibration starts -->
       skeleton calibration finish --> skeleton tracking 
       (causes the creation of userSkels3D entry + scene graph)

    +  exit --> re-entry of user
       (3D skeleton is made invisible/visible)

    +  lose a user  
       (causes the deletion of its userSkels3D entry + scene graph)
  */


  class NewUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    {
      System.out.println("Nouvel utilisateur detecte " + args.getId());
      try {
        // try to detect a pose for the new user
        poseDetectionCap.StartPoseDetection(calibPose, args.getId());
      }
      catch (StatusException e)
      { e.printStackTrace(); }
    }
  }  // end of NewUserObserver inner class



  class LostUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    { 
      int userID = args.getId();
      System.out.println("Utilisateur perdu " + userID);

      // delete skeleton from userSkels3D and the scene graph
      Skeleton3D skel = userSkels3D.remove(userID);
      if (skel == null)
        return;
      skel.delete();
    }
  } // end of LostUserObserver inner class



  class ExitUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    { 
      int userID = args.getId();
      System.out.println("Utilisateur sorti " + userID);

      // make 3D skeleton invisible when user exits
      Skeleton3D skel = userSkels3D.get(userID);
      if (skel == null)
        return;
      skel.setVisibility(false);
    }
  } // end of ExitUserObserver inner class



  class ReEnterUserObserver implements IObserver<UserEventArgs>
  {
    public void update(IObservable<UserEventArgs> observable, UserEventArgs args)
    { 
      int userID = args.getId();
      System.out.println("Utilisateur revenu " + userID);

      // make 3D skeleton visible when user re-enters
      Skeleton3D skel = userSkels3D.get(userID);
      if (skel == null)
        return;
      skel.setVisibility(true);
    }
  } // end of ReEnterUserObserver inner class



  class PoseDetectedObserver implements IObserver<PoseDetectionEventArgs>
  {
    public void update(IObservable<PoseDetectionEventArgs> observable,
                                                     PoseDetectionEventArgs args)
    {
      int userID = args.getUser();
      System.out.println(args.getPose() + " pose detectee pour utilisateur " + userID);
      try {
        // finished pose detection; switch to skeleton calibration
        poseDetectionCap.StopPoseDetection(userID);
        skelCap.requestSkeletonCalibration(userID, true);
      }
      catch (StatusException e)
      {  e.printStackTrace(); }
    }
  }  // end of PoseDetectedObserver inner class




  class CalibrationStartObserver implements IObserver<CalibrationStartEventArgs>
  {
    public void update(IObservable<CalibrationStartEventArgs> observable,
                                        CalibrationStartEventArgs args)
    { System.out.println("Calibration demarree pour utilisateur " + args.getUser());  }
  }  // end of CalibrationStartObserver inner class



  class CalibrationCompleteObserver implements IObserver<CalibrationProgressEventArgs>
  {
    public void update(IObservable<CalibrationProgressEventArgs> observable,
                                                    CalibrationProgressEventArgs args)
    {
      int userID = args.getUser();
      System.out.println("Calibration : " + args.getStatus() + " pour utilisateur " + userID);
      try {
        if (args.getStatus() == CalibrationProgressStatus.OK) {
          // calibration succeeeded; move to skeleton tracking
          System.out.println("Debut tracking utilisateur " + userID);
          skelCap.startTracking(userID);

          // create skeleton3D in userSkels3D, and add to scene
          Skeleton3D skel = new Skeleton3D(userID, skelCap);
          userSkels3D.put(userID, skel);
          sceneBG.addChild( skel.getBG() );
        }
        else   // calibration failed; return to pose detection
          poseDetectionCap.StartPoseDetection(calibPose, userID);
      }
      catch (StatusException e)
      {  e.printStackTrace(); }
    }
  }  // end of CalibrationCompleteObserver inner class

  
} // end of SkelsManager class


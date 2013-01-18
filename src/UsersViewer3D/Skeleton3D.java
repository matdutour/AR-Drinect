package UsersViewer3D;

// Skeleton3D.java
// Andrew Davison, October 2011, ad@fivedots.psu.ac.th

/* Creates the Joint3D and Limb3D objects representing a 
   skeleton. Connects them into a scene graph for the skeleton.

  The scene graph for the skeleton:

      skelBG-->visSW-->moveTG-->partsBG
                                |
                                --> joints
                                --> limbs

    visSW controls the skeleton's visibility;

    moveTG allows the skeleton to be moved as a single object (useful
    for positioning it in  the scene with Y_OFFSET and Z_OFFSET)

    The joints and limbs are updated when update() is called.

    It is possible to make a skeleton invisible or completely delete it
    by detaching the entire graoh from the scene.
*/

import java.util.*;

import org.OpenNI.*;

import javax.media.j3d.*;
import javax.vecmath.*;



public class Skeleton3D
{
  // scaling from Kinect coords to 3D scene coords
  private static final float XY_SCALE = 1/500.0f;
  private static final float Z_SCALE = -1/1000.0f;

  // positioning of skeleton so feet rest on the checkerboard floor
  private static final float Y_OFFSET = 2.5f;
  private static final float Z_OFFSET = 5.0f;

  private static final float LIMB_RADIUS = 0.1f;


  /*  the skeleton is a series of limbs;
      a limb is defined by a pair of joints which are 
      listed in jointPairs[]
  */
  private SkeletonJoint[] jointPairs =
      { SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.LEFT_ELBOW,
        SkeletonJoint.LEFT_ELBOW, SkeletonJoint.LEFT_HAND,      // left arm

        SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.RIGHT_ELBOW,
        SkeletonJoint.RIGHT_ELBOW, SkeletonJoint.RIGHT_HAND,    // right arm

        SkeletonJoint.HEAD, SkeletonJoint.NECK,
        SkeletonJoint.NECK, SkeletonJoint.LEFT_SHOULDER,                       
        SkeletonJoint.NECK, SkeletonJoint.RIGHT_SHOULDER,      // upper body

        SkeletonJoint.LEFT_SHOULDER, SkeletonJoint.TORSO,
        SkeletonJoint.RIGHT_SHOULDER, SkeletonJoint.TORSO,    // torso
        SkeletonJoint.LEFT_HIP, SkeletonJoint.TORSO,
        SkeletonJoint.RIGHT_HIP, SkeletonJoint.TORSO,

        SkeletonJoint.LEFT_HIP, SkeletonJoint.RIGHT_HIP,      // across hips

        SkeletonJoint.LEFT_HIP, SkeletonJoint.LEFT_KNEE,
        SkeletonJoint.LEFT_KNEE, SkeletonJoint.LEFT_FOOT,     // left leg

        SkeletonJoint.RIGHT_HIP, SkeletonJoint.RIGHT_KNEE,
        SkeletonJoint.RIGHT_KNEE, SkeletonJoint.RIGHT_FOOT    // right leg
      };


  // collections of joints and limbs making up the skeleton
  private ArrayList<Joint3D> joints3D;
  private ArrayList<Limb3D> limbs;


  private BranchGroup skelBG;      // top of the skeleton graph
  // private BranchGroup partsBG;     // where all the joints and limbs are attached

  private Switch visSW;            // for skeleton visibility
  private boolean isVisible;



  public Skeleton3D(int userID, SkeletonCapability skelCap)
  {
    // create top of scene graph for the skeleton
    BranchGroup partsBG = buildSkelGraph();  
         // all the joints and limbs are attached to partsBG

    HashMap<SkeletonJoint, Joint3D> jointsMap = 
                        new HashMap<SkeletonJoint, Joint3D>();
        // used for looking up joints when I create the limbs

    // build joints
    joints3D = new ArrayList<Joint3D>();
    buildJoints(joints3D, userID, skelCap, partsBG, jointsMap);

    // build limbs
    limbs = new ArrayList<Limb3D>();
    buildLimbs(limbs, partsBG, jointsMap);

    skelBG.compile();
  } // end of Skeleton3D()



  private BranchGroup buildSkelGraph()
  // creates the top-level scene graph for the skeleton
  {
    // skelBG-->visSW-->moveTG-->partsBG
    BranchGroup partsBG = new BranchGroup();
    partsBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    partsBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

    Transform3D t3d = new Transform3D();
    t3d.set(new Vector3f(0, Y_OFFSET, Z_OFFSET));   // so feet are on the floor
    TransformGroup moveTG = new TransformGroup(t3d);
    moveTG.addChild(partsBG);

    // create switch for visibility
    visSW = new Switch();
    visSW.setCapability(Switch.ALLOW_SWITCH_WRITE);
    visSW.addChild( moveTG );
    visSW.setWhichChild( Switch.CHILD_ALL);   // visible initially
    isVisible = true;

    skelBG = new BranchGroup();
    skelBG.setCapability(BranchGroup.ALLOW_DETACH);   // so can be 'destroyed'
    skelBG.addChild(visSW);

    return partsBG;    // where all the joints and limbs are attached
  }  // end of buildSkelGraph()


  public BranchGroup getBG()
  {  return skelBG; }



  private void buildJoints(ArrayList<Joint3D> joints3D, int userID,
                          SkeletonCapability skelCap,
                          BranchGroup partsBG,
                          HashMap<SkeletonJoint, Joint3D> jointsMap)
  /* create Joint3D objects, and connect their scene 
     graphs to the skeleton at partsBG  */
  {
    Joint3D j3d;
    for(SkeletonJoint joint : SkeletonJoint.values()) {
      j3d = buildJoint3D(joint, userID, skelCap);
      if (j3d != null) {
        partsBG.addChild( j3d.getTG() );
        joints3D.add(j3d);
        jointsMap.put(joint, j3d);   // build map for later
      }
    }
  }  // end of buildJoints()



  private Joint3D buildJoint3D(SkeletonJoint joint, int userID,
                         SkeletonCapability skelCap)
  // create a Joint3D object
  {
    if (!skelCap.isJointAvailable(joint) || !skelCap.isJointActive(joint)) {
      /* To deal with absense of WAIST,
         LEFT_COLLAR, LEFT_WRIST, LEFT_FINGER_TIP, LEFT_ANKLE,
         RIGHT_COLLAR, RIGHT_WRIST, RIGHT_FINGER_TIP, RIGHT_ANKLE
      */
      // System.out.println(joint + " not available");
      return null;
    }

    Joint3D j3d;
    if (joint == SkeletonJoint.HEAD)
      j3d = new Joint3D(joint, 0.22f, XY_SCALE, Z_SCALE, 
                                           userID, skelCap);   // bigger head
    else
      j3d = new Joint3D(joint, XY_SCALE, Z_SCALE, userID, skelCap);

    return j3d;
  }  // end of buildJoint3D()



  private void buildLimbs(ArrayList<Limb3D> limbs,
                          BranchGroup partsBG,
                          HashMap<SkeletonJoint, Joint3D> jointsMap)
  /* create Limb3D objects, and connect their scene graphs to the skeleton
     at partsBG.
     Each limb refers to two Joint3D objects, as specified in jointPairs[]
  */
  {
    Limb3D limb;
    Joint3D startJ3d, endJ3d;
    for (int i=0; i < jointPairs.length; i=i+2) {
      startJ3d = getJoint3D(jointPairs[i], jointsMap);
      endJ3d = getJoint3D(jointPairs[i+1], jointsMap);
      if ((startJ3d != null) && (endJ3d != null)) {
        limb = new Limb3D(startJ3d, endJ3d, LIMB_RADIUS);
        partsBG.addChild(limb.getTG());
        limbs.add(limb);
      }
    }
  }  // end of buildLimbs()



  private Joint3D getJoint3D(SkeletonJoint joint,
                          HashMap<SkeletonJoint, Joint3D> jointsMap)
  {
    Joint3D j3d = jointsMap.get(joint);
    if (j3d == null) {
      System.out.println("Undefined " + joint);
      return null;
    }
    return j3d;
  }  // end of getJoint3D()



  public void update()
  //  Update the skeleton's joints and limbs if the skeleton is visible
  {
    if (!isVisible)
      return;

    // update joints
    for(Joint3D j3d : joints3D)
    {
      j3d.update();
    }

    // update limbs
    for(Limb3D limb : limbs)
      limb.update();
  }  // end of update()



  public void setVisibility(boolean toVisible)
  // affect the visisbility of the entire skeleton
  {  
    if (toVisible) {
      visSW.setWhichChild(Switch.CHILD_ALL);   // make visible
      isVisible = true;
    }
    else {   // make invisible
      visSW.setWhichChild( Switch.CHILD_NONE );   // invisible
      isVisible = false;
    }
  }  // end of setVisibility()



  public void delete()
  // remove the entire skeleton from the scene graph
  {  skelBG.detach();  }



public Joint3D getRightHand() {
	for(Joint3D j3d : joints3D)
    {
      if(j3d.getJoint()==SkeletonJoint.LEFT_HAND)
    	  return j3d;
    }
	return null;
}
public Joint3D getMember(SkeletonJoint sj) {
	for(Joint3D j3d : joints3D)
    {
      if(j3d.getJoint()==sj)
    	  return j3d;
    }
	return null;
}

public Joint3D[] getLeftHandAndShoulder() {
	Joint3D[] tmp = new Joint3D[2];
	for(Joint3D j3d : joints3D)
    {
      if(j3d.getJoint()==SkeletonJoint.RIGHT_HAND)
    	  tmp[0]=j3d;
      if(j3d.getJoint()==SkeletonJoint.RIGHT_SHOULDER)
    	  tmp[1]=j3d;
    }
	return tmp;
}

} // end of Skeleton3D class


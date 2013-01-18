package UsersViewer3D;

// Limb3D.java
// Andrew Davison, October 2011, ad@fivedots.coe.psu.ac.th

/*  This class creates a scene graph representing a skeleton limb,
    and updates its visibility, position, orientation, and length
    when update() is called.

    The scene graph for the limb:
       startTG --->visSW-->orientTG --->scaleTG ---> baseTG ---> cylinder

   startTG maintains the limb's joint
   location, and is linked to the scene at run time;

   visSW: whether the limb is visible;

   orientTG: contains the orientation of the limb (axis of rotation, the angle);

   scaleTG: used to scale the y-axis length of the cylinder;

   baseTG: the (0,0,0) point for Java 3D's Cylinder is its middle, so baseTG lifts
           it up the y-axis by length/2, so (0,0,0) is now the center of its base.

*/


import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.*;
import java.text.DecimalFormat;

import com.sun.j3d.utils.geometry.Cylinder;

import org.OpenNI.*;


public class Limb3D
{
  private static final Vector3d ORIGIN = new Vector3d(0, 0, 0);

  // initial orientation: straight up
  private static final Vector3d UPVEC = new Vector3d(0.0, 1.0, 0.0);

  private static final double LIMB_LEN = 1;

  private static final double MIN_DIST = 0.00001;
            // small distance that probably indicates an error

  // colors for limb material
  private static final Color3f BLACK = new Color3f(0.0f, 0.0f, 0.0f);
  private static final Color3f WHITE = new Color3f(0.9f, 0.9f, 0.9f);
  private static final Color3f GRAY = new Color3f(0.6f, 0.6f, 0.6f);


  // for rotation calculations
  private Vector3d axisVec = new Vector3d();
  private Vector3d negAxisVec = new Vector3d();
  private AxisAngle4d rotAxisAngle = new AxisAngle4d();
  private AxisAngle4d negRotAxisAngle = new AxisAngle4d();

  private Transform3D orientT3d = new Transform3D();
  private Transform3D rotT3d = new Transform3D();
  private Transform3D negRotT3d = new Transform3D();

  private TransformGroup orientTG;
  private boolean firstRotation = true;
                 // to flag the need to undo the previous rotation
  private double limbAngle = 0;

  // for positioning the limb
  private TransformGroup startTG;
  private Transform3D startT3d; 

  // for scaling
  private TransformGroup scaleTG; 
  private Vector3d scaleLimb = new Vector3d(1,1,1);   // only y changes
  private Transform3D currTrans = new Transform3D();

  // for limb visibility
  private Switch visSW;            
  private boolean isVisible;


  // the joints connected to this limb
  private Joint3D startJ3d, endJ3d;
  private SkeletonJoint startJoint, endJoint;

  private DecimalFormat df; 


  public Limb3D(Joint3D startJ3d, Joint3D endJ3d, float radius)
  { 
    this.startJ3d = startJ3d;
    this.endJ3d = endJ3d;

    startJoint = startJ3d.getJoint();
    endJoint = endJ3d.getJoint();

    df = new DecimalFormat("0.#");  // 1 dp

    // limb's joint position
    startT3d = new Transform3D();
    startTG = new TransformGroup(startT3d);
    startTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ);
    startTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE);  // can alter

    // create switch for visibility
    visSW = new Switch();
    visSW.setCapability(Switch.ALLOW_SWITCH_WRITE);
    visSW.setWhichChild( Switch.CHILD_ALL);   // visible initially
    isVisible = true;

    // limb's orientation
    orientTG = new TransformGroup();
    orientTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ);
    orientTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE);  // can alter

    // for changing the length of a limb
    scaleTG = new TransformGroup();
    scaleTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ);
    scaleTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE);  // can alter

    TransformGroup baseTG = makeLimb(radius, LIMB_LEN);

    // limb subgraph's sequence of nodes:
    //  startTG --->visSW-->orientTG --->scaleTG ---> baseTG ---> cylinder

    startTG.addChild(visSW);
    visSW.addChild(orientTG);
    orientTG.addChild(scaleTG);
    scaleTG.addChild(baseTG);

  } // end of Limb3D()


  private TransformGroup makeLimb(float radius, double len)
  // a gray cylinder whose base is at (0,0,0)
  {
    // fix limb's start position
    TransformGroup baseTG = new TransformGroup();
    currTrans.setTranslation( new Vector3d(0, len/2, 0) );   // move up length/2
    baseTG.setTransform(currTrans);

    Appearance app = new Appearance();
    Material limbMaterial = new Material(GRAY, BLACK, GRAY, WHITE, 150.0f); 
       // sets ambient, emissive, diffuse, specular, shininess
    limbMaterial.setLightingEnable(true);
    app.setMaterial( limbMaterial );

    Cylinder cyl = new Cylinder( radius, (float)len, app);
    baseTG.addChild( cyl );
    return baseTG;
  }  // end of makeLimb()



  public TransformGroup getTG()
  {  return startTG;  }



  public void update()
  // update visibility, position, orientation, and length of limb
  {
    // get start and end joint positions
    Vector3d startPos = startJ3d.getPos();
    Vector3d endPos = endJ3d.getPos();

    if (!isLimbUpdatable(startPos, endPos)) {    // hide the limb
      setVisibility(false);
      return;
    }
    
    // both joints are ok, so make the limb visible
    if (!isVisible)
      setVisibility(true);

    // update limb position
    startT3d.set(startPos); 
    startTG.setTransform(startT3d); 

    Vector3d lengthVec = new Vector3d( (double)(endPos.x - startPos.x),
                                       (double)(endPos.y - startPos.y),
                                       (double)(endPos.z - startPos.z));
    double len = lengthVec.length();  // before lengthVec is normalized!

    rotateLimb(lengthVec);
    setLength(len);    // change length of limb
  }  // end of positionLimb()


  private boolean isLimbUpdatable(Vector3d startPos, Vector3d endPos)
  // can the limb be updated given these joint positions?
  {
    if (!startJ3d.isVisible() || !endJ3d.isVisible()) {
      // System.out.println("Missing joint in: " + startJoint + " -- " + endJoint);
      return false;
    }

    /* very small difference between the (x,z) coordinates of 
       the joints indicates an upcoming error */
    if ((Math.abs(endPos.x - startPos.x) < MIN_DIST) &&
        (Math.abs(endPos.z - startPos.z) < MIN_DIST)) {
      // System.out.println("Joints too close in: " + startJoint + " -- " + endJoint);
      return false;
    }

    return true;
  }  // end of isLimbUpdatable()



  private void setVisibility(boolean toVisible)
  // change limb visibility
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



  private void rotateLimb(Vector3d lengthVec)
  {
    if (!firstRotation) {   // calculate negative of previous rotation
      negAxisVec.negate(axisVec);
      negRotAxisAngle.set(negAxisVec, limbAngle);
    }

    // update limb orientation
    AxisAngle4d rotAxisAngle = calcRotation(lengthVec);
    doRotation(rotAxisAngle, negRotAxisAngle);
    firstRotation = false;
  }  // end of rotateLimb()



  private AxisAngle4d calcRotation(Vector3d lengthVec)
  // calculate axis angle rotation
  { 
	  lengthVec.normalize();
	  axisVec.cross(UPVEC, lengthVec);
    limbAngle = UPVEC.angle(lengthVec);
    rotAxisAngle.set(axisVec, limbAngle);     // build rotation
    return rotAxisAngle;
  } // end of calcRotation()



  private void doRotation(AxisAngle4d rotAxisAngle, AxisAngle4d negRotAxisAngle)
  // rotate the limb cylinder
  {
    orientTG.getTransform( orientT3d );         // get current transform

    if (!firstRotation) {   // undo previous rotation first
      negRotT3d.setRotation(negRotAxisAngle);
      orientT3d.mul(negRotT3d);
    }

    // apply new rotation
	  rotT3d.setRotation(rotAxisAngle);              
    orientT3d.mul(rotT3d);

    orientTG.setTransform( orientT3d );
  } // end of doRotation()



  private void setLength(double len)
  // change the cylinder's length to len (by changing the scaling)
  { 
    double lenChange = len / (LIMB_LEN * scaleLimb.y);
    scaleLimb.y *= lenChange;   // only y scale changes
    scaleTG.getTransform(currTrans);
    currTrans.setScale(scaleLimb);
    scaleTG.setTransform(currTrans);
  }  // end of setLength()


  private void printVec(String name, Vector3d v)
  // used during debugging
  {  System.out.println(name + ": (" + v.x + ", " + 
                                       df.format(v.y) + ", " + 
                                       v.z + ")");  } 

}  // end of Limb3D class

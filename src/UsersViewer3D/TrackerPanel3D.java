package UsersViewer3D;

// TrackerPanel3D.java
// Andrew Davison, October 2011, ad@fivedots.psu.ac.th

/* This class has two main tasks:
     * create the 3D scene
     * create the OpenNI context, user generator, and skeletons
      (which is done in configOpenNI()).

   This class builds a Java 3D scene consisting of a dark green 
   and blue tiled surface with labels along the X and Z axes, 
   a blue background, lit from two different directions. 

   The user (viewer) can move the camera through the scene by moving the mouse.

   Each detected user is represented by a 3D skeleton made up of limbs
   (cylinders) and joints (spheres). The skeletons are managed by the 
   SkelsManager class.

   All of the scene graph, apart from the skeletons,
   comes from the Checkers3D example in Chapter 15,
   "Killer Game Programming in Java"
   (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/), and is explained
   in detail there.
*/

import java.awt.*;
import javax.swing.*;

import org.OpenNI.*;

import java.nio.ShortBuffer;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.behaviors.vp.*;



public class TrackerPanel3D extends JPanel
// Holds the 3D canvas where the skeletons are displayed
{
  private static final int PWIDTH = 512;   // size of panel
  private static final int PHEIGHT = 512; 

  private static final int BOUNDSIZE = 100;  // larger than world

  private static final Point3d USERPOSN = new Point3d(0, 6.5, 10);
    // initial user position


  // 3D vars
  private SimpleUniverse su;
  public BranchGroup sceneBG;
  private BoundingSphere bounds;   // for environment nodes

  private volatile boolean isRunning;
  
  // OpenNI
  public Context context;
  private UserGenerator userGen;

  private SkelsManager skels;   // the skeletons manager


  public TrackerPanel3D(Context context, UserGenerator userGen)
  {
	  this.context=context;
	  this.userGen=userGen;
    setLayout( new BorderLayout() );
    setBackground(Color.white);
    setOpaque( false );
    setPreferredSize( new Dimension(PWIDTH, PHEIGHT));

    GraphicsConfiguration config =
					SimpleUniverse.getPreferredConfiguration();
    Canvas3D canvas3D = new Canvas3D(config);
    add("Center", canvas3D);
    canvas3D.setFocusable(true);     // give focus to the canvas 
    canvas3D.requestFocus();

    su = new SimpleUniverse(canvas3D);

    createSceneGraph();
    initUserPosition();        // set user's viewpoint
    orbitControls(canvas3D);   // controls for moving the viewpoint

    su.addBranchGraph( sceneBG );
    addReference2();
    //configOpenNI();

    //new Thread(this).start();   // start updating the panel's image
  } // end of TrackerPanel3D()



  private void createSceneGraph() 
  // initilise the scene
  { 
    sceneBG = new BranchGroup();
    sceneBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);   // to attach & detach skeletons
    sceneBG.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    sceneBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

    bounds = new BoundingSphere(new Point3d(0,0,0), BOUNDSIZE);   

    lightScene();         // add the lights
    addBackground();      // add the sky
    sceneBG.addChild( new CheckerFloor().getBG() );  // add the floor

    sceneBG.compile();   // fix the scene
  } // end of createSceneGraph()


  private void lightScene()
  /* One ambient light, 2 directional lights */
  {
    Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

    // Set up the ambient light
    AmbientLight ambientLightNode = new AmbientLight(white);
    ambientLightNode.setInfluencingBounds(bounds);
    sceneBG.addChild(ambientLightNode);

    // Set up the directional lights
    Vector3f light1Direction  = new Vector3f(-1.0f, -1.0f, -1.0f);
       // left, down, backwards 
    Vector3f light2Direction  = new Vector3f(1.0f, -1.0f, 1.0f);
       // right, down, forwards

    DirectionalLight light1 = 
            new DirectionalLight(white, light1Direction);
    light1.setInfluencingBounds(bounds);
    sceneBG.addChild(light1);

    DirectionalLight light2 = 
        new DirectionalLight(white, light2Direction);
    light2.setInfluencingBounds(bounds);
    sceneBG.addChild(light2);
  }  // end of lightScene()



  private void addBackground()
  // A blue sky
  { Background back = new Background();
    back.setApplicationBounds( bounds );
    back.setColor(0.17f, 0.65f, 0.92f);    // sky colour
    sceneBG.addChild( back );
  }  // end of addBackground()



  private void orbitControls(Canvas3D c)
  /* OrbitBehaviour allows the user to rotate around the scene, and to
     zoom in and out.  */
  {
    OrbitBehavior orbit = 
		new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
    orbit.setSchedulingBounds(bounds);

    ViewingPlatform vp = su.getViewingPlatform();
    vp.setViewPlatformBehavior(orbit);	 
  }  // end of orbitControls()


  private void initUserPosition()
  // Set the user's initial viewpoint using lookAt()
  {
    ViewingPlatform vp = su.getViewingPlatform();
    TransformGroup steerTG = vp.getViewPlatformTransform();

    Transform3D t3d = new Transform3D();
    steerTG.getTransform(t3d);

    // args are: viewer posn, where looking, up direction
    t3d.lookAt( USERPOSN, new Point3d(0,0,0), new Vector3d(0,1,0));
    t3d.invert();

    steerTG.setTransform(t3d);
  }  // end of initUserPosition()



  public void closeDown()
  {  isRunning = false;  } 
  
  public void setSkelMan(SkelsManager skels)
  {  this.skels = skels;  } 

  public void addReference(org.OpenNI.Point3D pos)
  {
	  if(pos!=null)
	  {
		  BranchGroup objRoot=new BranchGroup();
		  Transform3D translate1 = new Transform3D(); 
		  translate1.set(new Vector3f(pos.getX(), pos.getY(), pos.getZ()));

		  TransformGroup TG1 = new TransformGroup(translate1);

		  TG1.addChild(new ColorCube(0.15f));// de rayon 30 cm 
		  objRoot.addChild(TG1);
		  sceneBG.addChild(objRoot);
	  }
  }
  
  public void addReference2()
  {
		  BranchGroup objRoot=new BranchGroup();
		  Transform3D translate1 = new Transform3D(); 
		  translate1.set(new Vector3f(-0.4f, -0.4f, 0.0f));

		  TransformGroup TG1 = new TransformGroup(translate1);

		  TG1.addChild(new ColorCube(0.15f));// de rayon 30 cm 
		  objRoot.addChild(TG1);
		  sceneBG.addChild(objRoot);

  }
  
} // end of TrackerPanel3D class


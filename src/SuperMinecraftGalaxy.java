import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.shadow.BasicShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.lang.reflect.Field;

public class SuperMinecraftGalaxy extends SimpleApplication implements ActionListener {

    private DanielCharacterControl player;
    private Vector3f walkDirection = new Vector3f();
    private boolean left;
    private boolean right;
    private boolean up;
    private boolean down;
    private CapsuleCollisionShape capsuleShape;

    public static void main(String args[]) {
        SuperMinecraftGalaxy app = new SuperMinecraftGalaxy();
        app.start();
    }

    /**
     * Prepare the Physics Application State (jBullet)
     */
    private BulletAppState bulletAppState;

    /**
     * Activate custom rendering of shadows
     */
    BasicShadowRenderer bsr;

    /**
     * Prepare Materials
     */
    Material wall_mat;
    Material stone_mat;
    Material floor_mat;

    /**
     * Prepare geometries and physical nodes for bricks and cannon balls.
     */
    private RigidBodyControl brick_phy;
    private static final Box box;
    private RigidBodyControl ball_phy;
    private static final Sphere sphere;
    private RigidBodyControl floor_phy;
    private static final Box floor;

    /**
     * dimensions used for bricks and wall
     */
    private static final float brickLength = 1f;
    private static final float brickWidth = 1f;
    private static final float brickHeight = 1f;

    static {
        /** Initialize the cannon ball geometry */
        sphere = new Sphere(32, 32, 0.4f, true, false);
        sphere.setTextureMode(TextureMode.Projected);
        /** Initialize the brick geometry */
        box = new Box(Vector3f.ZERO, brickLength, brickHeight, brickWidth);
        box.scaleTextureCoordinates(new Vector2f(1f, .5f));
        /** Initialize the floor geometry */
        floor = new Box(Vector3f.ZERO, 1f, 1f, 1f);
//        floor = new Sphere(320, 320, 1000f, true, false);
        floor.scaleTextureCoordinates(new Vector2f(3, 6));
    }

    @Override
    public void simpleInitApp() {
        /** Set up Physics Game */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        /** Configure cam to look at scene */
        cam.setLocation(new Vector3f(0, 6f, 6f));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 1, 0));

        cam.setFrustumFar(40);
        /** Add InputManager action: Left click triggers shooting. */
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");

        /** Initialize the scene, materials, and physics space */
        initMaterials();
//        initCube(10);
        int radius = 30;
        initSphere(radius, 0, 0, 0, radius/2, radius);
        initFloor();
        initCrossHairs();
        initShadows();
//        bulletAppState.getPhysicsSpace().setGravity(Vector3f.ZERO);
        addGravityToPlanet();

        addDanielPlayer(radius * 3);
        setUpKeys();
    }

    /**
     * We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping:
     */
    private void setUpKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Jumps");
    }

    private void addDanielPlayer(int playerlocation) {
        capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
//        capsuleShape = new CylinderCollisionShape(new Vector3f(1.5f, 6f, 1), 1);
        player = new DanielCharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(20);
        player.setFallSpeed(30);
        player.setGravity(30);
//        player.setMaxSlope((float)((25.0f/180.0f) * Math.PI));
//        player.setUseViewDirection(false);
        player.setPhysicsLocation(new Vector3f(-10, 0, -playerlocation));
        player.setUpAxis(1);
        bulletAppState.getPhysicsSpace().add(player);
    }

    private void addGravityToPlanet() {
        bulletAppState.getPhysicsSpace().addTickListener(new PhysicsTickListener() {
            @Override
            public void prePhysicsTick(PhysicsSpace space, float f) {
                Vector3f floorLocation = floor_phy.getPhysicsLocation();
                // check in which direction we need to apply the force
                // subtract the location of the dynamic node from the
                // black holes location and normalize it
                Vector3f direction = floorLocation.subtract(player.getPhysicsLocation()).normalize();

                javax.vecmath.Vector3f[] upAxisDirection = (javax.vecmath.Vector3f[]) getPrivateField(player.getControllerId(), "upAxisDirection");


                upAxisDirection[0].set(-direction.getX(), -direction.getY(), -direction.getZ());
                upAxisDirection[1].set(-direction.getX(), -direction.getY(), -direction.getZ());
                upAxisDirection[2].set(-direction.getX(), -direction.getY(), -direction.getZ());

                getFlyByCamera().setUpVector(new Vector3f(-direction.getX(), -direction.getY(), -direction.getZ()));
            }


            @Override
            public void physicsTick(PhysicsSpace space, float f) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    public static Object getPrivateField(Object o, String fieldName) {

        // Go and find the private field...
        final Field fields[] = o.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; ++i) {
            if (fieldName.equals(fields[i].getName())) {
                try {
                    fields[i].setAccessible(true);
                    return fields[i].get(o);
                } catch (IllegalAccessException ex) {
                    System.out.println("IllegalAccessException accessing " + fieldName);
                }
            }
        }
        System.out.println("Field '" + fieldName + "' not found");
        return null;
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector3f camDir = cam.getDirection().clone().multLocal(0.6f);
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
    }

    /**
     * Every time the shoot action is triggered, a new cannon ball is produced.
     * The ball is set up to fly from the camera position in the camera direction.
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot") && !keyPressed) {
                makeCannonBall();
            }
        }
    };

    /**
     * Initialize the materials used in this scene.
     */
    public void initMaterials() {
        wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        wall_mat.setTexture("ColorMap", tex);

        stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        stone_mat.setTexture("ColorMap", tex2);

        floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.png");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(WrapMode.Repeat);
        floor_mat.setTexture("ColorMap", tex3);
    }

    /**
     * Make a solid floor and add it to the scene.
     */
    public void initFloor() {
        Geometry floor_geo = new Geometry("Floor", floor);
        floor_geo.setMaterial(floor_mat);
        floor_geo.setShadowMode(ShadowMode.Receive);
        floor_geo.setLocalTranslation(0, 0, 0);
        this.rootNode.attachChild(floor_geo);
/* Make the floor physical with mass 0.0f! */
        floor_phy = new RigidBodyControl(0.0f);
        floor_geo.addControl(floor_phy);
        bulletAppState.getPhysicsSpace().add(floor_phy);
    }

    /**
     * This loop builds a wall out of individual bricks.
     *
     * @param diameter
     */
    public void initCube(int diameter) {
        int x = 0;
        int y = 0;
        int z = 0;
        for (int k = x; k < x + diameter; k++) {
            for (int j = y; j < y + diameter; j++) {
                for (int i = z; i < z + diameter; i++) {
                    Vector3f vt = new Vector3f(i * 2, j * 2, k * 2);
                    makeBrick(vt);
                }
            }
        }
    }

    public void initSphere(int radius, int centerX, int centerY, int centerZ, int crustThicknessInPercentIThink, int radiusOffsetInCaseItDoesNotRenderProperly) {
        int diameter = radius * 2;
        int x = centerX - diameter;
        int y = centerY - diameter;
        int z = centerZ - diameter;

        float mid = (diameter - 1) / 2;
        for (int k = 0; k < diameter; k++) {
            for (int j = 0; j < diameter; j++) {
                for (int i = 0; i < diameter; i++) {
                    float partOfSphere = isPartOfSphere(i - mid, j - mid, k - mid, radius);
                    System.out.println(partOfSphere);
                    if (partOfSphere <= crustThicknessInPercentIThink && partOfSphere >= -radius - radiusOffsetInCaseItDoesNotRenderProperly) {
                        Vector3f vt = new Vector3f(i * 2 + x, j * 2 + y, k * 2 + z);
                        makeBrick(vt);
                    }
                }
            }
        }
    }

    private float isPartOfSphere(float x, float y, float z, float R) { // Implicit circle equation
        return x * x + y * y + z * z - R * R;
    }


    /**
     * Activate shadow casting and light direction
     */
    private void initShadows() {
        bsr = new BasicShadowRenderer(assetManager, 256);
        bsr.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        viewPort.addProcessor(bsr);
// Default mode is Off -- Every node declares own shadow mode!
        rootNode.setShadowMode(ShadowMode.Off);
    }

    /**
     * This method creates one individual physical brick.
     */
    public void makeBrick(Vector3f loc) {
        /** Create a brick geometry and attach to scene graph. */
        Geometry brick_geo = new Geometry("brick", box);
        brick_geo.setMaterial(wall_mat);
        rootNode.attachChild(brick_geo);
/** Position the brick geometry and activate shadows */
        brick_geo.setLocalTranslation(loc);
//        brick_geo.setShadowMode(ShadowMode.CastAndReceive);
/** Make brick physical with a mass > 0.0f. */
        brick_phy = new RigidBodyControl(0f);
/** Add physical brick to physics space. */
        brick_geo.addControl(brick_phy);
        bulletAppState.getPhysicsSpace().add(brick_phy);

    }

    /**
     * This method creates one individual physical cannon ball.
     * By defaul, the ball is accelerated and flies
     * from the camera position in the camera direction.
     */
    public void makeCannonBall() {
        /** Create a cannon ball geometry and attach to scene graph. */
        Geometry ball_geo = new Geometry("cannon ball", sphere);
        ball_geo.setMaterial(stone_mat);
        rootNode.attachChild(ball_geo);
/** Position the cannon ball and activate shadows */
        ball_geo.setLocalTranslation(cam.getLocation());
        ball_geo.setShadowMode(ShadowMode.CastAndReceive);
/** Make the ball physcial with a mass > 0.0f */
        ball_phy = new RigidBodyControl(1f);
/** Add physical ball to physics space. */
        ball_geo.addControl(ball_phy);
        bulletAppState.getPhysicsSpace().add(ball_phy);
/** Accelerate the physcial ball to shoot it. */
        ball_phy.setLinearVelocity(cam.getDirection().mult(25));
    }

    /**
     * A plus sign used as crosshairs to help the player with aiming.
     */
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");        // fake crosshairs :)
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    /**
     * These are our custom actions triggered by key presses.
     * We do not walk yet, we just keep track of the direction the user pressed.
     */
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            left = value;
        } else if (binding.equals("Rights")) {
            right = value;
        } else if (binding.equals("Ups")) {
            up = value;
        } else if (binding.equals("Downs")) {
            down = value;
        } else if (binding.equals("Jumps")) {
            player.jump();
        }
    }
}
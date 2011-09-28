import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.control.Control;

/**
 *
 * @author normenhansen
 */
public interface DanielPhysicsControl extends Control {

    public void setPhysicsSpace(PhysicsSpace space);

    public PhysicsSpace getPhysicsSpace();
}
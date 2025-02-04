package com.forcex.gfx3d;

import com.forcex.FX;
import com.forcex.anim.Animator;
import com.forcex.core.GL;
import com.forcex.gfx3d.shader.DefaultShader;
import com.forcex.math.Matrix4f;
import com.forcex.math.Quaternion;
import com.forcex.math.Vector3f;

public class ModelObject {
    private Matrix4f transform;
    private Quaternion rotation;
    private Vector3f position;
    private Mesh mesh;
    private boolean visible;
    private boolean lighting;
    private Animator animator;
    private String name = "model";
    private short id = -1;
    private boolean shadow_map_cullface = true;
    private boolean effect_animator = true;

    public ModelObject(Mesh mesh) {
        this.mesh = mesh;
        reset();
    }

    public ModelObject() {
        reset();
    }

    private void reset() {
        transform = new Matrix4f();
        rotation = new Quaternion();
        position = new Vector3f();
        lighting = true;
        visible = true;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public void setPosition(float x, float y, float z) {
        transform.setLocation(x, y, z);
    }

    public void applyRotationAxis(Vector3f axis, float angle) {
        transform.getRotation(rotation);
        Quaternion ac = Quaternion.fromAxisAngle(axis, angle);
        transform.setRotation(ac.multLocal(rotation));
    }

    public void setRotation(float x, float y, float z) {
        rotation.setEulerAngles(x, y, z);
        transform.setRotation(rotation);
    }

    public void lookAt(Vector3f target, boolean upZ) {
        rotation.lookAt(getPosition(), target, upZ);
        transform.setRotation(rotation);
    }

    public void lookAt(float targetX, float targetY, float targetZ, boolean upZ) {
        rotation.lookAt(getPosition(), new Vector3f(targetX, targetY, targetZ), upZ);
        transform.setRotation(rotation);
    }

    public Vector3f getRotation() {
        return getRotationQuaternion().getAngles();
    }

    public Vector3f getPosition() {
        return transform.getLocation(position);
    }

    public void setPosition(Vector3f position) {
        transform.setLocation(position);
    }

    public Quaternion getRotationQuaternion() {
        return transform.getRotation(rotation);
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public void setTransform(Matrix4f other) {
        transform.set(other);
    }

    public void update() {
        mesh.update();
    }

    public short getID() {
        return id;
    }

    public void setID(int id) {
        this.id = (short) id;
    }

    public void setEffectAnimator(boolean z) {
        effect_animator = z;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasAnimator() {
        return animator != null;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean z) {
        this.visible = z;
    }

    public Animator getAnimator() {
        return animator;
    }

    public void setAnimator(Animator animator) {
        this.animator = animator;
    }

    public boolean isShadowMapCullfaceEnabled() {
        return shadow_map_cullface;
    }

    public void setShadowMapCullfaceEnabled(boolean z) {
        shadow_map_cullface = z;
    }

    public void setLighting(boolean z) {
        lighting = z;
    }

    public void render(DefaultShader shader) {
        if (visible) {
            shader.setModelMatrix(transform);
            FX.gl.glEnable(GL.GL_BLEND);
            FX.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            if (!mesh.isClone && !mesh.started) {
                mesh.initialize();
            }
            if (animator != null && effect_animator) {
                if (!shader.flag(DefaultShader.SHADOW_MAP_FLAG)) {
                    animator.update();
                }
                mesh.render(shader, lighting, animator);
            } else {
                mesh.render(shader, lighting);
            }
            FX.gl.glDisable(GL.GL_BLEND);
        }
    }

    public void delete() {
        visible = false;
        transform = null;
        mesh.delete();
        mesh = null;
        animator = null;
    }
}

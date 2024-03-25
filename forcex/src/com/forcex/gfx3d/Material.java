package com.forcex.gfx3d;

import com.forcex.core.gpu.Texture;
import com.forcex.utils.Color;

public class Material {
    public Color color;
    public boolean isNormalMap = false, isTransparent = false;
    public int
            normalTexture = -1,
            diffuseTexture = -1;
    public float specular;
    public float diffuse;
    public float ambient;
    public float reflection;
    public String textureName = "";

    public Material() {
        color = new Color(Color.WHITE);
        ambient = 1.0f;
        diffuse = 1.0f;
        specular = 1.0f;
        reflection = 0.3f;
    }

    public boolean isTransparent() {
        return (isTransparent || color.a < 240);
    }

    public void delete() {
        deleteTextures();
        textureName = null;
        color = null;
    }

    public void deleteTextures() {
        Texture.remove(diffuseTexture);
        diffuseTexture = -1;
        Texture.remove(normalTexture);
        normalTexture = -1;
    }

    protected void update(boolean normalMap) {
        if (diffuseTexture == -1) {
            diffuseTexture = Texture.genTextureWhite();
        }
        if (normalTexture == -1 && normalMap) {
            normalTexture = Texture.genTextureWhite();
        }
    }

    public Material clone() {
        Material mat = new Material();
        mat.color.set(color);
        mat.diffuseTexture = diffuseTexture;
        mat.textureName = textureName;
        mat.diffuse = diffuse;
        mat.ambient = ambient;
        mat.specular = specular;
        mat.reflection = reflection;
        mat.isNormalMap = isNormalMap;
        mat.normalTexture = normalTexture;
        return mat;
    }
}

package com.forcex.gfx3d.particle;

import com.forcex.gfx3d.shader.ShaderProgram;
import com.forcex.math.Matrix4f;
import com.forcex.utils.Color;

/** Shader de billboards de particulas: textura * color (el alfa del color se usa para el desvanecido). */
public class ParticleShader extends ShaderProgram {
    private static final String VERTEX =
            "attribute vec4 vertex;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "varying vec2 texcoord;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * vec4(vertex.xy, 0.0, 1.0);\n" +
            "    texcoord = vertex.zw;\n" +
            "}\n";

    private static final String FRAGMENT =
            "#ifdef GL_ES\n" +
            "precision mediump float;\n" +
            "#endif\n" +
            "varying vec2 texcoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec4 color;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(uTexture, texcoord) * color;\n" +
            "}\n";

    public final int attribute_vertex;
    private final int u_MVPMatrix;
    private final int u_color;

    public ParticleShader() {
        createProgram(VERTEX, FRAGMENT, "");
        attribute_vertex = getAttribLocation("vertex");
        u_MVPMatrix = getUniformLocation("uMVPMatrix");
        u_color = getUniformLocation("color");
        start();
        setInt(getUniformLocation("uTexture"), 0);
        stop();
    }

    public void setMVPMatrix(Matrix4f matrix) {
        setMatrix4f(u_MVPMatrix, matrix);
    }

    public void setParticleColor(Color color) {
        setColor4(u_color, color);
    }
}

package com.forcex.gfx3d.shader;

import com.forcex.FX;
import com.forcex.core.GL;
import com.forcex.io.BinaryStreamReader;
import com.forcex.io.FileSystem;
import com.forcex.math.Matrix2f;
import com.forcex.math.Matrix3f;
import com.forcex.math.Matrix4f;
import com.forcex.math.Vector2f;
import com.forcex.math.Vector3f;
import com.forcex.math.Vector4f;
import com.forcex.utils.Color;
import com.forcex.utils.Logger;

public class ShaderProgram {

    public boolean isStarted;

    // generic properties
    public int attrib_position = -1;
    public int attrib_color = -1;
    public int attrib_texcoord = -1;
    public int attrib_normal = -1;
    public int attrib_bone_wights = -1;
    public int attrib_bone_indices = -1;
    public int attrib_tangent = -1;
    public int attrib_bitangent = -1;

    // shader properties
    private int program = -1;
    private int VertexShaderID;
    private int FragmentShaderID;
    private float[] matrix_data = null;

    GL gl = FX.gl;

    public ShaderProgram(String vertex_shader, String fragment_shader) {
        createProgram(vertex_shader, fragment_shader, "");
    }

    public ShaderProgram() {
    }

    public void start() {
        isStarted = true;
        gl.glUseProgram(program);
    }

    public void stop() {
        if (!isStarted) {
            return;
        }
        isStarted = false;
        gl.glUseProgram(0);
    }

    public int getProgram() {
        return program;
    }

    private int createShader(String source, boolean fragment) {
        int shader = gl.glCreateShader(fragment ? GL.GL_FRAGMENT_SHADER : GL.GL_VERTEX_SHADER, source);
        if (shader == -1) {
            FX.device.showInfo(
                    "ForceX: \n" +
                            "Shader Program:\n" +
                            "Estate: CRASHED\n" +
                            "Error can't create the " + (fragment ? "fragment" : "vertex") + " shader.", true);
            FX.device.stopRender();
        }
        if (gl.glGetShaderi(shader, GL.GL_COMPILE_STATUS) != GL.GL_TRUE) {
            Logger.log(gl.glGetShaderInfoLog(shader));
            FX.device.showInfo(
                    "ForceX: \n" +
                            "Shader Program:\n" +
                            "Estate: CRASHED\n" +
                            "Type: " + (fragment ? "fragment" : "vertex") + "\n" +
                            "Source: " + source + "\n" +
                            "Info: " + gl.glGetShaderInfoLog(shader) + "\n", true);
            FX.device.stopRender();
        }
        return shader;
    }

    public void createProgram(String vertex_shader, String fragment_shader, String prefix) {
        // check if exist
        String vertexSource = prefix;
        String fragmentSource = prefix;
        {
            BinaryStreamReader is = FX.fs.open(vertex_shader, FileSystem.ReaderType.STREAM);
            if (is != null) {
                vertexSource += new String(is.readByteArray(is.length()));
                is.clear();
            } else {
                vertexSource += vertex_shader;
            }
            is = FX.fs.open(fragment_shader, FileSystem.ReaderType.STREAM);
            if (is != null) {
                fragmentSource += new String(is.readByteArray(is.length()));
                is.clear();
            } else {
                fragmentSource += fragment_shader;
            }
        }
        VertexShaderID = createShader(vertexSource, false);
        FragmentShaderID = createShader(fragmentSource, true);
        program = gl.glCreateProgram(VertexShaderID, FragmentShaderID);
    }

    public void setInt(int loc, int value) {
        if (loc == -1) {
            return;
        }
        gl.glUniform1i(loc, value);
    }

    public void setFloat(int loc, float value) {
        if (loc == -1) {
            return;
        }
        gl.glUniform1f(loc, value);
    }

    public void setBoolean(int loc, boolean z) {
        if (loc == -1) {
            return;
        }
        gl.glUniform1i(loc, (z ? 1 : 0));
    }

    public void setMatrix3f(int loc, Matrix3f matrix) {
        if (loc == -1) {
            return;
        }
        gl.glUniformMatrix3f(loc, 1, matrix.data);
    }

    public void setMatrix4f(int loc, Matrix4f matrix) {
        if (loc == -1) {
            return;
        }
        gl.glUniformMatrix4f(loc, 1, matrix.data);
    }

    public void setMatrix2f(int loc, Matrix2f matrix) {
        if (loc == -1) {
            return;
        }
        gl.glUniformMatrix2f(loc, 1, matrix.data);
    }

    public void setMatrix4fArray(int loc, Matrix4f[] array) {
        if (loc == -1) {
            return;
        }
        if(matrix_data == null || matrix_data.length < array.length * 16) {
            matrix_data = new float[array.length * 16];
        }
        for (int i = 0; i < array.length; i++) {
            array[i].get(matrix_data, i * 16);
        }
        gl.glUniformMatrix4f(loc, array.length, matrix_data);
    }

    public void setVector2(int loc, Vector2f vector) {
        if (loc == -1) {
            return;
        }
        gl.glUniform2f(loc, vector.x, vector.y);
    }

    public void setVector3(int loc, Vector3f vector) {
        if (loc == -1) {
            return;
        }
        gl.glUniform3f(loc, vector.x, vector.y, vector.z);
    }

    public void setVector4(int loc, Vector4f vector) {
        if (loc == -1) {
            return;
        }
        gl.glUniform4f(loc, vector.x, vector.y, vector.z, vector.w);
    }

    public void setColor4(int loc, Color color) {
        if (loc == -1) {
            return;
        }
        gl.glUniform4f(loc, color.r * 0.003921f, color.g * 0.003921f, color.b * 0.003921f, color.a * 0.003921f);
    }

    public void setColor3(int loc, Color color) {
        if (loc == -1) {
            return;
        }
        gl.glUniform3f(loc, color.r * 0.003921f, color.g * 0.003921f, color.b * 0.003921f);
    }

    public void setTextureChannel(String texture, int channel) {
        start();
        setInt(getUniformLocation(texture), channel);
        stop();
    }

    public int getAttribLocation(String name) {
        return gl.glGetAttribLocation(program, name);
    }

    public int getUniformLocation(String name) {
        return gl.glGetUniformLocation(program, name);
    }

    public void cleanUp() {
        stop();
        matrix_data = null;
        gl.glCleanProgram(program, VertexShaderID, FragmentShaderID);
    }
}

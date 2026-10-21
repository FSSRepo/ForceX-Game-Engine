package com.forcex.gfx3d.effect;
import com.forcex.gfx3d.shader.*;
import java.nio.*;
import com.forcex.utils.*;
import com.forcex.*;
import com.forcex.core.*;
import com.forcex.gfx3d.*;
import com.forcex.math.*;

/**
 * Cielo procedural: degradado del horizonte al cenit, sol con halo y nubes que se desplazan.
 * Se dibuja centrado en la camara (no se puede alcanzar) y sin escribir profundidad,
 * asi que debe renderizarse antes que el resto de la escena.
 */
public class SkyBox{
	private final ShaderProgram shader;
	private final int ibo;
	private final int vbo;
	private int length;
	private final int vertex_idx, mvp_idx, sun_idx, time_idx;
	private final int zenith_idx, horizon_idx, ground_idx, cloud_idx;
	private final GL gl = FX.gl;
	private final Matrix4f view_rotation = new Matrix4f();
	private final Matrix4f temp = new Matrix4f();

	private final Vector3f sun_direction = new Vector3f(0.5f, 0.6f, 0.4f).normalize();
	private final Color zenith = new Color(52, 105, 205, 255);
	private final Color horizon = new Color(178, 208, 238, 255);
	private final Color ground = new Color(150, 165, 170, 255);
	private float cloud_cover = 0.5f;
	private float time = 0f;

	public SkyBox(float side_size) {
		shader = new ShaderProgram();
		shader.createProgram(
			"attribute vec3 vertexs;\n"+
			"uniform mat4 mvp;\n"+
			"varying vec3 dir;\n"+
			"void main(){\n"+
			"	dir = vertexs;\n"+
			"	gl_Position = mvp * vec4(vertexs,1.0);\n"+
			"}\n",
			"#ifdef GL_ES\n"+
			"#ifdef GL_FRAGMENT_PRECISION_HIGH\n"+
			"precision highp float;\n"+
			"#else\n"+
			"precision mediump float;\n"+
			"#endif\n"+
			"#endif\n"+
			"varying vec3 dir;\n"+
			"uniform vec3 sun;\n"+
			"uniform vec3 zenith;\n"+
			"uniform vec3 horizon;\n"+
			"uniform vec3 ground;\n"+
			"uniform float cloud_cover;\n"+
			"uniform float time;\n"+
			"float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }\n"+
			"float noise(vec2 p){\n"+
			"	vec2 i = floor(p);\n"+
			"	vec2 f = fract(p);\n"+
			"	f = f * f * (3.0 - 2.0 * f);\n"+
			"	return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),\n"+
			"	           mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);\n"+
			"}\n"+
			"float fbm(vec2 p){\n"+
			"	float v = 0.0;\n"+
			"	float a = 0.5;\n"+
			"	for(int i = 0; i < 4; i++){ v += a * noise(p); p *= 2.03; a *= 0.5; }\n"+
			"	return v;\n"+
			"}\n"+
			"void main(){\n"+
			"	vec3 d = normalize(dir);\n"+
			"	float up = d.y;\n"+
			"	vec3 col = mix(horizon, zenith, pow(clamp(up, 0.0, 1.0), 0.55));\n"+
			"	col = mix(col, ground, clamp(-up * 4.0, 0.0, 1.0));\n"+
			"	float s = max(dot(d, normalize(sun)), 0.0);\n"+
			"	col += vec3(1.0, 0.92, 0.75) * (pow(s, 400.0) * 2.0 + pow(s, 12.0) * 0.25);\n"+
			"	if(up > 0.02){\n"+
			"		vec2 uv = d.xz / (up + 0.18) * 1.4 + vec2(time * 0.02, time * 0.008);\n"+
			"		float c = smoothstep(1.0 - cloud_cover, 1.0, fbm(uv));\n"+
			"		c *= smoothstep(0.02, 0.25, up);\n"+
			"		vec3 cloud = mix(vec3(1.0), vec3(0.72, 0.76, 0.82), clamp(1.0 - c, 0.0, 1.0) * 0.5);\n"+
			"		col = mix(col, cloud, c * 0.9);\n"+
			"	}\n"+
			"	gl_FragColor = vec4(col, 1.0);\n"+
			"}\n", "");
		vertex_idx = shader.getAttribLocation("vertexs");
		mvp_idx = shader.getUniformLocation("mvp");
		sun_idx = shader.getUniformLocation("sun");
		zenith_idx = shader.getUniformLocation("zenith");
		horizon_idx = shader.getUniformLocation("horizon");
		ground_idx = shader.getUniformLocation("ground");
		cloud_idx = shader.getUniformLocation("cloud_cover");
		time_idx = shader.getUniformLocation("time");

		float s = side_size;
		float[] vertices = {
			//front
			-s,+s,+s, +s,+s,+s, +s,-s,+s, -s,-s,+s,
			//right
			+s,+s,+s, +s,+s,-s, +s,-s,-s, +s,-s,+s,
			//back
			+s,+s,-s, -s,+s,-s, -s,-s,-s, +s,-s,-s,
			//left
			-s,+s,-s, -s,+s,+s, -s,-s,+s, -s,-s,-s,
			//top
			-s,+s,-s, +s,+s,-s, +s,+s,+s, -s,+s,+s,
			//bottom
			-s,-s,+s, +s,-s,+s, +s,-s,-s, -s,-s,-s
		};

		short[] indices = {
			0,2,1,0,3,2, //front
			4,6,5,4,7,6, //right
			8,10,9,8,11,10, //back
			12,14,13,12,15,14, //left
			16,18,17,16,19,18, //top
			20,22,21,20,23,22 //bottom
		};

		length = indices.length;
		vbo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, BufferUtils.createFloatBuffer(vertices), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
		ibo = gl.glGenBuffer();
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,indices.length*2,BufferUtils.createShortBuffer(indices), GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
	}

	/** Direccion hacia el sol (no necesita estar normalizada); y > 0 es sobre el horizonte. */
	public SkyBox setSunDirection(float x, float y, float z) {
		sun_direction.set(x, y, z).normalize();
		return this;
	}

	public SkyBox setColors(Color zenith, Color horizon, Color ground) {
		this.zenith.set(zenith);
		this.horizon.set(horizon);
		this.ground.set(ground);
		return this;
	}

	/** Cantidad de nubes de 0 (despejado) a 1 (cubierto). */
	public SkyBox setCloudCover(float cover) {
		cloud_cover = Math.max(0f, Math.min(1f, cover));
		return this;
	}

	public void render(Camera cam) {
		time += FX.gpu.getDeltaTime();

		// solo la rotacion de la vista: el cielo acompana a la camara
		view_rotation.set(cam.getViewMatrix());
		view_rotation.data[Matrix4f.M03] = 0f;
		view_rotation.data[Matrix4f.M13] = 0f;
		view_rotation.data[Matrix4f.M23] = 0f;

		boolean depth_test = gl.glIsEnabled(GL.GL_DEPTH_TEST);
		boolean cull_face = gl.glIsEnabled(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_CULL_FACE);
		gl.glDepthMask(false);

		shader.start();
		shader.setMatrix4f(mvp_idx, cam.getProjectionMatrix().mult(temp, view_rotation));
		shader.setVector3(sun_idx, sun_direction);
		shader.setColor3(zenith_idx, zenith);
		shader.setColor3(horizon_idx, horizon);
		shader.setColor3(ground_idx, ground);
		shader.setFloat(cloud_idx, cloud_cover);
		shader.setFloat(time_idx, time);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,vbo);
		gl.glVertexAttribPointer(vertex_idx,3,GL.GL_FLOAT,false,12,0);
		gl.glEnableVertexAttribArray(vertex_idx);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo);
		gl.glDrawElements(GL.GL_TRIANGLES, length);
		gl.glDisableVertexAttribArray(vertex_idx);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER,0);
		gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,0);
		shader.stop();

		gl.glDepthMask(true);
		if(depth_test) gl.glEnable(GL.GL_DEPTH_TEST);
		if(cull_face) gl.glEnable(GL.GL_CULL_FACE);
	}
}

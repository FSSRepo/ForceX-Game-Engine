
#ifdef useIn2dFlag
attribute vec4 vertex;
attribute vec3 aColor;
uniform vec2 position;
uniform mat2 mat2d;
#else
attribute vec4 vertex;
uniform mat4 uMVPMatrix;
#endif

#ifdef useColor
uniform vec4 color;
#endif

#ifdef useBlendTextured
uniform vec2 texoffset1;
uniform vec2 texoffset2;
uniform vec2 texinfo;
varying vec2 texcoord1;
varying vec2 texcoord2;
varying float blend;
#else
varying vec2 texcoord;
#endif
varying vec4 vColor;

void main(){
	#ifdef useIn2dFlag
		gl_Position = vec4((vertex.xy * mat2d) + position,0.0,1.0);
		gl_PointSize = 0.09;
	#else
		gl_Position = uMVPMatrix * vec4(vertex.xy,0.0,1.0);
	#endif
	#ifdef useBlendTextured
		vec2 texcoord = vertex.zw;
		texcoord /= texinfo.x;
		texcoord1 = texcoord + texoffset1;
		texcoord2 = texcoord + texoffset2;
		blend = texinfo.y;
	#else
		texcoord = vertex.zw;
	#endif
	#ifdef useColor
		vColor = vec4(aColor,1.0) * color;
	#else
		vColor = vec4(aColor,1.0);
	#endif
}

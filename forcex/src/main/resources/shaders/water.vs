attribute vec3 vertexs;

#ifdef lightingFlag
uniform mat4 projView;
uniform mat4 modelMatrix;
uniform vec3 cameraPosition;
uniform vec3 lightPosition;
#else
uniform mat4 mvp;
#endif
uniform float time;
uniform bool isPiscine;

varying vec2 texcoords;

#ifdef lightingFlag
#ifdef reflectionFlag 
varying vec2 cs;
#endif
varying vec3 V;
varying vec3 L;
#endif
const float PI = 3.1415927;

float wave(float t){
	float wv = 0.0;
	wv += sin((vertexs.x + t) * 2.0) * 0.03;
	wv += sin((vertexs.z + t) * PI) * 0.07;
	wv += sin((vertexs.z + t) * 1.0) * 0.04;
	wv += sin(t + vertexs.x * PI) * 0.04;
	return wv;
}

void main(){
	vec4 vp = vec4(vertexs.x,vertexs.y,vertexs.z, 1.0);
	if(!isPiscine){
		vp.y += wave(time);
	}
	#ifdef lightingFlag
		vec4 pos = modelMatrix * vp;
		#ifdef reflectionFlag
			vec4 clip = projView * pos;
			cs = (clip.xy / clip.w) * 0.5 + 0.5;
			gl_Position = clip;
		#else
			gl_Position = projView * pos;
		#endif
		V = normalize(cameraPosition - pos.xyz);
		L = normalize(pos.xyz - lightPosition);
	#else
		gl_Position = mvp * vp;
	#endif
	texcoords = (vertexs.xz * 0.5 + 0.5) * 1.5;
}

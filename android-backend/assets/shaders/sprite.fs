#ifdef GL_ES
precision mediump float;
#endif

#ifdef useBlendTextured
varying float blend;
varying vec2 texcoord1;
varying vec2 texcoord2;
#else
varying vec2 texcoord;
#endif
varying vec4 vColor;
uniform sampler2D uTexture;

void main(){
	#ifdef useBlendTextured
		vec4 tex1 = texture2D(uTexture,texcoord1);
		vec4 tex2 = texture2D(uTexture,texcoord2);
		gl_FragColor = mix(tex2,tex1,blend) * vColor;
	#else
		gl_FragColor = texture2D(uTexture,texcoord) * vColor;
	#endif
}

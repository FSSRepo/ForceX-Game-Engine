#ifdef GL_ES
precision mediump float;
#endif

#ifdef filterAlpha
uniform sampler2D uTexture;
varying vec2 vTexcoords;
#endif

void main() {
	#ifdef filterAlpha
	float alpha = texture2D(uTexture,vTexcoords).a;
	if(alpha < 0.3){
		discard;
	}
	#endif
}

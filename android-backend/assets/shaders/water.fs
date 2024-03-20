#ifdef GL_ES
precision mediump float;
#endif

varying vec2 texcoords;

#ifdef lightingFlag
#ifdef reflectionFlag
uniform sampler2D uReflectTex;
varying vec2 cs;
#endif
varying vec3 V;
varying vec3 L;
uniform sampler2D uNormal;
uniform vec3 lightColor;
#endif

uniform sampler2D uDudv;
uniform sampler2D uTexture;
uniform float moveFactor;

void main(){
	vec2 distorted = texture2D(uDudv, vec2(texcoords.x + moveFactor, texcoords.y)).rg * 0.1;
	distorted = texcoords + vec2(distorted.x, distorted.y + moveFactor);
	distorted = (texture2D(uDudv, distorted).rg * 2.0 - 1.0) * 0.15;
	
	#ifdef lightingFlag
		#ifdef reflectionFlag
			vec2 reflectTexCoords = vec2(cs.x, -cs.y);
			reflectTexCoords += distorted;
    		reflectTexCoords.x = clamp(reflectTexCoords.x, 0.001, 0.999);
    		reflectTexCoords.y = clamp(reflectTexCoords.y, -0.999, -0.001);
  		#endif
    	vec3 N = normalize(texture2D(uNormal,distorted).rgb * 2.0 - 1.0);
		float spec = pow(dot(reflect(L,N),V),4.0);
		#ifdef reflectionFlag
			vec3 reflectColor = texture2D(uReflectTex,reflectTexCoords).rgb;
			gl_FragColor.rgb = mix(reflectColor, vec3(0.05,0.24,0.3),0.5) + (spec * lightColor);
			gl_FragColor.a = 1.0;
			if(spec < 0.6){
				gl_FragColor.a = 0.45;
			}
		#else
			gl_FragColor = texture2D(uTexture,distorted) + (spec * vec4(lightColor,0.0));
			if(spec < 0.3){
				gl_FragColor.a = 0.45;
			}
		#endif
	#else
		gl_FragColor = mix(texture2D(uTexture,distorted), vec4(0.05,0.05,0.1,1.0), 0.3);
		gl_FragColor.a = 0.35;
	#endif
}

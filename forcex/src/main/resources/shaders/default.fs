#ifdef GL_ES
precision mediump float;
#endif

varying vec2 vTexCoord;

#ifdef colorFlag
varying vec4 vColor;
#endif

#ifdef lightingFlag
varying vec3 vNormal;
varying vec3 L;
varying vec3 V;
uniform vec3 lightAmbient;
uniform vec3 lightColor;
#endif

#ifdef fogFlag
varying float vFogEffect;
uniform vec3 uFogColor;
#endif

uniform sampler2D uDiffuseTexture;

#ifdef normalMapFlag
uniform sampler2D uNormalTexture;
varying mat3 TBN;
#endif

#ifdef reflectionCubeMapFlag
varying vec3 vReflectedCoords;
uniform samplerCube uReflectTexture;
uniform float mat_reflection;
#endif

#ifdef shadowMapFlag
uniform sampler2D uShadowTexture;
uniform float shadowFactor;
#ifdef shadowPCF
uniform vec2 ShadowSize;
#endif
varying vec4 vShadowCoords;
#endif

#ifdef clipPlane
varying float clip_factor;
#endif

#ifdef lightingFlag
uniform float mat_specular;
uniform float mat_diffuse;
uniform float mat_ambient;
#endif
uniform vec4 mat_color;
#ifdef normalMapFlag
uniform bool mat_useNormalMap;
#endif

#ifdef shadowMapFlag
float calcBias(float ld)
{
	float cosTheta = clamp(ld, 0.0, 1.0 );
 	float bias = 0.0001 * tan(acos(cosTheta));
	bias = clamp(bias, 0.0, 0.001);
 	return bias;
}
	
#ifdef shadowPCF
	float lookup(vec2 offset, float ld){
		float distanceFromLight = texture2D(uShadowTexture,vShadowCoords.xy + offset).z;
		float bias = calcBias(ld);
		return (distanceFromLight > vShadowCoords.z - bias) ? 1.0 : shadowFactor;
	}
	
	float shadowMap(float ld){
		float shadow = 1.0;
		for(float y = -1.0;y <= 1.0;y++){
			for(float x = -1.0;x <= 1.0;x++){
				shadow += lookup(vec2(x,y) * ShadowSize,ld);
			}
		}
		shadow *= 0.0625;
		return shadow + 0.2;
	}
#else
	float shadowMap(float ld){
		float distanceFromLight = texture2D(uShadowTexture,vShadowCoords.xy).z;
		float bias = calcBias(ld);
		return (distanceFromLight > vShadowCoords.z - bias) ? 1.0 : shadowFactor;
	}
#endif
#endif

float lerp(float a,float b,float f){
	return a + f * (b - a);
}

void main(){
	#ifdef clipPlane
		if(clip_factor < 0.0)
			discard;
	#endif
	
	vec4 texcolor = texture2D(uDiffuseTexture,vTexCoord);
	
	#ifdef alphaTestFlag
	if(texcolor.a < 0.2)
		discard;
	#endif
	
	#ifdef lightingFlag
	#ifdef normalMapFlag
	float diff = 0.0;
	if(mat_useNormalMap){
		vec3 N = normalize(TBN * (texture2D(uNormalTexture,vTexCoord).rgb * 2.0 - 1.0));
		diff = mat_diffuse * max(dot(N,L),0.0);
		float spec = mat_specular * pow(max(dot(reflect(-L,N),V),0.0),5.0);
		vec3 lighting = (mat_ambient * lightAmbient) + (lightColor * (diff + spec));
		gl_FragColor.rgb = texcolor.rgb * mat_color.rgb * lighting;
		gl_FragColor.a  = texcolor.a * mat_color.a;
	}else{
		diff = mat_diffuse * max(dot(vNormal,L),0.0);
		float spec = mat_specular * pow(max(dot(reflect(-L,vNormal),V),0.0),5.0);
		vec3 lighting = (mat_ambient * lightAmbient) + (lightColor * (diff + spec));
		gl_FragColor.rgb = texcolor.rgb * mat_color.rgb * lighting;
		gl_FragColor.a  = texcolor.a * mat_color.a;
	}
	#else
		float diff = mat_diffuse * max(dot(vNormal,L),0.0);
		float spec = mat_specular * pow(max(dot(reflect(-L,vNormal),V),0.0),5.0);
		vec3 lighting = (mat_ambient * lightAmbient) + (lightColor * (diff + spec));
		gl_FragColor.rgb = texcolor.rgb * mat_color.rgb * lighting;
		gl_FragColor.a  = texcolor.a * mat_color.a;
	#endif
	#else
		#ifdef colorFlag
			gl_FragColor = texcolor * vColor;
		#else
			gl_FragColor = texcolor * mat_color;
		#endif
	#endif
	
	#ifdef gammaCorrection
		gl_FragColor.rgb = pow(gl_FragColor.rgb,vec3(0.6));
	#endif
	
	#ifdef reflectionCubeMapFlag
		gl_FragColor = mix(gl_FragColor,textureCube(uReflectTexture,vReflectedCoords),mat_reflection);
	#endif
	
	#ifdef shadowMapFlag
		gl_FragColor.rgb *= shadowMap(diff);
	#endif
	
	#ifdef fogFlag
	gl_FragColor.rgb = mix(uFogColor,gl_FragColor.rgb,vFogEffect);
	#endif
}

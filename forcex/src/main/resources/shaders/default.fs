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
/*
 * Bias in normalized depth. A constant term (which avoids acne on surfaces
 * facing the light, where the slope term is zero) is combined with a
 * slope-proportional term.
 * With the default ortho frustum (near 0.1, far 300), 0.001 of normalized
 * depth is roughly 0.3 world units.
 */
const float SHADOW_CONSTANT_BIAS = 0.0005;
const float SHADOW_SLOPE_BIAS = 0.0005;

float calcBias(float ld)
{
	float cosTheta = clamp(ld, 0.0, 1.0 );
	float slope = clamp(tan(acos(cosTheta)), 0.0, 10.0);
	return SHADOW_CONSTANT_BIAS + (SHADOW_SLOPE_BIAS * slope / 10.0);
}

bool outsideShadowMap(vec2 uv){
	return uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0;
}
	
#ifdef shadowPCF
	float lookup(vec2 offset, float ld){
		float distanceFromLight = texture2D(uShadowTexture,vShadowCoords.xy + offset).r;
		float bias = calcBias(ld);
		return (distanceFromLight > vShadowCoords.z - bias) ? 1.0 : 0.0;
	}
	
	float shadowMap(float ld){
		if(outsideShadowMap(vShadowCoords.xy)){
			return 1.0;
		}
		float visibility = 0.0;
		for(float y = -1.0;y <= 1.0;y++){
			for(float x = -1.0;x <= 1.0;x++){
				vec2 offset = vec2(x,y) * ShadowSize;
				if(outsideShadowMap(vShadowCoords.xy + offset)){
					visibility += 1.0;
				}else{
					visibility += lookup(offset,ld);
				}
			}
		}
		visibility /= 9.0;
		return mix(shadowFactor, 1.0, visibility);
	}
#else
	float shadowMap(float ld){
		if(outsideShadowMap(vShadowCoords.xy)){
			return 1.0;
		}
		float distanceFromLight = texture2D(uShadowTexture,vShadowCoords.xy).r;
		float bias = calcBias(ld);
		float visibility = (distanceFromLight > vShadowCoords.z - bias) ? 1.0 : 0.0;
		return mix(shadowFactor, 1.0, visibility);
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
	
	float diff = 0.0;
	
	#ifdef lightingFlag
	float spec = 0.0;
	#ifdef normalMapFlag
	if(mat_useNormalMap){
		vec3 N = normalize(TBN * (texture2D(uNormalTexture,vTexCoord).rgb * 2.0 - 1.0));
		diff = mat_diffuse * max(dot(N,L),0.0);
		spec = mat_specular * pow(max(dot(reflect(-L,N),V),0.0),5.0);
	}else{
		diff = mat_diffuse * max(dot(vNormal,L),0.0);
		spec = mat_specular * pow(max(dot(reflect(-L,vNormal),V),0.0),5.0);
	}
	#else
		diff = mat_diffuse * max(dot(vNormal,L),0.0);
		spec = mat_specular * pow(max(dot(reflect(-L,vNormal),V),0.0),5.0);
	#endif
		float shading = 1.0;
		#ifdef shadowMapFlag
		shading = shadowMap(diff);
		#endif
		vec3 lighting = (mat_ambient * lightAmbient) + (lightColor * (diff + spec) * shading);
		gl_FragColor.rgb = texcolor.rgb * mat_color.rgb * lighting;
		gl_FragColor.a  = texcolor.a * mat_color.a;
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
	
	#ifdef fogFlag
	gl_FragColor.rgb = mix(uFogColor,gl_FragColor.rgb,vFogEffect);
	#endif
}

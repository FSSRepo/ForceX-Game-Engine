#ifdef GL_ES
precision highp float;
#endif

varying vec4 params;
uniform sampler2D uTexture;
uniform vec2 pixs;

const float span_max = 8.0;
const float reduce_mul = (1.0 / 8.0);
const float reduce_min = (1.0 / 256.0);
const float half_one = 0.5;
const vec3 luma = vec3(0.3, 0.6, 0.1);
const float six_one = 1.66667;

vec3 fxaa(){
	vec2 uv0 = params.xy;
	vec2 uv1 = params.zw;
	vec3 rgbNW = texture2D(uTexture, uv1).rgb;
	vec3 rgbNE = texture2D(uTexture, uv1 + (vec2(1,0) * pixs)).rgb;
	vec3 rgbSW = texture2D(uTexture, uv1 + (vec2(0,1) * pixs)).rgb;
	vec3 rgbSE = texture2D(uTexture, uv1 + (vec2(1,1) * pixs)).rgb;
	vec3 rgbM =  texture2D(uTexture, uv0).rgb;
    float lumaNW = dot(rgbNW, luma);
    float lumaNE = dot(rgbNE, luma);
    float lumaSW = dot(rgbSW, luma);
    float lumaSE = dot(rgbSE, luma);
    float lumaM  = dot(rgbM,  luma);
    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
    vec2 dir = vec2(-((lumaNW + lumaNE) - (lumaSW + lumaSE)),(lumaNW + lumaSW) - (lumaNE + lumaSE));
    float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * reduce_mul), reduce_min);
    float dirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
    dir = min(vec2(span_max,span_max), max(vec2(-span_max,-span_max), dir * dirMin)) * pixs;
    vec3 rgbA = half_one * 
   		(texture2D(uTexture, uv0 + dir * -six_one).rgb + 
    	 texture2D(uTexture, uv0 + dir * six_one).rgb);
    vec3 rgbB = rgbA * half_one + 0.25 * 
    	(texture2D(uTexture, uv0 + dir * -half_one).rgb + 
    	 texture2D(uTexture, uv0 + dir * half_one).rgb);
    float lumaB = dot(rgbB, luma);
    if((lumaB < lumaMin) || (lumaB > lumaMax)) {
        return rgbA;
    }
    return rgbB;
}

void main(){
	gl_FragColor = vec4(fxaa(),1.0);
}

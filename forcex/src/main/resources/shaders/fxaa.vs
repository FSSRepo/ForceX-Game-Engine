attribute vec2 positions;
uniform vec2 pixs;
varying vec4 params;

const float subpix = 1.0 / 8.0;

void main() {
    gl_Position = vec4(positions,0.0,1.0);
    params.xy = positions * 0.5 + 0.5;
    params.zw = params.xy - (pixs * (0.5 + subpix));
}

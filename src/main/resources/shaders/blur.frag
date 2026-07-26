#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texel;
uniform float u_radius;

void main() {
    float r = max(u_radius, 1.0);
    vec2 step = u_texel * r;
    vec4 sum = texture2D(u_texture, v_texCoords) * 0.2;
    sum += texture2D(u_texture, v_texCoords + vec2(step.x, 0.0)) * 0.15;
    sum += texture2D(u_texture, v_texCoords - vec2(step.x, 0.0)) * 0.15;
    sum += texture2D(u_texture, v_texCoords + vec2(0.0, step.y)) * 0.15;
    sum += texture2D(u_texture, v_texCoords - vec2(0.0, step.y)) * 0.15;
    sum += texture2D(u_texture, v_texCoords + step) * 0.1;
    sum += texture2D(u_texture, v_texCoords - step) * 0.1;
    gl_FragColor = vec4(sum.rgb, sum.a) * v_color;
}

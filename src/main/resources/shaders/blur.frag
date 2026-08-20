#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texel;
uniform vec2 u_direction;
uniform float u_radius;

void main() {
    float r = max(u_radius, 1.0);
    vec2 step = u_texel * u_direction * r;
    vec4 sum = texture2D(u_texture, v_texCoords) * 0.227027;
    sum += texture2D(u_texture, v_texCoords + step * 1.384615) * 0.316216;
    sum += texture2D(u_texture, v_texCoords - step * 1.384615) * 0.316216;
    sum += texture2D(u_texture, v_texCoords + step * 3.230769) * 0.070270;
    sum += texture2D(u_texture, v_texCoords - step * 3.230769) * 0.070270;
    gl_FragColor = vec4(sum.rgb, sum.a) * v_color;
}

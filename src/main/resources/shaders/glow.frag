#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_intensity;

void main() {
    vec4 tex = texture2D(u_texture, v_texCoords);
    float edge = abs(v_texCoords.x - 0.5) * 2.0;
    edge = max(edge, abs(v_texCoords.y - 0.5) * 2.0);
    float glow = smoothstep(0.55, 1.0, edge) * u_intensity;
    vec3 glowRgb = vec3(1.0, 0.85, 0.4) * glow;
    gl_FragColor = vec4(tex.rgb * v_color.rgb + glowRgb, tex.a * v_color.a);
}

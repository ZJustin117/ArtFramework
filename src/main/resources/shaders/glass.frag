#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_texel;
uniform float u_radius;
uniform float u_tint;
uniform float u_time;

void main() {
    float r = max(u_radius, 1.0);
    vec2 step = u_texel * r;
    // cheap liquid-ish UV wobble
    vec2 uv = v_texCoords;
    uv.x += sin(uv.y * 18.0 + u_time * 1.7) * u_texel.x * r * 0.8;
    uv.y += cos(uv.x * 14.0 + u_time * 1.3) * u_texel.y * r * 0.8;

    vec4 c = texture2D(u_texture, uv) * 0.35;
    c += texture2D(u_texture, uv + vec2(step.x, 0.0)) * 0.15;
    c += texture2D(u_texture, uv - vec2(step.x, 0.0)) * 0.15;
    c += texture2D(u_texture, uv + vec2(0.0, step.y)) * 0.15;
    c += texture2D(u_texture, uv - vec2(0.0, step.y)) * 0.15;
    c += texture2D(u_texture, uv + step) * 0.025;
    c += texture2D(u_texture, uv - step) * 0.025;

    // frosted lift + cool tint
    vec3 tint = mix(c.rgb, vec3(0.75, 0.85, 1.0), clamp(u_tint, 0.0, 1.0) * 0.35);
    float edge = max(abs(v_texCoords.x - 0.5), abs(v_texCoords.y - 0.5)) * 2.0;
    float rim = smoothstep(0.75, 1.0, edge) * 0.25;
    tint += vec3(rim);
    gl_FragColor = vec4(tint, clamp(v_color.a, 0.0, 1.0));
}

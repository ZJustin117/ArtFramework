#ifdef GL_ES
precision mediump float;
#endif
varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_intensity;
uniform float u_angle;
uniform float u_width;
uniform float u_phase;

void main() {
    // White square UV; diagonal traveling band (bright, readable under translucent panel).
    float rad = radians(u_angle);
    float cs = cos(rad);
    float sn = sin(rad);
    float axis = v_texCoords.x * cs + v_texCoords.y * sn;
    // Map axis roughly to 0..1 for typical diagonal.
    float axis01 = axis * 0.5 + 0.5;
    float halfW = max(0.04, u_width * 0.55);
    float d = abs(axis01 - u_phase);
    d = min(d, abs(axis01 + 1.0 - u_phase));
    d = min(d, abs(axis01 - 1.0 - u_phase));
    float band = 1.0 - smoothstep(0.0, halfW, d);
    band = band * band;
    float glow = band * max(u_intensity, 0.0);
    // Hot core + cool halo
    vec3 core = vec3(1.0, 1.0, 1.0) * glow;
    vec3 halo = vec3(0.45, 0.85, 1.0) * glow * 0.85;
    vec3 wave = core * 0.55 + halo;
    // Outside the band this is an overlay with no pixels. A baseline black alpha turns the
    // full-frame target into an unintended screen dimmer.
    float a = clamp(glow * 0.95, 0.0, 1.0) * v_color.a;
    gl_FragColor = vec4(wave, a);
}

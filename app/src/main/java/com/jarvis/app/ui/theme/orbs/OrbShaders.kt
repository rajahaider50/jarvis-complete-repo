package com.jarvis.app.ui.theme.orbs

/** AGSL (RuntimeShader) sources, Android 13+ only. Every source declares and uses resolution, time and level. */
object OrbShaders {
    private const val NOISE = """
uniform float2 resolution;
uniform float time;
uniform float level;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.0 + float2(3.1, 1.7);
        a *= 0.5;
    }
    return v;
}
"""

    val GLASS: String = NOISE + """
half4 main(float2 fc) {
    float2 p = (fc - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
    float r = length(p);
    if (r > 1.0) return half4(0.0);
    float t = time * 0.4;
    float amp = 0.3 + level * 0.7;
    float2 q = p * 1.6;
    float n1 = fbm(q + float2(t, -t * 0.7));
    float n2 = fbm(q * 1.3 - float2(t * 0.8, t) + n1 * amp * 2.0);
    float3 col = mix(float3(1.0, 0.25, 0.8), float3(0.2, 0.85, 1.0), smoothstep(0.3, 0.7, n1));
    col = mix(col, float3(1.0, 0.75, 0.3), smoothstep(0.55, 0.85, n2) * 0.6);
    col *= 0.5 + 1.2 * n2;
    float core = smoothstep(0.95, 0.0, r);
    col *= core;
    float rim = smoothstep(0.80, 0.98, r) * (1.0 - smoothstep(0.98, 1.0, r));
    col += float3(0.15, 0.4, 1.0) * rim * 1.5;
    col = clamp(col, 0.0, 1.0);
    float a = clamp(max(core * 0.9, rim), 0.0, 1.0);
    return half4(half3(col * a), half(a));
}
"""

    val FIRE: String = NOISE + """
half4 main(float2 fc) {
    float2 p = (fc - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
    float r = length(p);
    float ang = atan(p.y, p.x);
    float amp = 0.10 + level * 0.18;
    float2 dir = float2(cos(ang), sin(ang));
    float n = fbm(dir * 2.5 + float2(r * 3.0, time * 1.2) + float2(0.0, -time * 0.6));
    float radius = 0.60 + (n - 0.5) * amp * 2.0;
    float d = abs(r - radius);
    float glow = exp(-d * 9.0);
    float flame = smoothstep(0.30, 0.0, d) * n;
    float inten = clamp(glow * 1.1 + flame * 0.9, 0.0, 1.0);
    float3 col = mix(float3(0.9, 0.1, 0.0), float3(1.0, 0.85, 0.3), inten) * inten;
    col = clamp(col, 0.0, 1.0);
    return half4(half3(col), half(inten));
}
"""

    val SUN: String = NOISE + """
half4 main(float2 fc) {
    float2 p = (fc - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
    float r = length(p);
    if (r > 1.0) return half4(0.0);
    float disk = 0.78;
    float t = time * 0.15;
    float3 col = float3(0.0);
    float a = 0.0;
    if (r < disk) {
        float2 sp = p / disk;
        float sr = length(sp);
        float z = sqrt(max(1.0 - sr * sr, 0.0));
        float2 uv = sp / (z + 1.0) * 2.0;
        float n = fbm(uv * 2.2 + float2(t, -t));
        float n2 = fbm(uv * 4.5 - float2(t * 1.3, t) + n);
        float heat = clamp(n * 0.6 + n2 * 0.7 + (1.0 - sr) * 0.3 + level * 0.2, 0.0, 1.0);
        col = mix(float3(0.6, 0.1, 0.0), float3(1.0, 0.8, 0.25), heat);
        col = mix(col, float3(1.0, 0.95, 0.7), smoothstep(0.75, 1.0, heat));
        a = 1.0;
    } else {
        float k = 1.0 - (r - disk) / (1.0 - disk);
        float cor = pow(clamp(k, 0.0, 1.0), 2.5) * (0.7 + level);
        col = float3(1.0, 0.5, 0.1) * cor;
        a = clamp(cor, 0.0, 1.0);
    }
    col = clamp(col, 0.0, 1.0);
    return half4(half3(col * a), half(a));
}
"""

    val GOLD: String = NOISE + """
half4 main(float2 fc) {
    float2 p = (fc - 0.5 * resolution) / (0.5 * min(resolution.x, resolution.y));
    float r = length(p);
    float ang = atan(p.y, p.x);
    float wob = 0.03 + level * 0.08;
    float3 col = float3(0.0);
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float rad = 0.6 + wob * sin(ang * (3.0 + fi) + time * (1.2 + fi * 0.5) + fi * 2.0);
        float d = abs(r - rad);
        float line = exp(-d * d * (900.0 - fi * 200.0));
        float glow = exp(-d * 6.0) * 0.25;
        float3 c = mix(float3(1.0, 0.78, 0.2), float3(1.0, 0.95, 0.7), fi / 2.0);
        col += c * (line + glow);
    }
    col = clamp(col, 0.0, 1.0);
    float a = max(col.r, max(col.g, col.b));
    return half4(half3(col), half(a));
}
"""
}

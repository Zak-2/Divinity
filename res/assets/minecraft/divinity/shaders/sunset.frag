#version 330 core

uniform vec2 iResolution;
uniform float iTime;
uniform vec2 iMouse;

out vec4 fragColor;

void main()
{
    vec2 I = gl_FragCoord.xy;
    vec4 O = vec4(0.0);

    // Time for animation
    float t = iTime;
    float i = 0.0;
    float z = 0.0;
    float d;
    float s;

    // Clear fragcolor and raymarch with 100 iterations
    for (O *= i; i++ < 100.0; )
    {
        // Compute raymarch sample point
        vec3 p = z * normalize(vec3(I + I, 0.0) - iResolution.xyy);

        // Turbulence loop
        for (d = 5.0; d < 200.0; d += d)
            p += 0.6 * sin(p.yzx * d - 0.2 * t) / d;

        // Compute distance (smaller steps in clouds when s is negative)
        z += d = 0.005 + max(s = 0.3 - abs(p.y), -s * 0.2) / 4.0;

        // Coloring with sine wave using cloud depth and x-coordinate
        O += (cos(s / 0.07 + p.x + 0.5 * t - vec4(3.0, 4.0, 5.0, 0.0)) + 1.5) * exp(s / 0.1) / d;
    }

    // Tanh tonemapping
    O = tanh(O * O / 4e8);

    fragColor = O;
}

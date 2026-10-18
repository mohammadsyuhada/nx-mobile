// NX Redux Mobile "Grid" effect: LibretroDroid's LCD shader (GPL-3.0, Filippo Scognamiglio) in RetroArch form.
#if defined(VERTEX)
attribute vec4 VertexCoord;
attribute vec4 TexCoord;
uniform mat4 MVPMatrix;
uniform vec2 TextureSize;
uniform vec2 OutputSize;
varying vec2 coords;
varying vec2 screenCoords;
varying float screenMaskStrength;
varying float density;
void main() {
    gl_Position = MVPMatrix * VertexCoord;
    coords = TexCoord.xy;
    screenCoords = coords * TextureSize;
    density = min(OutputSize.x / TextureSize.x, OutputSize.y / TextureSize.y);
    screenMaskStrength = smoothstep(2.0, 6.0, density);
}
#elif defined(FRAGMENT)
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform sampler2D Texture;
uniform vec2 TextureSize;
varying vec2 coords;
varying vec2 screenCoords;
varying float screenMaskStrength;
varying float density;
#define INTENSITY 0.25
#define BRIGHTBOOST 0.25
void main() {
    vec2 threshold = vec2(1.0 / density);
    vec2 x = fract(screenCoords);
    x = 0.5 * (smoothstep(vec2(0.0), threshold, x) + smoothstep(vec2(1.0) - threshold, vec2(1.0), x));
    vec2 sharpCoords = (floor(screenCoords) + x) / TextureSize;
    vec3 texel = texture2D(Texture, sharpCoords).rgb;
    vec3 pixelHigh = ((1.0 + BRIGHTBOOST) - (0.2 * texel)) * texel;
    vec3 pixelLow  = ((1.0 - INTENSITY) + (0.1 * texel)) * texel;
    vec2 c = fract(screenCoords) * 2.0 - vec2(1.0);
    c = c * c;
    float mask = 1.0 - c.x - c.y;
    gl_FragColor = vec4(mix(texel, mix(pixelLow, pixelHigh, mask), screenMaskStrength), 1.0);
}
#endif

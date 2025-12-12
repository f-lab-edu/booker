export interface ShaderProgram {
  name: string;
  vertexShader: string;
  fragmentShader: string;
}

const vertexShaderSource = `
  attribute vec4 aPosition;
  void main() {
    gl_Position = aPosition;
  }
`;

const fragmentShaderSource = `
  precision mediump float;
  uniform vec2 iResolution;
  uniform float iTime;
  uniform vec2 iMouse;

  // Spectral color function
  vec3 spectral_color(float t) {
    vec3 c;
    float l = 0.0;

    if (t >= 0.0 && t < 0.16) {
      l = (t - 0.0) / (0.16 - 0.0);
      c = mix(vec3(0.5, 0.0, 1.0), vec3(0.0, 0.0, 1.0), l);
    } else if (t >= 0.16 && t < 0.42) {
      l = (t - 0.16) / (0.42 - 0.16);
      c = mix(vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 1.0), l);
    } else if (t >= 0.42 && t < 0.6425) {
      l = (t - 0.42) / (0.6425 - 0.42);
      c = mix(vec3(0.0, 1.0, 1.0), vec3(0.0, 1.0, 0.0), l);
    } else if (t >= 0.6425 && t < 0.8575) {
      l = (t - 0.6425) / (0.8575 - 0.6425);
      c = mix(vec3(0.0, 1.0, 0.0), vec3(1.0, 1.0, 0.0), l);
    } else if (t >= 0.8575 && t <= 1.0) {
      l = (t - 0.8575) / (1.0 - 0.8575);
      c = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), l);
    }

    return c;
  }

  void main() {
    vec2 uv = gl_FragCoord.xy / iResolution.xy;
    vec2 p = (2.0 * gl_FragCoord.xy - iResolution.xy) / min(iResolution.x, iResolution.y);

    // Mouse influence
    vec2 mouseNorm = iMouse / iResolution.xy;
    float mouseInfluence = length(uv - mouseNorm) * 2.0;

    // Animated waves
    float d = length(p);
    float angle = atan(p.y, p.x);

    float wave1 = sin(d * 8.0 - iTime * 1.5 + mouseInfluence) * 0.5 + 0.5;
    float wave2 = sin(angle * 6.0 + iTime * 0.8) * 0.5 + 0.5;
    float wave3 = sin(d * 12.0 + angle * 4.0 - iTime * 2.0) * 0.5 + 0.5;

    // Combine waves
    float pattern = (wave1 + wave2 + wave3) / 3.0;

    // Add time-based color shift
    float colorShift = fract(pattern + iTime * 0.1);

    // Get spectral color
    vec3 color = spectral_color(colorShift);

    // Add subtle vignette
    float vignette = smoothstep(0.8, 0.2, d);
    color *= vignette * 0.8 + 0.2;

    // Brightness modulation
    color *= 0.6 + 0.4 * pattern;

    gl_FragColor = vec4(color, 1.0);
  }
`;

export const spectralWaveShader: ShaderProgram = {
  name: 'Spectral Waves',
  vertexShader: vertexShaderSource,
  fragmentShader: fragmentShaderSource,
};

const dreamyBlobFragmentShader = `
  precision highp float;
  uniform vec2 iResolution;
  uniform float iTime;
  uniform vec2 iMouse;

  // Dramatic purple/white palette like reference
  const vec3 deepPurple = vec3(0.15, 0.1, 0.25); // Dark purple base
  const vec3 midPurple = vec3(0.35, 0.25, 0.5); // Medium purple
  const vec3 violet = vec3(0.55, 0.36, 0.96); // Bright violet #8b5cf6
  const vec3 lavender = vec3(0.7, 0.55, 0.95); // Light purple
  const vec3 lightGray = vec3(0.6, 0.6, 0.65); // Gray for blobs
  const vec3 brightWhite = vec3(0.95, 0.95, 1.0); // Almost white

  // Smooth minimum function for metaball blending
  float smoothMin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
  }

  // Hash function for random numbers
  float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
  }

  // Simple noise
  float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
  }

  // Generate stars
  float stars(vec2 p, float density) {
    vec2 grid = floor(p * density);
    vec2 f = fract(p * density);

    float starHash = hash(grid);

    // Only create star if hash is above threshold
    if (starHash < 0.95) return 0.0;

    // Star position within grid cell
    vec2 starPos = vec2(hash(grid + 0.1), hash(grid + 0.2));
    float dist = length(f - starPos);

    // Star brightness varies
    float brightness = hash(grid + 0.3);

    // Twinkling effect
    float twinkle = sin(iTime * 2.0 + starHash * 100.0) * 0.5 + 0.5;

    // Star size
    float star = smoothstep(0.02, 0.0, dist) * brightness * twinkle;

    return star;
  }

  // Wavy horizontal lines
  float waves(vec2 p, float time) {
    float wave1 = sin(p.x * 2.0 + time * 0.5) * 0.05;
    float wave2 = sin(p.x * 3.0 - time * 0.3) * 0.03;
    float wave3 = sin(p.x * 1.5 + time * 0.7) * 0.04;

    float totalWave = wave1 + wave2 + wave3;

    // Create horizontal bands
    float bands = sin((p.y + totalWave) * 8.0) * 0.5 + 0.5;
    bands = smoothstep(0.3, 0.7, bands);

    return bands * 0.15;
  }

  void main() {
    vec2 uv = gl_FragCoord.xy / iResolution.xy;
    vec2 p = (2.0 * gl_FragCoord.xy - iResolution.xy) / min(iResolution.x, iResolution.y);

    float t = iTime * 0.3;

    // Mouse/cursor interaction
    vec2 mouseNorm = iMouse / iResolution.xy;
    vec2 mouseP = mouseNorm * 2.0 - 1.0;
    mouseP.x *= iResolution.x / iResolution.y;

    // All blobs centered on cursor with tiny offsets for organic feel
    vec2 blob1 = mouseP + vec2(sin(t * 0.5) * 0.05, cos(t * 0.6) * 0.05);
    vec2 blob2 = mouseP + vec2(cos(t * 0.4) * 0.05, sin(t * 0.5) * 0.05);
    vec2 blob3 = mouseP + vec2(sin(t * 0.6) * 0.05, cos(t * 0.7) * 0.05);

    // Huge blob radius for dramatic effect
    float d1 = length(p - blob1) / 3.5;
    float d2 = length(p - blob2) / 3.0;
    float d3 = length(p - blob3) / 3.5;

    // Blend blobs
    float k = 0.8;
    float blobField = smoothMin(smoothMin(d1, d2, k), d3, k);
    float blob = smoothstep(1.2, 0.1, blobField);

    // Cursor glow distance
    float mouseDist = length(p - mouseP);
    float cursorGlow = exp(-mouseDist * 0.8) * 0.8; // Higher brightness
    float cursorInfluence = smoothstep(3.5, 0.0, mouseDist);

    // Base: deep purple to mid purple gradient
    vec3 baseColor = mix(deepPurple, midPurple, uv.y * 0.6 + 0.2);

    // Large dramatic blobs - REDUCED brightness
    vec3 blobColor = mix(lightGray, brightWhite, blob * 0.5);

    // Mix blob with base - LESS intense blobs
    vec3 color = mix(baseColor, blobColor, blob * 0.4);

    // Add violet accents - reduced
    color = mix(color, violet, blob * 0.2);
    color = mix(color, lavender, blob * blob * 0.25);

    // Cursor interaction - balanced
    vec3 glowColor = mix(violet, lavender, cursorGlow * 0.3);
    color = mix(color, glowColor, cursorInfluence * 0.6);
    // Bright white highlight at cursor
    color += brightWhite * cursorGlow * 0.4;

    // Subtle pulsing
    float pulse = sin(t * 2.0) * 0.5 + 0.5;
    color = mix(color, violet, pulse * blob * 0.1);

    // Very subtle vignette
    float vignette = smoothstep(2.0, 0.3, length(p));
    color *= vignette * 0.9 + 0.1;

    // Slightly brighter overall
    color *= 1.15;

    gl_FragColor = vec4(color, 1.0);
  }
`;

export const dreamyBlobShader: ShaderProgram = {
  name: 'Dreamy Blobs',
  vertexShader: vertexShaderSource,
  fragmentShader: dreamyBlobFragmentShader,
};

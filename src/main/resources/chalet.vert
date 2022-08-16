#version 450

layout(location=0) in vec3 pos;
layout(location=1) in vec2 coords;

layout(binding=1) uniform UniformBuffer {
    mat4 matrix;
};

layout(location=0) out vec2 fragCoords;

void main() {
    gl_Position = matrix * vec4(pos, 1.0);
    fragCoords = coords;
}

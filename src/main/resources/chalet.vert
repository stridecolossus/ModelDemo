#version 450

layout(set = 0, binding = 1) uniform UniformBuffer {
    mat4 model;
    mat4 view;
    mat4 projection;
};

layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 coords;

layout(location = 0) out vec2 fragCoords;

void main() {
    gl_Position = projection * view * model * vec4(pos, 1.0);
    fragCoords = coords;
}

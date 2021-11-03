#version 450

layout(location = 0) in vec3 inCoords;
layout(set = 0, binding = 1) uniform samplerCube cubemap;
layout(location = 0) out vec4 outColour;

void main() {
    outColour = texture(cubemap, inCoords);
}

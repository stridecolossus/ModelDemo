package org.sarge.jove.demo.model;

import org.sarge.jove.geometry.Matrix;
import org.sarge.jove.geometry.Point;
import org.sarge.jove.geometry.Vector;
import org.sarge.jove.platform.vulkan.VkBufferUsage;
import org.sarge.jove.platform.vulkan.VkMemoryProperty;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.scene.Camera;
import org.sarge.jove.scene.Projection;
import org.sarge.jove.util.MathsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CameraConfiguration {
	@Bean
	public static Camera camera() {
		final Camera cam = new Camera();
		cam.move(new Point(0, -0.5f, -2));
		return cam;
	}

	@Bean
	public static Matrix matrix(Swapchain swapchain, Camera cam) {
		// Create perspective projection
		final Matrix projection = Projection.DEFAULT.matrix(0.1f, 100, swapchain.extents());

		// TODO - temporary
		// Construct model transform
		final Matrix x = Matrix.rotation(Vector.X, MathsUtil.toRadians(90));
		final Matrix y = Matrix.rotation(Vector.Y, MathsUtil.toRadians(-120));
		final Matrix model = y.multiply(x);

		// Create matrix
		return projection.multiply(cam.matrix()).multiply(model);
	}

	@Bean
	public static VulkanBuffer uniform(LogicalDevice dev, AllocationService allocator, Matrix matrix) {
		final MemoryProperties<VkBufferUsage> props = new MemoryProperties.Builder<VkBufferUsage>()
				.usage(VkBufferUsage.UNIFORM_BUFFER)
				.required(VkMemoryProperty.HOST_VISIBLE)
				.required(VkMemoryProperty.HOST_COHERENT)
				.build();

		return VulkanBuffer.create(dev, allocator, matrix.length(), props);
	}
}

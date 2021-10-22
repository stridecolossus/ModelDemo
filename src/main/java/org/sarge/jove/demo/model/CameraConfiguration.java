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
import org.sarge.jove.scene.Projection;
import org.sarge.jove.util.MathsUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CameraConfiguration {
	@Bean
	public static Matrix matrix(Swapchain swapchain) {
		// Create perspective projection
		final Matrix projection = Projection.DEFAULT.matrix(0.1f, 100, swapchain.extents());

		// Construct view transform
		final Matrix trans = new Matrix.Builder()
				.identity()
				.column(3, new Point(0, -0.5f, -2))
				.build();

		final Matrix rot = new Matrix.Builder()
				.identity()
				.row(0, Vector.X)
				.row(1, Vector.Y)
				.row(2, Vector.Z)
				.build();

		final Matrix view = rot.multiply(trans);

		// TODO - temporary
		// Construct model transform
		final Matrix x = Matrix.rotation(Vector.X, MathsUtil.toRadians(90));
		final Matrix y = Matrix.rotation(Vector.Y, MathsUtil.toRadians(-120));
		final Matrix model = y.multiply(x);

		// Create matrix
		return projection.multiply(view).multiply(model);
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

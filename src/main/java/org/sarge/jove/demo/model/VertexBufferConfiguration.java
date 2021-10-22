package org.sarge.jove.demo.model;

import org.sarge.jove.model.Model;
import org.sarge.jove.model.ModelLoader;
import org.sarge.jove.platform.vulkan.VkBufferUsage;
import org.sarge.jove.platform.vulkan.VkMemoryProperty;
import org.sarge.jove.platform.vulkan.common.Command.Pool;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.util.DataSource;
import org.sarge.jove.util.ResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexBufferConfiguration {
	@Bean
	public static Model model(DataSource src) {
		final var loader = ResourceLoader.of(src, new ModelLoader());
System.out.println("loader="+loader);
		return loader.apply("chalet.model");
	}

	@Bean
	public static VulkanBuffer vbo(LogicalDevice dev, AllocationService allocator, Pool graphics, Model model) {
		// Create staging buffer
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, model.vertices());

		// Init VBO memory properties
		final MemoryProperties<VkBufferUsage> props = new MemoryProperties.Builder<VkBufferUsage>()
				.usage(VkBufferUsage.TRANSFER_DST)
				.usage(VkBufferUsage.VERTEX_BUFFER)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create VBO
		final VulkanBuffer vbo = VulkanBuffer.create(dev, allocator, staging.length(), props);

		// Copy staging to VBO
		staging.copy(vbo).submitAndWait(graphics);

		// Release staging
		staging.close();

		return vbo;
	}
}

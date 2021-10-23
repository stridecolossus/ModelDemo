package org.sarge.jove.demo.model;

import org.sarge.jove.common.Bufferable;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private AllocationService allocator;
	@Autowired private Pool graphics;

	@Bean
	public static Model model(DataSource src) throws Exception {
//		final var adapter = ResourceLoader.of(src, new ObjectModelLoader());
//		final Model model = adapter.apply("chalet.obj").iterator().next();
//		final ModelLoader out = new ModelLoader();
//		out.write(model, new FileOutputStream("./src/main/resources/chalet.model"));

		final var loader = ResourceLoader.of(src, new ModelLoader());
		return loader.apply("chalet.model");
	}

	@Bean
	public static Model.Header header(Model model) {
		return model.header();
	}

	@Bean
	public VulkanBuffer vbo(Model model) {
		return buffer(model.vertices(), VkBufferUsage.VERTEX_BUFFER);
	}

	@Bean
	public VulkanBuffer index(Model model) {
		return buffer(model.index().get(), VkBufferUsage.INDEX_BUFFER);
	}

	private VulkanBuffer buffer(Bufferable data, VkBufferUsage usage) {
		// Create staging buffer
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, data);

		// Init buffer memory properties
		final MemoryProperties<VkBufferUsage> props = new MemoryProperties.Builder<VkBufferUsage>()
				.usage(VkBufferUsage.TRANSFER_DST)
				.usage(usage)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create buffer
		final VulkanBuffer buffer = VulkanBuffer.create(dev, allocator, staging.length(), props);

		// Copy staging to buffer
		staging.copy(buffer).submitAndWait(graphics);

		// Release staging
		staging.close();

		return buffer;
	}
}

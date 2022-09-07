package org.sarge.jove.demo.model;

import java.io.*;
import java.nio.file.Paths;

import org.sarge.jove.common.Bufferable;
import org.sarge.jove.io.*;
import org.sarge.jove.model.*;
import org.sarge.jove.platform.obj.ObjectModelLoader;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.memory.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
public class ModelConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private AllocationService allocator;
	@Autowired private Pool graphics;

	@Bean
	public static Model model(DataSource data) throws IOException {
		final var loader = new ResourceLoaderAdapter<>(data, new ModelLoader());
		return loader.load("chalet.model");
	}

	@Bean
	public VertexBuffer vbo(Model model) {
		final VulkanBuffer buffer = buffer(model.vertices(), VkBufferUsageFlag.VERTEX_BUFFER);
		return new VertexBuffer(buffer);
	}

	@Bean
	public VulkanBuffer index(Model model) {
		final VulkanBuffer buffer = buffer(model.index().get(), VkBufferUsageFlag.INDEX_BUFFER);
		return new IndexBuffer(buffer, model.count());
	}

	protected VulkanBuffer buffer(Bufferable data, VkBufferUsageFlag usage) {
		// Create staging buffer
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, data);

		// Init buffer memory properties
		final var props = new MemoryProperties.Builder<VkBufferUsageFlag>()
				.usage(VkBufferUsageFlag.TRANSFER_DST)
				.usage(usage)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create buffer
		final VulkanBuffer buffer = VulkanBuffer.create(dev, allocator, staging.length(), props);

		// Copy staging to buffer
		staging.copy(buffer).submit(graphics);

		// Release staging
		staging.destroy();

		return buffer;
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
//		final var adapter = new ResourceLoaderAdapter<>(data, new ObjectModelLoader());
//		final Model model = adapter.load("chalet.obj").iterator().next();
//		final ModelLoader loader2 = new ModelLoader();
//		loader2.save(model, new DataOutputStream(new FileOutputStream("../Data/chalet.model")));

		final DataSource src = FileDataSource.home(Paths.get("workspace/Demo/Data"));
		final var loader = new ResourceLoaderAdapter<>(src, new ObjectModelLoader());
		final Model model = loader.load("chalet.obj").iterator().next();

		final ModelLoader out = new ModelLoader();
		out.save(model, new DataOutputStream(new FileOutputStream("/Users/Sarge/workspace/Demo/Data/chalet.model")));
	}
}

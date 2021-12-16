package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.common.Bufferable;
import org.sarge.jove.io.DataSource;
import org.sarge.jove.io.ResourceLoaderAdapter;
import org.sarge.jove.model.Model;
import org.sarge.jove.model.ModelLoader;
import org.sarge.jove.platform.vulkan.VkBufferUsage;
import org.sarge.jove.platform.vulkan.VkMemoryProperty;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private AllocationService allocator;
	@Autowired private Pool transfer;

	@Bean
	public static Model model(DataSource data) throws IOException {
//		final var adapter = new ResourceLoaderAdapter<>(data, new ObjectModelLoader());
//		final Model model = adapter.load("chalet.obj").iterator().next();
//		final ModelLoader loader2 = new ModelLoader();
//		loader2.save(model, new DataOutputStream(new FileOutputStream("../Data/chalet.model")));

		final var loader = new ResourceLoaderAdapter<>(data, new ModelLoader());
		return loader.load("chalet.model");
	}

	@Bean
	public VulkanBuffer vbo(Model model) {
		return buffer(model.vertices(), VkBufferUsage.VERTEX_BUFFER);
	}

	@Bean
	public VulkanBuffer index(Model model) {
		return buffer(model.index(), VkBufferUsage.INDEX_BUFFER);
	}

//	@Bean
//	public VulkanBuffer instances() {
//		final Bufferable data = new Bufferable() {
//			@Override
//			public int length() {
//				return 3 * Point.LAYOUT.length();
//			}
//
//			@Override
//			public void buffer(ByteBuffer bb) {
//				new Point(-1, 0, 0).buffer(bb);
//				new Point(0, 0, -1).buffer(bb);
//				new Point(1, 0, -2).buffer(bb);
//			}
//		};
//
//		return buffer(data, VkBufferUsage.VERTEX_BUFFER);
//	}

	@Bean
	public VulkanBuffer skyboxVertexBuffer(Model skybox) {
		return buffer(skybox.vertices(), VkBufferUsage.VERTEX_BUFFER);
	}

	protected VulkanBuffer buffer(Bufferable data, VkBufferUsage usage) {
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
		staging.copy(buffer).submitAndWait(transfer);

		// Release staging
		staging.destroy();

		return buffer;
	}
}

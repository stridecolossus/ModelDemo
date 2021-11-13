package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.io.DataSource;
import org.sarge.jove.io.ImageData;
import org.sarge.jove.io.NativeImageLoader;
import org.sarge.jove.io.ResourceLoaderAdapter;
import org.sarge.jove.platform.vulkan.VkAccess;
import org.sarge.jove.platform.vulkan.VkFormat;
import org.sarge.jove.platform.vulkan.VkImageAspect;
import org.sarge.jove.platform.vulkan.VkImageLayout;
import org.sarge.jove.platform.vulkan.VkImageType;
import org.sarge.jove.platform.vulkan.VkImageUsage;
import org.sarge.jove.platform.vulkan.VkMemoryProperty;
import org.sarge.jove.platform.vulkan.VkPipelineStage;
import org.sarge.jove.platform.vulkan.common.Command.Pool;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.image.Image;
import org.sarge.jove.platform.vulkan.image.ImageCopyCommand;
import org.sarge.jove.platform.vulkan.image.ImageCopyCommand.CopyRegion;
import org.sarge.jove.platform.vulkan.image.ImageDescriptor;
import org.sarge.jove.platform.vulkan.image.ImageExtents;
import org.sarge.jove.platform.vulkan.image.View;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.pipeline.Barrier;
import org.sarge.jove.platform.vulkan.render.Sampler;
import org.sarge.jove.platform.vulkan.util.FormatBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextureConfiguration {
	@Autowired private LogicalDevice dev;

	@Bean
	public Sampler sampler() {
		return new Sampler.Builder(dev).build();
	}

	@Bean
	public View texture(AllocationService allocator, DataSource src, Pool graphics) throws IOException {
		// Load texture image
//		final var loader = new DataSourceResourceLoader<>(src, new ImageData.Loader());
//		final ImageData image = loader.load("chalet.jpg");

		final var loader = new ResourceLoaderAdapter<>(src, new NativeImageLoader());
		final ImageData image = loader.load("chalet.jpg");

		// Determine image format
		final VkFormat format = FormatBuilder.format(image.layout());
//		final VkFormat format = VkFormat.R8G8B8A8_UNORM;
//System.err.println("IMAGE="+format);

		// Create descriptor
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.type(VkImageType.IMAGE_TYPE_2D)
				.aspect(VkImageAspect.COLOR)
				.extents(new ImageExtents(image.size()))
				.format(format)
				.build();

		// Init image memory properties
		final var props = new MemoryProperties.Builder<VkImageUsage>()
				.usage(VkImageUsage.TRANSFER_DST)
				.usage(VkImageUsage.SAMPLED)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create texture
		final Image texture = new Image.Builder()
				.descriptor(descriptor)
				.properties(props)
				.build(dev, allocator);

		// Prepare texture
		new Barrier.Builder()
				.source(VkPipelineStage.TOP_OF_PIPE)
				.destination(VkPipelineStage.TRANSFER)
				.barrier(texture)
					.newLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.destination(VkAccess.TRANSFER_WRITE)
					.build()
				.build()
				.submitAndWait(graphics);

		// Create staging buffer
		//final Bufferable data = Bufferable.of(image.bytes());
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, image.data());

		// Copy staging to texture
		new ImageCopyCommand.Builder()
				.image(texture)
				.buffer(staging)
				.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
				.region(CopyRegion.of(descriptor))
				.build()
				.submitAndWait(graphics);

		// Release staging
		staging.destroy();

		// Transition to sampled image
		new Barrier.Builder()
				.source(VkPipelineStage.TRANSFER)
				.destination(VkPipelineStage.FRAGMENT_SHADER)
				.barrier(texture)
					.oldLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.newLayout(VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
					.source(VkAccess.TRANSFER_WRITE)
					.destination(VkAccess.SHADER_READ)
					.build()
				.build()
				.submitAndWait(graphics);

		// Create texture view
		return new View.Builder(texture)
				.mapping(image)
				.build();
	}
}

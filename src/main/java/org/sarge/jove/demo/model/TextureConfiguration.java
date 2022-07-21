package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.io.*;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.image.*;
import org.sarge.jove.platform.vulkan.memory.*;
import org.sarge.jove.platform.vulkan.pipeline.Barrier;
import org.sarge.jove.platform.vulkan.util.FormatBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
public class TextureConfiguration {
	@Autowired private LogicalDevice dev;

	@Bean
	public Sampler sampler(ApplicationConfiguration cfg) {
		return new Sampler.Builder()
				.anisotropy(cfg.getAnisotropy())
				.build(dev);
	}

	@Bean
	public View texture(AllocationService allocator, DataSource data, Pool transfer) throws IOException {
		// Load texture image
//		final var loader = new DataSourceResourceLoader<>(src, new ImageData.Loader());
//		final ImageData image = loader.load("chalet.jpg");

//		final var loader = new ResourceLoaderAdapter<>(data, new NativeImageLoader());
//		final ImageData image = loader.load("chalet.jpg");

		final var loader = new ResourceLoaderAdapter<>(data, new VulkanImageLoader());
		final ImageData image = loader.load("chalet.ktx2");

		// Determine image format
		final VkFormat format = FormatBuilder.format(image);
//		final VkFormat format = VkFormat.R8G8B8A8_UNORM;
//		System.out.println(format);

		// Create descriptor
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.type(VkImageType.TWO_D)
				.aspect(VkImageAspect.COLOR)
				.extents(image.extents())
				.format(format)
				.mipLevels(image.levels().size())
				.build();

		// Init image memory properties
		final var props = new MemoryProperties.Builder<VkImageUsageFlag>()
				.usage(VkImageUsageFlag.TRANSFER_DST)
				.usage(VkImageUsageFlag.SAMPLED)
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
				.submitAndWait(transfer);

		// Create staging buffer
		final VulkanBuffer staging = VulkanBuffer.staging(dev, allocator, image.data());

		// Copy staging to image
		new ImageCopyCommand.Builder()
				.image(texture)
				.buffer(staging)
				.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
				.region(image)
				.build()
				.submitAndWait(transfer);

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
				.submitAndWait(transfer);



		// Create texture view
		return new View.Builder(texture)
				.mapping(ComponentMapping.of(image.components()))
				.build();
	}
}

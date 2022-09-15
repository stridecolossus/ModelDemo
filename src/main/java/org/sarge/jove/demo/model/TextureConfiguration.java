package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.io.*;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.core.Command.Pool;
import org.sarge.jove.platform.vulkan.image.*;
import org.sarge.jove.platform.vulkan.image.Image.Descriptor;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.pipeline.Barrier;
import org.sarge.jove.platform.vulkan.util.FormatBuilder;
import org.springframework.beans.factory.annotation.*;
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
	public View texture(DataSource data, @Qualifier("graphics") Pool transfer) throws IOException {
		final var loader = new ResourceLoaderAdapter<>(data, new NativeImageLoader());
		final ImageData image = loader.load("chalet.jpg");

//		final var loader = new ResourceLoaderAdapter<>(data, new VulkanImageLoader());
//		final ImageData image = loader.load("chalet.ktx2");
// TODO - format hint => VkFormat.R8G8B8A8_SRGB !!!

		// Determine image format
		final VkFormat format = FormatBuilder.format(image);
//		final VkFormat format = VkFormat.R8G8B8A8_UNORM;
//		System.out.println(format);

		// Create descriptor
		final Descriptor descriptor = new Descriptor.Builder()
				.type(VkImageType.TWO_D)
				.aspect(VkImageAspect.COLOR)
				.extents(image.size())
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
		final Image texture = new DefaultImage.Builder()
				.descriptor(descriptor)
				.properties(props)
				.build(dev);

		// Prepare texture
		new Barrier.Builder()
				.source(VkPipelineStage.TOP_OF_PIPE)
				.destination(VkPipelineStage.TRANSFER)
				.image(texture)
					.newLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.destination(VkAccess.TRANSFER_WRITE)
					.build()
				.build()
				.submit(transfer);

		// Create staging buffer
		final VulkanBuffer staging = VulkanBuffer.staging(dev, image.data());

		// Copy staging to image
		new ImageTransferCommand.Builder()
				.image(texture)
				.buffer(staging)
				.layout(VkImageLayout.TRANSFER_DST_OPTIMAL)
				.region(image)
				.build()
				.submit(transfer);

		// Release staging
		staging.destroy();

		// Transition to sampled image
		new Barrier.Builder()
				.source(VkPipelineStage.TRANSFER)
				.destination(VkPipelineStage.FRAGMENT_SHADER)
				.image(texture)
					.oldLayout(VkImageLayout.TRANSFER_DST_OPTIMAL)
					.newLayout(VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
					.source(VkAccess.TRANSFER_WRITE)
					.destination(VkAccess.SHADER_READ)
					.build()
				.build()
				.submit(transfer);

		// Create texture view
		return new View.Builder(texture)
				.mapping(ComponentMapping.of(image.components()))
				.build();
	}
}

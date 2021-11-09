package org.sarge.jove.demo.model;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.sarge.jove.common.Dimensions;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.common.ClearValue;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.Surface;
import org.sarge.jove.platform.vulkan.image.Image;
import org.sarge.jove.platform.vulkan.image.ImageDescriptor;
import org.sarge.jove.platform.vulkan.image.ImageExtents;
import org.sarge.jove.platform.vulkan.image.View;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.render.Attachment;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Subpass;
import org.sarge.jove.platform.vulkan.render.Subpass.Reference;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PresentationConfiguration {
	@Autowired private LogicalDevice dev;

	@Bean
	public Swapchain swapchain(Surface.Properties props, ApplicationConfiguration cfg) {
		// Select presentation mode
		final VkPresentModeKHR mode = props.modes().contains(VkPresentModeKHR.MAILBOX_KHR) ? VkPresentModeKHR.MAILBOX_KHR : Swapchain.DEFAULT_PRESENTATION_MODE;

		// Select SRGB surface format
		final List<VkSurfaceFormatKHR> formats = props.formats();
		final VkSurfaceFormatKHR format = formats
				.stream()
				.filter(f -> f.format == VkFormat.B8G8R8_UNORM)
				.filter(f -> f.colorSpace == VkColorSpaceKHR.SRGB_NONLINEAR_KHR)
				.findAny()
				.orElse(formats.get(0));

		// Create swapchain
		return new Swapchain.Builder(dev, props)
				.count(cfg.getFrameCount())
				.clear(cfg.getBackground())
				.format(format.format)
				.space(format.colorSpace)
				.presentation(mode)
				.build();
	}

	@Bean
	public View depth(Swapchain swapchain, AllocationService allocator) {
		// Define depth image
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.aspect(VkImageAspect.DEPTH)
				.extents(new ImageExtents(swapchain.extents()))
				.format(Image.depth(dev.parent()))
				.build();

		// Init properties
		final MemoryProperties<VkImageUsage> props = new MemoryProperties.Builder<VkImageUsage>()
				.usage(VkImageUsage.DEPTH_STENCIL_ATTACHMENT)
				.required(VkMemoryProperty.DEVICE_LOCAL)
				.build();

		// Create depth image
		final Image image = new Image.Builder()
				.descriptor(descriptor)
				.tiling(VkImageTiling.OPTIMAL)
				.properties(props)
				.build(dev, allocator);

		// Create depth view
		return new View.Builder(image)
				.clear(ClearValue.DEPTH)
				.build();
	}

	@Bean
	public RenderPass pass(Swapchain swapchain, @Qualifier("depth") View view) {
		// Create colour attachment
		final Attachment colour = new Attachment.Builder()
				.format(swapchain.format())
				.load(VkAttachmentLoadOp.CLEAR)
				.store(VkAttachmentStoreOp.STORE)
				.finalLayout(VkImageLayout.PRESENT_SRC_KHR)
				.build();

		// Create depth-stencil attachment
		final Attachment depth = new Attachment.Builder()
				.format(view.image().descriptor().format())
				.load(VkAttachmentLoadOp.CLEAR)
				.finalLayout(VkImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
				.build();

		// Create sub-pass
		final Subpass subpass = new Subpass.Builder()
				.colour(new Reference(colour, VkImageLayout.COLOR_ATTACHMENT_OPTIMAL))
				.depth(new Reference(depth, VkImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL))
				.dependency()
					.subpass(Subpass.EXTERNAL)
					.source()
						.stage(VkPipelineStage.COLOR_ATTACHMENT_OUTPUT)
						.build()
					.destination()
						.stage(VkPipelineStage.COLOR_ATTACHMENT_OUTPUT)
						.access(VkAccess.COLOR_ATTACHMENT_WRITE)
						.build()
					.build()
				.build();

		// Create render pass
		return RenderPass.create(dev, List.of(subpass));
	}

	@Bean
	public static List<FrameBuffer> buffers(Swapchain swapchain, RenderPass pass, View depth) {
		final Dimensions extents = swapchain.extents();
		return swapchain
				.views()
				.stream()
				.map(view -> FrameBuffer.create(pass, extents, List.of(view, depth)))
				.collect(toList());
	}
}

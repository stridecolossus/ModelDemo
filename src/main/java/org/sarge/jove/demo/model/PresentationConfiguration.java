package org.sarge.jove.demo.model;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Set;

import org.sarge.jove.common.Dimensions;
import org.sarge.jove.io.ImageData.Extents;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.common.ClearValue.DepthClearValue;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.PhysicalDevice;
import org.sarge.jove.platform.vulkan.core.Surface;
import org.sarge.jove.platform.vulkan.image.Image;
import org.sarge.jove.platform.vulkan.image.ImageDescriptor;
import org.sarge.jove.platform.vulkan.image.View;
import org.sarge.jove.platform.vulkan.memory.AllocationService;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.render.Attachment;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Subpass;
import org.sarge.jove.platform.vulkan.render.Subpass.Reference;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.platform.vulkan.util.FormatSelector;
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
		final VkPresentModeKHR mode = Swapchain.mode(props, VkPresentModeKHR.MAILBOX_KHR);

		// Select SRGB surface format
		final VkSurfaceFormatKHR format = props.format(VkFormat.B8G8R8_UNORM, VkColorSpaceKHR.SRGB_NONLINEAR_KHR);

		// Create swapchain
		return new Swapchain.Builder(dev, props)
				.count(cfg.getFrameCount())
				.clear(cfg.getBackground())
				.format(format)
				.presentation(mode)
				.build();
	}

	@Bean
	public View depth(Swapchain swapchain, AllocationService allocator) {
		// Select depth format
		final var filter = FormatSelector.feature(Set.of(VkFormatFeature.DEPTH_STENCIL_ATTACHMENT), true);
		final PhysicalDevice parent = dev.parent();
		final FormatSelector selector = new FormatSelector(parent::properties, filter);
		final VkFormat format = selector.select(List.of(VkFormat.D32_SFLOAT, VkFormat.D32_SFLOAT_S8_UINT, VkFormat.D24_UNORM_S8_UINT)).orElseThrow();

		// Define depth image
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.aspect(VkImageAspect.DEPTH)
				.extents(new Extents(swapchain.extents()))
				.format(format)
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
				.build()
				.clear(DepthClearValue.DEFAULT);
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

	// TODO - destroy
	@Bean
	public static List<FrameBuffer> buffers(Swapchain swapchain, RenderPass pass, View depth) {
		final Dimensions extents = swapchain.extents();
		return swapchain
				.attachments()
				.stream()
				.map(view -> FrameBuffer.create(pass, extents, List.of(view, depth)))
				.collect(toList());
	}
}

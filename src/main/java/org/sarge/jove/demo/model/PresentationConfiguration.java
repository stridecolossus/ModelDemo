package org.sarge.jove.demo.model;

import java.util.List;

import org.sarge.jove.io.ImageData.Extents;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.image.*;
import org.sarge.jove.platform.vulkan.image.ClearValue.DepthClearValue;
import org.sarge.jove.platform.vulkan.memory.*;
import org.sarge.jove.platform.vulkan.render.*;
import org.sarge.jove.platform.vulkan.render.Subpass.Reference;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

@Configuration
class PresentationConfiguration {
	@Autowired private LogicalDevice dev;

	@Bean
	public Swapchain swapchain(Surface surface, ApplicationConfiguration cfg) {
		// Select presentation mode
		final VkPresentModeKHR mode = surface.mode(VkPresentModeKHR.MAILBOX_KHR);

		// Select SRGB surface format
		final VkSurfaceFormatKHR format = surface.format(VkFormat.B8G8R8_UNORM, VkColorSpaceKHR.SRGB_NONLINEAR_KHR, null);

		// Create swapchain
		return new Swapchain.Builder(dev, surface)
				.count(cfg.getFrameCount())
				.clear(cfg.getBackground())
				.format(format)
				.presentation(mode)
				.build();
	}

	// TODO - selector still needs work
	@Bean
	public View depth(Swapchain swapchain, AllocationService allocator) {
		// Select depth format
		final FormatSelector selector = new FormatSelector(dev.parent(), true, VkFormatFeature.DEPTH_STENCIL_ATTACHMENT);
		final VkFormat format = selector.select(VkFormat.D32_SFLOAT, VkFormat.D32_SFLOAT_S8_UINT, VkFormat.D24_UNORM_S8_UINT).orElseThrow();

		// Define depth image
		final ImageDescriptor descriptor = new ImageDescriptor.Builder()
				.aspect(VkImageAspect.DEPTH)
				.extents(new Extents(swapchain.extents()))
				.format(format)
				.build();

		// Init properties
		final MemoryProperties<VkImageUsageFlag> props = new MemoryProperties.Builder<VkImageUsageFlag>()
				.usage(VkImageUsageFlag.DEPTH_STENCIL_ATTACHMENT)
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
				.format(view)
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
		return new FrameBuffer.Builder()
				.pass(pass)
				.extents(swapchain.extents())
				.attachment(depth)
				.build(swapchain.attachments());
	}
}

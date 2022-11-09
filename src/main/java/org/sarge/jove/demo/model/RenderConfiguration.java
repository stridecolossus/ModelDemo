package org.sarge.jove.demo.model;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.*;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.image.*;
import org.sarge.jove.platform.vulkan.image.ClearValue.DepthClearValue;
import org.sarge.jove.platform.vulkan.image.Image.Descriptor;
import org.sarge.jove.platform.vulkan.memory.MemoryProperties;
import org.sarge.jove.platform.vulkan.pipeline.*;
import org.sarge.jove.platform.vulkan.render.*;
import org.sarge.jove.scene.RenderLoop;
import org.springframework.context.annotation.*;

@Configuration
public class RenderConfiguration {
	private final LogicalDevice dev;
	private final VkFormat format;

	public RenderConfiguration(LogicalDevice dev) {
	    final FormatSelector selector = new FormatSelector(dev.parent(), true, VkFormatFeature.DEPTH_STENCIL_ATTACHMENT);
	    this.format = selector.select(VkFormat.D32_SFLOAT, VkFormat.D32_SFLOAT_S8_UINT, VkFormat.D24_UNORM_S8_UINT).orElseThrow();
		this.dev = dev;
	}

	@Bean("pipeline.bind")
	static Command pipeline(Pipeline pipeline) {
		return pipeline.bind();
	}

	@Bean("descriptor.bind")
	static Command descriptor(DescriptorSet set, PipelineLayout layout) {
		return set.bind(layout);
	}

	@Bean("vbo.bind")
	static Command vbo(VertexBuffer vbo) {
		return vbo.bind(0);
	}

	@Bean("index.bind")
	static Command vbo(IndexBuffer index) {
		return index.bind(0);
	}

	@Bean
	static DrawCommand draw(Model.Header model) {
		return DrawCommand.of(model);
	}

	@Bean
	static RenderSequence sequence(List<Command> commands) {
		return RenderSequence.of(commands);
	}

	@Bean
	public View depth(Swapchain swapchain) {
		// Configure depth image
	    final Descriptor descriptor = new Descriptor.Builder()
	            .aspect(VkImageAspect.DEPTH)
	            .extents(swapchain.extents())
	            .format(format)
	            .build();

	    // Configure as device-local
	    final var props = new MemoryProperties.Builder<VkImageUsageFlag>()
		        .usage(VkImageUsageFlag.DEPTH_STENCIL_ATTACHMENT)
		        .required(VkMemoryProperty.DEVICE_LOCAL)
		        .build();

	    // Create image
	    final Image image = new DefaultImage.Builder()
		        .descriptor(descriptor)
		        .tiling(VkImageTiling.OPTIMAL)
		        .properties(props)
		        .build(dev);

	    // Create view
	    return View.of(image).clear(DepthClearValue.DEFAULT);
	}

	@Bean
	public RenderPass pass(Swapchain swapchain) {
		final Attachment col = new Attachment.Builder()
				.format(swapchain.format())
				.load(VkAttachmentLoadOp.CLEAR)
				.store(VkAttachmentStoreOp.STORE)
				.finalLayout(VkImageLayout.PRESENT_SRC_KHR)
				.build();

	    final Attachment depth = new Attachment.Builder()
			    .format(format)
			    .load(VkAttachmentLoadOp.CLEAR)
			    .finalLayout(VkImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
			    .build();

		return new RenderPass.Builder()
				.subpass()
					.colour(col)
					.depth(depth, VkImageLayout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
					.build()
				.build(dev);
	}

	@Bean
	public static RenderLoop loop(ScheduledExecutorService executor, FrameProcessor proc, RenderSequence seq) {
		final Runnable task = () -> proc.render(seq);
		final RenderLoop loop = new RenderLoop(executor);
		loop.start(task);
		return loop;
	}
}

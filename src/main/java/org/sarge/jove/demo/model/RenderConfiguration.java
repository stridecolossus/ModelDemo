package org.sarge.jove.demo.model;

import java.util.List;

import org.sarge.jove.control.FrameCounter;
import org.sarge.jove.control.FrameThrottle;
import org.sarge.jove.control.FrameTracker;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkIndexType;
import org.sarge.jove.platform.vulkan.common.Command;
import org.sarge.jove.platform.vulkan.common.Command.Buffer;
import org.sarge.jove.platform.vulkan.common.Command.Pool;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.DrawCommand;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.RenderTask;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RenderConfiguration {
	@Bean
	public static List<Buffer> sequence(List<FrameBuffer> frames, Pipeline pipeline, VulkanBuffer vbo, VulkanBuffer index, List<DescriptorSet> sets, Pool graphics, Model model) {
		// Allocate command for each frame
		final int count = frames.size();
		final List<Buffer> buffers = graphics.allocate(count);

		// Create draw command
		final Command draw = DrawCommand.of(model);

		// Record render sequence
		for(int n = 0; n < count; ++n) {
			final FrameBuffer fb = frames.get(n);
			final DescriptorSet ds = sets.get(n);
			buffers
				.get(n)
				.begin()
					.add(fb.begin())
					.add(pipeline.bind())
					.add(vbo.bindVertexBuffer())
					.add(index.bindIndexBuffer(VkIndexType.UINT32))
					.add(ds.bind(pipeline.layout()))
					.add(draw)
					.add(FrameBuffer.END)
				.end();
		}

		return buffers;
	}

	@Bean
	public static Task render(Swapchain swapchain, List<Buffer> buffers, Pool presentation, ApplicationConfiguration cfg) {
		return new RenderTask(swapchain, cfg.getFrameCount(), buffers::get, presentation.queue());
	}

	@Bean
	static FrameCounter counter() {
		return new FrameCounter();
	}

	@Bean
	public static Task tracker(FrameCounter counter) {
		final FrameTracker tracker = new FrameTracker();
		tracker.add(new FrameThrottle());
		tracker.add(counter);
		return tracker;
	}
}

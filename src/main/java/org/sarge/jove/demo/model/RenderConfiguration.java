package org.sarge.jove.demo.model;

import java.util.List;

import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.common.Command;
import org.sarge.jove.platform.vulkan.common.Command.Buffer;
import org.sarge.jove.platform.vulkan.common.Command.Pool;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.DrawCommand;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.RenderLoop;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RenderConfiguration {
	@Bean
	public static List<Buffer> sequence(List<FrameBuffer> frames, Pipeline pipeline, VulkanBuffer vbo, List<DescriptorSet> sets, Pool graphics, Model model) {
		// Allocate command for each frame
		final int count = frames.size();
		final List<Buffer> buffers = graphics.allocate(count);

		// Create draw command
		final Command draw = DrawCommand.draw(model.header().count());

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
					.add(ds.bind(pipeline.layout()))
					.add(draw)
					.add(FrameBuffer.END)
				.end();
		}

		return buffers;
	}

	@Bean
	public static Runnable render(Swapchain swapchain, List<Buffer> buffers, Pool presentation, ApplicationConfiguration cfg) {
		return new RenderLoop(swapchain, cfg.getFrameCount(), buffers::get, presentation.queue());
	}
}

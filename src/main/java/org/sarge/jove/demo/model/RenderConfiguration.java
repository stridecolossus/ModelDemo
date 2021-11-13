package org.sarge.jove.demo.model;

import java.util.List;
import java.util.function.BiConsumer;

import org.sarge.jove.control.FrameCounter;
import org.sarge.jove.control.FrameThrottle;
import org.sarge.jove.control.FrameTracker;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkIndexType;
import org.sarge.jove.platform.vulkan.common.Command;
import org.sarge.jove.platform.vulkan.common.Command.Buffer;
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
	public static List<Buffer> command(Command.Pool graphics, List<FrameBuffer> buffers, BiConsumer<Command.Buffer, Integer> recorder, BiConsumer<Command.Buffer, Integer> skyboxRecorder) {
		final List<Command.Buffer> commands = graphics.allocate(2);

		for(int n = 0; n < 2; ++n) {
			final Command.Buffer cmd = commands.get(n);
			final FrameBuffer fb = buffers.get(n);
			cmd.begin().add(fb.begin());

			recorder.accept(cmd, n);
			skyboxRecorder.accept(cmd, n);

//			final DescriptorSet ds = sets.get(n);
//			recorder.accept(cmd, ds);
//			skyboxRecorder.accept(cmd, ds);

			cmd.add(FrameBuffer.END).end();
		}

		return commands;
	}

	@Bean
	public static BiConsumer<Command.Buffer, Integer> recorder(Pipeline pipeline, List<DescriptorSet> descriptors, VulkanBuffer vbo, VulkanBuffer index, DrawCommand draw) {
		return (cmd, n) -> {

			final DescriptorSet set = descriptors.get(n);

			cmd
					.add(pipeline.bind())
					.add(set.bind(pipeline.layout()))
					.add(vbo.bindVertexBuffer())
					.add(index.bindIndexBuffer(VkIndexType.UINT32))
					.add(draw);
		};
	}

	@Bean
	public static BiConsumer<Command.Buffer, Integer> skyboxRecorder(Pipeline skyboxPipeline, List<DescriptorSet> skyboxDescriptors, VulkanBuffer skyboxVertexBuffer, Model skybox) {
		final DrawCommand draw = DrawCommand.of(skybox);

		return (cmd, n) -> {
			final DescriptorSet set = skyboxDescriptors.get(n);
			cmd
					.add(skyboxPipeline.bind())
					.add(set.bind(skyboxPipeline.layout()))
					.add(skyboxVertexBuffer.bindVertexBuffer())
					.add(draw);
		};
	}

	@Bean
	public static Task render(Swapchain swapchain, List<Command.Buffer> buffers, Command.Pool presentation, ApplicationConfiguration cfg) {
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

package org.sarge.jove.demo.model;

import java.util.List;

import javax.annotation.PostConstruct;

import org.sarge.jove.control.FrameCounter;
import org.sarge.jove.control.FrameThrottle;
import org.sarge.jove.control.FrameTracker;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkIndexType;
import org.sarge.jove.platform.vulkan.core.Command;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PushUpdateCommand;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.DrawCommand;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.RenderTask;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class RenderConfiguration {
	@Component
	class Sequence {
		@Autowired private Command.Pool graphics;
		@Autowired private List<FrameBuffer> buffers;
		@Autowired private PushUpdateCommand update;

		@Autowired private Pipeline pipeline;
		@Autowired private List<DescriptorSet> descriptors;
		@Autowired private VulkanBuffer vbo;
		@Autowired private VulkanBuffer index;
		@Autowired private DrawCommand draw;

		@Autowired private Pipeline skyboxPipeline;
		@Autowired private List<DescriptorSet> skyboxDescriptors;
		@Autowired private VulkanBuffer skyboxVertexBuffer;
		@Autowired private Model skybox;

		private List<Command.Buffer> commands;

		@PostConstruct
		void init() {
			commands = graphics.allocate(2);
		}

		public Command.Buffer get(int index) {
			final Command.Buffer cmd = commands.get(index);
			if(cmd.isReady()) {
				cmd.reset();
			}
//			final Command.Buffer cmd = graphics.allocate();
			record(cmd, index);
			return cmd;
		}

//		A command pool must not be used concurrently in multiple threads.
//		The application must not allocate and/or free descriptor sets from the same pool in multiple threads simultaneously.

		// frame =
		// 	command pool
		//	descriptor set pool cache
		//	descriptor set cache
		//	buffer pool

		// allocate and free
		// => resetting buffers or pool

		// resetting individual command buffers
		// => expensive

		// resetting command pool
		// => periodic reset

		// one-time command if not being reused
		// minimise secondary buffers (expensive)
		// avoid reset individual

		private void record(Command.Buffer cmd, int idx) {
			final FrameBuffer fb = buffers.get(idx);
			final DescriptorSet ds = descriptors.get(idx);
			final DescriptorSet ds2 = skyboxDescriptors.get(idx);

			cmd
				.begin()
				.add(update)
				.add(fb.begin())
					.add(pipeline.bind())
					.add(ds.bind(pipeline.layout()))
					.add(vbo.bindVertexBuffer())
					.add(index.bindIndexBuffer(VkIndexType.UINT32))
					.add(draw)
					.add(skyboxPipeline.bind())
					.add(ds2.bind(skyboxPipeline.layout()))
					.add(skyboxVertexBuffer.bindVertexBuffer())
					.add(DrawCommand.of(skybox))
				.add(FrameBuffer.END)
				.end();
		}
	}

	@Bean
	public static Task render(Swapchain swapchain, Sequence seq, Command.Pool presentation) {
		return new RenderTask(swapchain, swapchain.count(), seq::get, presentation.queue());
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

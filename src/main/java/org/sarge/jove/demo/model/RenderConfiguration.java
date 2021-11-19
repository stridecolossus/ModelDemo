package org.sarge.jove.demo.model;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.sarge.jove.control.FrameCounter;
import org.sarge.jove.control.FrameThrottle;
import org.sarge.jove.control.FrameTracker;
import org.sarge.jove.control.RenderLoop.Task;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkCommandBufferUsage;
import org.sarge.jove.platform.vulkan.VkIndexType;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.Command;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
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
		@Autowired private LogicalDevice dev;
		@Autowired private Queue graphics;
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

		private Command.Pool[] pools = new Command.Pool[2];

		@PostConstruct
		void init() {
			for(int n = 0; n < 2; ++n) {
				pools[n] = Command.Pool.create(dev, graphics);
			}
		}

		@PreDestroy
		void destroy() {
			for(int n = 0; n < 2; ++n) {
				pools[n].destroy();
			}
		}

		public Command.Buffer get(int index) {
			final Command.Pool pool = pools[index];
			pool.reset();

			final Command.Buffer buffer = pool.allocate();
			record(buffer, index);

			return buffer;
		}

//		A command pool must not be used concurrently in multiple threads.
//		The application must not allocate and/or free descriptor sets from the same pool in multiple threads simultaneously.

		// frame =
		// 	command pool
		//	descriptor set pool cache
		//	descriptor set cache
		//	buffer pool

		private void record(Command.Buffer cmd, int idx) {
			final FrameBuffer fb = buffers.get(idx);
			final DescriptorSet ds = descriptors.get(idx);
			final DescriptorSet ds2 = skyboxDescriptors.get(idx);

			cmd
				.begin(VkCommandBufferUsage.ONE_TIME_SUBMIT)
				.add(update)
				.add(fb.begin())
					.add(pipeline.bind())
					.add(ds.bind(pipeline.layout()))
					.add(vbo.bindVertexBuffer(0))
					//.add(instances.bindVertexBuffer(1))
					.add(index.bindIndexBuffer(VkIndexType.UINT32))
					.add(draw)
					.add(skyboxPipeline.bind())
					.add(ds2.bind(skyboxPipeline.layout()))
					.add(skyboxVertexBuffer.bindVertexBuffer(0))
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

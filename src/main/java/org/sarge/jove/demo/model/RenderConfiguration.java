package org.sarge.jove.demo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.sarge.jove.control.FrameCounter;
import org.sarge.jove.control.FrameThrottle;
import org.sarge.jove.control.FrameTracker;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkIndexType;
import org.sarge.jove.platform.vulkan.VkPipelineStage;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.Command;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.VulkanBuffer;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PushUpdateCommand;
import org.sarge.jove.platform.vulkan.render.DefaultFrameRenderer;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.DrawCommand;
import org.sarge.jove.platform.vulkan.render.FrameBuffer;
import org.sarge.jove.platform.vulkan.render.FrameBuilder;
import org.sarge.jove.platform.vulkan.render.FrameBuilder.Recorder;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.platform.vulkan.render.VulkanFrame;
import org.sarge.jove.platform.vulkan.render.VulkanFrame.FrameRenderer;
import org.sarge.jove.scene.RenderLoop.Task;
import org.sarge.jove.scene.RenderTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RenderConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private Queue graphics;
	@Autowired private List<FrameBuffer> buffers;
	private final List<Command.Pool> pools = new ArrayList<>();

	@PreDestroy
	void destroy() {
		buffers.forEach(FrameBuffer::destroy);
		pools.forEach(Command.Pool::destroy);
	}

	@Bean
	static Recorder recorder(Pipeline pipeline, List<DescriptorSet> descriptors, VulkanBuffer vbo, VulkanBuffer index, Model model) {
		final DescriptorSet ds = descriptors.get(0); // TODO
		final DrawCommand draw = DrawCommand.of(model);

		return buffer -> {
			buffer
					.add(pipeline.bind())
					.add(ds.bind(pipeline.layout()))
					.add(vbo.bindVertexBuffer(0))
					.add(index.bindIndexBuffer(VkIndexType.UINT32))
					.add(draw);
		};
	}

	@Bean
	public Task render(Swapchain swapchain, List<Recorder> recorders, Queue presentation, PushUpdateCommand update) {
		final FrameRenderer[] array = new FrameRenderer[2];
		for(int n = 0; n < 2; ++n) {
			final Command.Pool pool = Command.Pool.create(dev, graphics);
			pools.add(pool); // TODO - urgh, maybe track in post processor? nasty tho

			final Supplier<Command.Buffer> factory = () -> {
				pool.reset();
				return pool.allocate();
			};

			final Recorder delegate = seq -> {
				//sequence.record(seq, 0);
				seq.add(update);
				for(Recorder r : recorders) {
					r.record(seq);
				}
			};

			final Recorder recorder = delegate.render(buffers.get(n));

			final FrameBuilder builder = new FrameBuilder(factory, recorder);
			array[n] = new DefaultFrameRenderer(builder, VkPipelineStage.COLOR_ATTACHMENT_OUTPUT);
		}

		final Supplier<VulkanFrame> frameFactory = () -> new VulkanFrame(swapchain, presentation, n -> array[n]);
		return new RenderTask(swapchain.count(), frameFactory);
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

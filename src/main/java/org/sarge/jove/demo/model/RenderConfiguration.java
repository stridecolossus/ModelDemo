package org.sarge.jove.demo.model;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.sarge.jove.control.*;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkPipelineStage;
import org.sarge.jove.platform.vulkan.common.Queue;
import org.sarge.jove.platform.vulkan.core.*;
import org.sarge.jove.platform.vulkan.pipeline.*;
import org.sarge.jove.platform.vulkan.render.*;
import org.sarge.jove.platform.vulkan.render.FrameBuilder.Recorder;
import org.sarge.jove.platform.vulkan.render.VulkanFrame.FrameRenderer;
import org.sarge.jove.scene.RenderLoop.Task;
import org.sarge.jove.scene.RenderTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

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
	static Recorder recorder(Pipeline pipeline, List<DescriptorSet> descriptors, VertexBuffer vbo, IndexBuffer index, Model model) {
		final DescriptorSet ds = descriptors.get(0); // TODO
		final DrawCommand draw = DrawCommand.of(model);

		return buffer -> {
			buffer
					.add(pipeline.bind())
					.add(ds.bind(pipeline.layout()))
					.add(vbo.bind(0))
					.add(index.bind(0))
					.add(draw);
		};
	}

	@Bean
	public Task render(Swapchain swapchain, List<Recorder> recorders, Queue presentation, PushConstantUpdateCommand update) {
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
		return new RenderTask(swapchain.attachments().size(), frameFactory);
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

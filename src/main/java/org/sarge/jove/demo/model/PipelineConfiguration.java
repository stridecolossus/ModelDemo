package org.sarge.jove.demo.model;

import java.io.IOException;
import java.util.function.Function;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkShaderStage;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.core.Shader;
import org.sarge.jove.platform.vulkan.pipeline.Pipeline;
import org.sarge.jove.platform.vulkan.pipeline.PipelineLayout;
import org.sarge.jove.platform.vulkan.render.DescriptorSet;
import org.sarge.jove.platform.vulkan.render.RenderPass;
import org.sarge.jove.platform.vulkan.render.Swapchain;
import org.sarge.jove.util.DataSource;
import org.sarge.jove.util.ResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PipelineConfiguration {
	private final LogicalDevice dev;
	private final Function<String, Shader> loader;

	public PipelineConfiguration(LogicalDevice dev, DataSource src) {
		this.dev = dev;
		this.loader = ResourceLoader.of(src, new Shader.Loader(dev));
	}

	@Bean
	public Shader vertex() throws IOException {
		return loader.apply("spv.chalet.vert");
	}

	@Bean
	public Shader fragment() throws IOException {
		return loader.apply("spv.chalet.frag");
	}

	@Bean
	PipelineLayout pipelineLayout(DescriptorSet.Layout layout) {
		return new PipelineLayout.Builder()
				.add(layout)
				.build(dev);
	}

	@Bean
	public Pipeline pipeline(RenderPass pass, Swapchain swapchain, Shader vertex, Shader fragment, PipelineLayout layout, Model.Header model) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(layout)
				.pass(pass)
				.viewport()
					.viewport(viewport)
					.scissor(viewport)
					.build()
				.shader(VkShaderStage.VERTEX)
					.shader(vertex)
					.build()
				.shader(VkShaderStage.FRAGMENT)
					.shader(fragment)
					.build()
				.input()
					.add(model.layout())
					.build()
				.assembly()
					.topology(model.primitive())
					.build()
				.depth()
					.enable(true)
					.build()
				.build(dev);
	}
}

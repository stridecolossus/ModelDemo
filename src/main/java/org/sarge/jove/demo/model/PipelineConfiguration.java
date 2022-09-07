package org.sarge.jove.demo.model;

import java.io.IOException;

import org.sarge.jove.common.Rectangle;
import org.sarge.jove.io.*;
import org.sarge.jove.model.Model;
import org.sarge.jove.platform.vulkan.VkShaderStage;
import org.sarge.jove.platform.vulkan.core.LogicalDevice;
import org.sarge.jove.platform.vulkan.pipeline.*;
import org.sarge.jove.platform.vulkan.render.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

@Configuration
class PipelineConfiguration {
	@Autowired private LogicalDevice dev;
	@Autowired private DataSource classpath;

	@Bean
	Shader vertex() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.vert.spv");
	}

	@Bean
	Shader fragment() throws IOException {
		final var loader = new ResourceLoaderAdapter<>(classpath, new Shader.Loader(dev));		// TODO
		return loader.load("chalet.frag.spv");
	}

	@Bean
	PipelineLayout pipelineLayout(DescriptorLayout layout) {
		return new PipelineLayout.Builder()
				.add(layout)
				.build(dev);
	}

	@Bean
	public Pipeline pipeline(RenderPass pass, Swapchain swapchain, Shader vertex, Shader fragment, PipelineLayout pipelineLayout, Model model) {
		final Rectangle viewport = new Rectangle(swapchain.extents());
		return new Pipeline.Builder()
				.layout(pipelineLayout)
				.pass(pass)
				.viewport(viewport)
				.shader(VkShaderStage.VERTEX, vertex)
				.shader(VkShaderStage.FRAGMENT, fragment)
				.input()
					.add(model.layout())
					.build()
				.assembly()
					.topology(model.primitive())
					.build()
				.depth()
					.enable(true)
					.build()
				.build(null, dev);
	}
}

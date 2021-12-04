package org.sarge.jove.demo.model;

import static org.sarge.lib.util.Check.notEmpty;
import static org.sarge.lib.util.Check.oneOrMore;

import java.util.List;

import org.sarge.jove.common.Colour;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class ApplicationConfiguration {
	private String title;
	private int frames = 2;
	private Colour col = Colour.BLACK;
	private String data;
	private List<String> features;
	private float anisotropy = 8;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = notEmpty(title);
	}

	public int getFrameCount() {
		return frames;
	}

	public void setFrameCount(int frames) {
		this.frames = oneOrMore(frames);
	}

	public Colour getBackground() {
		return col;
	}

	public void setBackground(float[] col) {
		this.col = Colour.of(col);
	}

	public String getDataDirectory() {
		return data;
	}

	public void setDataDirectory(String data) {
		this.data = notEmpty(data);
	}

	public List<String> getFeatures() {
		return features;
	}

	public void setFeatures(List<String> features) {
		this.features = List.copyOf(features);
	}

	public float getAnisotropy() {
		return anisotropy;
	}

	public void setAnisotropy(float anisotropy) {
		this.anisotropy = anisotropy;
	}
}

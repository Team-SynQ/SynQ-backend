package com.synq.backend.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.util.StringUtils;

public class ProjectUpdateRequest {

	private String title;
	private String description;

	@JsonIgnore
	private boolean titlePresent;

	@JsonIgnore
	private boolean descriptionPresent;

	@JsonSetter("title")
	public void setTitle(String title) {
		this.title = title == null ? null : title.trim();
		this.titlePresent = true;
	}

	@JsonSetter("description")
	public void setDescription(String description) {
		this.description = description;
		this.descriptionPresent = true;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

	@JsonIgnore
	public boolean hasTitle() {
		return titlePresent;
	}

	@JsonIgnore
	public boolean hasDescription() {
		return descriptionPresent;
	}

	@JsonIgnore
	@AssertTrue
	public boolean isAnyFieldPresent() {
		return titlePresent || descriptionPresent;
	}

	@JsonIgnore
	@AssertTrue
	public boolean isTitleValid() {
		return !titlePresent || StringUtils.hasText(title) && title.length() <= 30;
	}

	@JsonIgnore
	@AssertTrue
	public boolean isDescriptionValid() {
		return !descriptionPresent || description == null || description.length() <= 500;
	}
}

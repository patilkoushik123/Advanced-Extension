package com.research.assistant;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class GeminiResponse {
private List<Candidate> candidates;
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public static class Candidate{
	private Content content;
}
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public static  class Content{
	private List<Part> parts;
}
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public static class Part{
	private String text;
}
}
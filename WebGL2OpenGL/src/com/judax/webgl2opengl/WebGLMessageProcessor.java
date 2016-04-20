package com.judax.webgl2opengl;

public interface WebGLMessageProcessor
{
	public void startFrame();
	public String queueWebGLMessage(WebGLMessage webGLMessage);
	public void endFrame();
	public void update();
	public void renderFrame();
}

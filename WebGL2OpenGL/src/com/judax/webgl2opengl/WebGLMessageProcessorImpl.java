package com.judax.webgl2opengl;

import java.util.LinkedList;

/**
 * This class tries to batch WebGL2OpenGL messages as much as possible.
 * It makes a differentiation between calls made inside a frame and calls made outside a frame.
 * 
 * @author ijamardo
 *
 */
public class WebGLMessageProcessorImpl implements WebGLMessageProcessor
{
	private LinkedList<WebGLMessage> webGLMessagesQueue = new LinkedList<WebGLMessage>();
	public LinkedList<WebGLMessage> webGLMessagesQueueCopy = new LinkedList<WebGLMessage>();
	private LinkedList<WebGLMessage> webGLMessagesQueueInsideAFrame = new LinkedList<WebGLMessage>();
	protected LinkedList<WebGLMessage> webGLMessagesQueueInsideAFrameCopy = new LinkedList<WebGLMessage>();
	private boolean insideAFrame = false;
	
	@Override
	public synchronized void startFrame()
	{
		if (insideAFrame)
		{
			throw new IllegalStateException();
		}
		insideAFrame = true;
		
		// Add all the received messages from outside a frame to a copy so they can be processed when the update is called.
		webGLMessagesQueueCopy.addAll(webGLMessagesQueue);
		webGLMessagesQueue.clear();
	}

	@Override
	public synchronized void queueWebGLMessage(WebGLMessage webGLMessage)
	{
		if (!insideAFrame)
		{
			webGLMessagesQueue.add(webGLMessage);
		}
		else
		{
			webGLMessagesQueueInsideAFrame.add(webGLMessage);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void endFrame()
	{
		if (!insideAFrame)
		{
			throw new IllegalStateException();
		}
		insideAFrame = false;
		// Make a copy of the queued draw events so it can be used in more than one onDrawFrame call while more messages are stacked up
		webGLMessagesQueueInsideAFrameCopy = (LinkedList<WebGLMessage>)webGLMessagesQueueInsideAFrame.clone();
		// Clear the queue of messages inside this frame
		webGLMessagesQueueInsideAFrame.clear();
	}
	
	public synchronized void update()
	{
		// First run any webgl calls outside of a frame
		for (WebGLMessage webGLMessage: webGLMessagesQueueCopy)
		{
			webGLMessage.run();
		}
		// Get rid of all of them!
		webGLMessagesQueueCopy.clear();
	}

	@Override
	public synchronized void renderFrame()
	{
		if (!webGLMessagesQueueCopy.isEmpty()) throw new IllegalStateException("All the non-frame related WebGL calls should have been processed before a frame is rendered!");
		
		// Use the copy of the queue of webgl calls inside this frame
		for (WebGLMessage webGLMessage: webGLMessagesQueueInsideAFrameCopy)
		{
			webGLMessage.run();
		}
		// Do not clear the copy of the queue of webgl calls inside this frame because depending on the speed of the OpenGL thread and the JS thread, it could be used to make multiple render calls 
	}
}

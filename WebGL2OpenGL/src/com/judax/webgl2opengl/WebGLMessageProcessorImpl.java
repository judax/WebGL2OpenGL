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
	
	private WebGLMessage synchronousWebGLMessage = null;
	private String synchronousWebGLMessageResult = null;
	
	@Override
	public synchronized void startFrame()
	{
		if (insideAFrame)
		{
			String message = "Calling startFrame while inside an existing frame!";
			System.err.println("JUDAX: " + message);
			throw new IllegalStateException(message);
		}
		insideAFrame = true;
		
		// Add all the received messages from outside a frame to a copy so they can be processed when the update is called.
		webGLMessagesQueueCopy.addAll(webGLMessagesQueue);
		webGLMessagesQueue.clear();
	}

	@Override
	public synchronized String queueWebGLMessage(WebGLMessage webGLMessage)
	{
		String result = "";

		if (webGLMessage.isSynchronous())
		{
			// The message needs to be synchronous, so:
			// 1.- Store the message so it can be executed in the OpenGL thread.
			// 2.- Stack all the messages up until now to be called too!
			// 3.- Wait for the synchronous message to be processed.
			// 4.- Return the result of the call
			this.synchronousWebGLMessage = webGLMessage;
			webGLMessagesQueueCopy.addAll(webGLMessagesQueue);
			webGLMessagesQueue.clear();
			
			if (insideAFrame && !webGLMessagesQueueInsideAFrame.isEmpty())
			{
				String s = "";
				for (int i = 0; i < webGLMessagesQueueInsideAFrame.size(); i++)
				{
					s += "'" + webGLMessagesQueueInsideAFrame.get(i).getWebGLFunctionName() + "'" + (i < webGLMessagesQueueInsideAFrame.size() - 1 ? ", " : "");
				}
				System.err.println("JUDAX: Synchronous WebGLMessage '" + webGLMessage.getMessage() + "' inside a frame with queued render calls: " + s);
				webGLMessagesQueueCopy.addAll(webGLMessagesQueueInsideAFrame);
				webGLMessagesQueueInsideAFrame.clear();
			}
			
			try
			{
				this.wait();
			}
			catch(InterruptedException e)
			{
			}
			return synchronousWebGLMessageResult;
		}
		
		if (!insideAFrame)
		{
			webGLMessagesQueue.add(webGLMessage);
		}
		else
		{
			webGLMessagesQueueInsideAFrame.add(webGLMessage);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void endFrame()
	{
		if (!insideAFrame)
		{
			String message = "Calling endFrame outside of a frame!";
			System.err.println("JUDAX: " + message);
			throw new IllegalStateException(message);
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

		// If there is a synchronous webGLMessage, execute it, store the result and notify the waiting thread
		if (synchronousWebGLMessage != null)
		{
			synchronousWebGLMessageResult = synchronousWebGLMessage.fromWebGL2OpenGL();
			synchronousWebGLMessage = null;
			this.notifyAll();
		}
	}

	@Override
	public synchronized void renderFrame()
	{
		// If there is no synchronous message and there are still messages to be processed in the update phase, we are in trouble!
		if (!webGLMessagesQueueCopy.isEmpty() && synchronousWebGLMessage == null) 
		{
			String s = "";
			for (int i = 0; i < webGLMessagesQueueCopy.size(); i++)
			{
				s += "'" + webGLMessagesQueueCopy.get(i).getWebGLFunctionName() + "'" + (i < webGLMessagesQueueCopy.size() - 1 ? ", " : "");
			}
			String message = "All the non-frame related WebGL calls should have been processed before a frame is rendered! Pending calls are: " + s;
			System.err.println("JUDAX: " + message);
			throw new IllegalStateException(message);
		}
		
		// Use the copy of the queue of webgl calls inside this frame
		for (WebGLMessage webGLMessage: webGLMessagesQueueInsideAFrameCopy)
		{
			webGLMessage.run();
		}
		// Do not clear the copy of the queue of webgl calls inside this frame because depending on the speed of the OpenGL thread and the JS thread, it could be used to make multiple render calls 
	}
}

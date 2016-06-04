package com.judax.webgl2opengl;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class tries to batch WebGL2OpenGL messages as much as possible.
 * It makes a differentiation between calls made inside a frame and calls made outside a frame.
 * 
 * @author ijamardo
 *
 */
public class WebGLMessageProcessorImpl implements WebGLMessageProcessor
{
	private LinkedList<WebGLMessage> webGLMessagesQueueForUpdate = new LinkedList<WebGLMessage>();
	private LinkedList<WebGLMessage> webGLMessagesQueueInsideAFrame = new LinkedList<WebGLMessage>();
	protected LinkedList<WebGLMessage> webGLMessagesQueueInsideAFrameForRender = new LinkedList<WebGLMessage>();
	private boolean insideAFrame = false;
	private boolean webGLMessagesQueueInsideAFrameCopyRendered = false;
	
	private Lock lock = new ReentrantLock();
	private Condition synchronousWebGLMessagePocessed = lock.newCondition();
	private Condition renderFrameCalledForBothEyes = lock.newCondition();
	
	private WebGLMessage synchronousWebGLMessage = null;
	private String synchronousWebGLMessageResult = null;
	
	private int renderFrameCounter = 0;
	
//	private long startFrameTime = 0; 
	
	@Override
	public void startFrame()
	{
		lock.lock();
		try
		{
		//		startFrameTime = System.currentTimeMillis();
			
			if (insideAFrame)
			{
				String message = "Calling startFrame while inside an existing frame!";
				System.err.println("JUDAX: " + message);
		//			throw new IllegalStateException(message);
			}
			insideAFrame = true;
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public String queueWebGLMessage(WebGLMessage webGLMessage)
	{
		lock.lock();
		try
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
				
				if (insideAFrame)
				{
					System.err.println("JUDAX: A synchronous call to '" + webGLMessage.getMessage() + "' made inside a frame. Not a great idea. Many of these calls might slow down the JS process.");
					if (!webGLMessagesQueueInsideAFrame.isEmpty())
					{
						String s = "";
						for (int i = 0; i < webGLMessagesQueueInsideAFrame.size(); i++)
						{
							s += "'" + webGLMessagesQueueInsideAFrame.get(i).getWebGLFunctionName() + "'" + (i < webGLMessagesQueueInsideAFrame.size() - 1 ? ", " : "");
						}
						System.err.println("JUDAX: Synchronous WebGLMessage '" + webGLMessage.getMessage() + "' inside a frame with queued render calls: " + s);
						webGLMessagesQueueForUpdate.addAll(webGLMessagesQueueInsideAFrame);
						webGLMessagesQueueInsideAFrame.clear();
					}
				}
	
				synchronousWebGLMessageResult = null;
				while(synchronousWebGLMessageResult == null)
				{
					try
					{
						synchronousWebGLMessagePocessed.await();
					}
					catch(InterruptedException e)
					{
					}
				}
				
				return synchronousWebGLMessageResult;
			}
			
			if (!insideAFrame)
			{
				webGLMessagesQueueForUpdate.add(webGLMessage);
			}
			else
			{
				webGLMessagesQueueInsideAFrame.add(webGLMessage);
			}
			return result;
		}
		finally
		{
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void endFrame()
	{
		lock.lock();
		try
		{
			if (!insideAFrame)
			{
				String message = "Calling endFrame outside of a frame!";
				System.err.println("JUDAX: " + message);
	//			throw new IllegalStateException(message);
			}
			insideAFrame = false;
			
			if (webGLMessagesQueueInsideAFrameCopyRendered)
			{
				// If the current batch has been rendered, make a copy of the queued draw events so it can be used in more than one onDrawFrame call while more messages are stacked up
				webGLMessagesQueueInsideAFrameForRender = (LinkedList<WebGLMessage>)webGLMessagesQueueInsideAFrame.clone();
				webGLMessagesQueueInsideAFrameCopyRendered = false;
			}
			else 
			{
				// If the current batch has not been rendered yet, stack the calls
				webGLMessagesQueueInsideAFrameForRender.addAll(webGLMessagesQueueInsideAFrame);
			}
			
			// Clear the queue of messages inside this frame
			webGLMessagesQueueInsideAFrame.clear();
			
	//		long endFrameTime = System.currentTimeMillis();
	//		long elapsedFrameTime = endFrameTime - startFrameTime;
	//		System.out.println("JUDAX: " + elapsedFrameTime + " millis from startFrame to endFrame.");
			
			try
			{
				renderFrameCalledForBothEyes.await();
			}
			catch(InterruptedException e)
			{
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void update()
	{
		lock.lock();
		try
		{
			// First run any webgl calls outside of a frame
			for (WebGLMessage webGLMessage: webGLMessagesQueueForUpdate)
			{
				webGLMessage.run();
			}
			// Get rid of all of them!
			webGLMessagesQueueForUpdate.clear();
	
			// If there is a synchronous webGLMessage, execute it, store the result and notify the waiting thread
			if (synchronousWebGLMessage != null)
			{
				synchronousWebGLMessageResult = synchronousWebGLMessage.fromWebGL2OpenGL();
				synchronousWebGLMessage = null;
				synchronousWebGLMessagePocessed.signal();
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public void renderFrame()
	{
		lock.lock();
		try
		{
	//		long startTime = System.currentTimeMillis();
			
			// If there is no synchronous message and there are still messages to be processed in the update phase, we are in trouble!
			if (!webGLMessagesQueueForUpdate.isEmpty() && synchronousWebGLMessage == null) 
			{
				String s = "";
				for (int i = 0; i < webGLMessagesQueueForUpdate.size(); i++)
				{
					s += "'" + webGLMessagesQueueForUpdate.get(i).getWebGLFunctionName() + "'" + (i < webGLMessagesQueueForUpdate.size() - 1 ? ", " : "");
				}
				String message = "All the non-frame related WebGL calls should have been processed before a frame is rendered! Pending calls are: " + s;
				System.err.println("JUDAX: " + message);
				update();
	//			throw new IllegalStateException(message);
			}
			
			// Use the copy of the queue of webgl calls inside this frame
			for (WebGLMessage webGLMessage: webGLMessagesQueueInsideAFrameForRender)
			{
				webGLMessage.fromWebGL2OpenGL();
			}
			// Do not clear the copy of the queue of webgl calls inside this frame because depending on the speed of the OpenGL thread and the JS thread, it could be used to make multiple render calls
			// But mark that the current batch of messages in the copy have been rendered.
			webGLMessagesQueueInsideAFrameCopyRendered = true;
			
	//		long endTime = System.currentTimeMillis();
	//		long elapsedTime = endTime - startTime;
	//		System.out.println("JUDAX: " + elapsedTime + " millis to process " + webGLMessagesQueueInsideAFrameCopy.size() + " messages.");
			
			renderFrameCounter++;
			if (renderFrameCounter == 2)
			{
				renderFrameCounter = 0;
				renderFrameCalledForBothEyes.signal();
			}
		}
		finally
		{
			lock.unlock();
		}
	}
}

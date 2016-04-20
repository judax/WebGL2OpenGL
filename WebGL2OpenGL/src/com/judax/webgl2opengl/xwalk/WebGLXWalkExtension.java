package com.judax.webgl2opengl.xwalk;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.xwalk.core.XWalkExtension;

import com.judax.webgl2opengl.WebGLMessage;
import com.judax.webgl2opengl.WebGLMessageProcessor;

// TODOs
// * Asking for the current modelview and projection matrix from the JS side to the native side is not correct. As the JS thread and the native threads are completely separate, 
//   it is very likely that there will be a discrepancy between the values what can lead to hideous results (one or more frame discrepancies). The best approach could be to 
//   identify the projection and modelview matrices in the shaders to be able to provide them when the WebGLMessages are executed.
//   Maybe parsing the vertex shader for patterns? Maybe the developer could provide the names of the uniforms?
//	 Anyway, many matrices are calculated in the app code so the shader approach may not work.
// * Stereo rendering is also something important to take into account. Two modelview matrices are needed and two render passes.
//   A temporal solution could be to provide a different function to request both eyes modelview matrices from JS to the native side and perform two render passes in the JS side.
// * The Oculus SDK example is providing the matrices transposed. Find ways to be able to specify what is coming and what to do and also how to optimize this transposing of the matrices.
// * Move all the JSON logic to the C++ side to see if there is a performance gain.
// * How about caching webglmessages? A combination between JS and native unique id-s for some messages that would be already processed and ready to be called. Would there be any peformance gain? 

public class WebGLXWalkExtension
{
	private static final String EXTENSION_NAME = "WebGLXWalkExtension";
  private static final String EXTENSION_JS_CODE = "" +
  		"exports.makeCallSync = function(callString) {" +
  		"  return extension.internal.sendSyncMessage(callString);" +
  		"};" +
  		"exports.makeCallAsync = function(callString) {" +
  		"  extension.postMessage(callString);" +
  		"};" +
  		"";
  
  @SuppressWarnings("unused")
	private XWalkExtensionImpl xwalkExtension = new XWalkExtensionImpl();
  private WebGLMessageProcessor webGLMessageProcessor = null;
  
  public WebGLXWalkExtension(WebGLMessageProcessor webGLMessageProcessor)
  {
  	if (webGLMessageProcessor == null) throw new NullPointerException("The given WebGLMessageProcessor cannot be null.");
  	this.webGLMessageProcessor = webGLMessageProcessor;
  }
  
	@SuppressWarnings("unused")
	private static String fromMatrixToString(float[] matrix)
	{
		String matrixString = "[";
		int lengthMinus1 = matrix.length - 1;
		for (int i = 0; i < lengthMinus1; i++) 
		{
			matrixString += matrix[i] + ",";
		}
		matrixString += matrix[15] + "]";
		return matrixString;
	}
			
	// A private class so nothing that is not needed is exposed to the users of the OculusMobileSDKHeadTrackingXWalkExtension instances
	private class XWalkExtensionImpl extends XWalkExtension
	{
		public XWalkExtensionImpl()
		{
			super(EXTENSION_NAME, EXTENSION_JS_CODE);
		}
		
		private String processMessage(final String message)
		{
			String result = "";
			// TODO: Maybe it would be a good idea to handle the response message.
			// One possibility could be to have listeners for WebGLMessageProcessor instances that would simply
			// make the call to the instanceId using postMessage (this class could be the listener for all message processors).
			if (message.equals("startFrame"))
			{
				webGLMessageProcessor.startFrame();
			}
			else if (message.equals("endFrame"))
			{
				webGLMessageProcessor.endFrame();
			}
			else
			{
				try
				{
					WebGLMessage webGLMessage = new WebGLMessage(message);
					result = webGLMessageProcessor.queueWebGLMessage(webGLMessage);
				}
				catch(Exception e)
				{
					// TODO: How can we notify the JS side that something went wrong?
					// Remember: This is how a XWalk extension can notify information back to the JS side asynchronously: postMessage(instanceID, message);
					System.err.println("JUDAX: " + e.toString());
					e.printStackTrace();
				}
			}
			return result;
		}
		
		@Override
		public String onSyncMessage(int instanceID, String message)
		{
			return processMessage(message);
		}
		
		@Override
		public void onMessage(int instanceID, String message)
		{
			processMessage(message);
		}
	};	
}

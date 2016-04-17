package com.judax.webgl2opengl.xwalk;

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
					webGLMessageProcessor.queueWebGLMessage(webGLMessage);
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
	
/*

			else if (name.equals("getParameter"))
			{
				int target = args.getInt(0);
				
				switch( target )
				{
					// Bool
					case GLES20.GL_BLEND : case GLES20.GL_CULL_FACE : case GLES20.GL_DEPTH_TEST : case GLES20.GL_DEPTH_WRITEMASK : case GLES20.GL_DITHER : case GLES20.GL_POLYGON_OFFSET_FILL :
					case GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE : case GLES20.GL_SAMPLE_COVERAGE : case GLES20.GL_SAMPLE_COVERAGE_INVERT : case GLES20.GL_SCISSOR_TEST :
					case GLES20.GL_SHADER_COMPILER : case GLES20.GL_STENCIL_TEST :
					{
						boolean[] value = new boolean[1];
						GLES20.glGetBooleanv(target, value, 0);
					}
				
					// Int
					case GLES20.GL_ACTIVE_TEXTURE: case GLES20.GL_ALPHA_BITS: case GLES20.GL_ARRAY_BUFFER_BINDING: case GLES20.GL_BLEND_DST_ALPHA: case GLES20.GL_BLEND_DST_RGB:
					case GLES20.GL_BLEND_EQUATION_ALPHA: case GLES20.GL_BLEND_EQUATION_RGB: case GLES20.GL_BLEND_SRC_ALPHA: case GLES20.GL_BLEND_SRC_RGB: case GLES20.GL_BLUE_BITS:
					case GLES20.GL_CULL_FACE_MODE: case GLES20.GL_CURRENT_PROGRAM: case GLES20.GL_DEPTH_BITS: case GLES20.GL_DEPTH_FUNC: case GLES20.GL_ELEMENT_ARRAY_BUFFER_BINDING:
					case GLES20.GL_FRAMEBUFFER_BINDING: case GLES20.GL_FRONT_FACE: case GLES20.GL_GENERATE_MIPMAP_HINT: case GLES20.GL_GREEN_BITS:
					case GLES20.GL_IMPLEMENTATION_COLOR_READ_FORMAT: case GLES20.GL_IMPLEMENTATION_COLOR_READ_TYPE: case GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS:
					case GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE: case GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS: case GLES20.GL_MAX_RENDERBUFFER_SIZE:
					case GLES20.GL_MAX_TEXTURE_IMAGE_UNITS: case GLES20.GL_MAX_TEXTURE_SIZE: case GLES20.GL_MAX_VARYING_VECTORS: case GLES20.GL_MAX_VERTEX_ATTRIBS:
					case GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS: case GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS: case GLES20.GL_MAX_VIEWPORT_DIMS:
					case GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS: case GLES20.GL_NUM_SHADER_BINARY_FORMATS: case GLES20.GL_PACK_ALIGNMENT: case GLES20.GL_RED_BITS:
					case GLES20.GL_RENDERBUFFER_BINDING: case GLES20.GL_SAMPLE_BUFFERS: case GLES20.GL_SAMPLES: case GLES20.GL_STENCIL_BACK_FAIL: case GLES20.GL_STENCIL_BACK_FUNC:
					case GLES20.GL_STENCIL_BACK_PASS_DEPTH_FAIL: case GLES20.GL_STENCIL_BACK_PASS_DEPTH_PASS: case GLES20.GL_STENCIL_BACK_REF: case GLES20.GL_STENCIL_BACK_VALUE_MASK:
					case GLES20.GL_STENCIL_BACK_WRITEMASK: case GLES20.GL_STENCIL_BITS: case GLES20.GL_STENCIL_CLEAR_VALUE: case GLES20.GL_STENCIL_FAIL: case GLES20.GL_STENCIL_FUNC:
					case GLES20.GL_STENCIL_PASS_DEPTH_FAIL: case GLES20.GL_STENCIL_PASS_DEPTH_PASS: case GLES20.GL_STENCIL_REF: case GLES20.GL_STENCIL_VALUE_MASK:
					case GLES20.GL_STENCIL_WRITEMASK: case GLES20.GL_SUBPIXEL_BITS: case GLES20.GL_TEXTURE_BINDING_2D: case GLES20.GL_TEXTURE_BINDING_CUBE_MAP:
					case GLES20.GL_UNPACK_ALIGNMENT:
		            case 0x84FE: case 0x84FF: //TEXTURE_MAX_ANISOTROPY_EXT && MAX_TEXTURE_MAX_ANISOTROPY_EXT
					{
						GLint value;
						GLES20.glGetIntegerv(target, &value);
						return JSValueMakeNumber(context, value);
					}
						
					//float
					case GLES20.GL_DEPTH_CLEAR_VALUE: case GLES20.GL_LINE_WIDTH: case GLES20.GL_POLYGON_OFFSET_FACTOR:
					case GLES20.GL_POLYGON_OFFSET_UNITS: case GLES20.GL_SAMPLE_COVERAGE_VALUE:
					{
						GLfloat value;
						GLES20.glGetFloatv(target, &value);
						return JSValueMakeNumber(context, value);
						
					}

					// Two float values
					case GLES20.GL_ALIASED_LINE_WIDTH_RANGE: case GLES20.GL_ALIASED_POINT_SIZE_RANGE: case GLES20.GL_DEPTH_RANGE:
					{
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLfloat>(context, 2);
						GLfloat *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLfloat>(context, jsvalues, length, values);
						GLES20.glGetFloatv(target, values);
						return JSObjectAsValue(jsvalues);
					}
					
					// bvec4
					case GLES20.GL_COLOR_WRITEMASK:
					{
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLboolean>(context, 4);
						GLboolean *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLboolean>(context, jsvalues, length, values);
						GLES20.glGetBooleanv(target, values);
						return JSObjectAsValue(jsvalues);
					}
						
					// ivec4
					case GLES20.GL_SCISSOR_BOX: case GLES20.GL_VIEWPORT:
					{
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLint>(context, 4);
						GLint *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLint>(context, jsvalues, length, values);
						GLES20.glGetIntegerv(target, values);
						return JSObjectAsValue(jsvalues);
					}
						
					// fvec4
					case GLES20.GL_BLEND_COLOR: case GLES20.GL_COLOR_CLEAR_VALUE:
					{
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLfloat>(context, 4);
						GLfloat *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLfloat>(context, jsvalues, length, values);
						GLES20.glGetFloatv(target, values);
						return JSObjectAsValue(jsvalues);
					}
				
					// list<int>
					case GLES20.GL_COMPRESSED_TEXTURE_FORMATS:
					{
						int count = 0 ;
						GLES20.glGetIntegerv(GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS, &count);
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLint>(context, count);
						GLint *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLint>(context, jsvalues, length, values);
						GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, values);
						return JSObjectAsValue(jsvalues);
					}

					// list<int>
					case GLES20.GL_SHADER_BINARY_FORMATS:
					{
						int count = 0 ;
						GLES20.glGetIntegerv(GLES20.GL_NUM_SHADER_BINARY_FORMATS, &count);
						JSObjectRef jsvalues = JSTypedArrays::NewTypedArray<GLint>(context, count);
						GLint *values = NULL ;
						uint32_t length = 0 ;
						JSTypedArrays::GetTypedArrayData<GLint>(context, jsvalues, length, values);
						GLES20.glGetIntegerv(GLES20.GL_SHADER_BINARY_FORMATS, values);
						return JSObjectAsValue(jsvalues);
					}
		            default: {
						IDTKLog(IDTK_LOG_WARNING, "Unhandled WebGL enum in getParameter, fallback to integer: %i" , target );
		                GLint value = 0;
		                GLES20.glGetIntegerv(target, &value);
		                return JSValueMakeNumber(context, value);
		            }
				}
			}
			else
			{
				*exception = JSUtilities::StringToValue(context, WEBGLES20.GL_ERROR_ARGCOUNT);
			}

 */
	
}

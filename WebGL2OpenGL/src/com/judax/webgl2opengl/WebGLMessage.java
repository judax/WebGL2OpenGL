package com.judax.webgl2opengl;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Base64;
import android.util.SparseIntArray;

public class WebGLMessage implements Runnable
{
	private static final boolean VERBOSE = false;
		
	private static final int BYTES_PER_FLOAT = 4;
	private static final int BYTES_PER_INT = 4;
	private static final int BYTES_PER_SHORT = 2;
	
  private static SparseIntArray jsIdsToNativeIds = new SparseIntArray();
  
  // Configuration data
  private static HashSet<String> projectionMatrixUniformNames = new HashSet<String>();
  private static HashSet<Integer> projectionMatrixUniformJSIds = new HashSet<Integer>();
  private static HashSet<String> modelViewMatrixUniformNames = new HashSet<String>();
  private static HashSet<Integer> modelViewMatrixUniformsJSIds = new HashSet<Integer>();
 
  // Some default names of matrix uniforms
  static 
  {
  	// PROJECTION
  	
  	// WebGLLessons
  	projectionMatrixUniformNames.add("uProjectionMatrix");
  	projectionMatrixUniformNames.add("uPMatrix");

  	// ThreeJS
  	projectionMatrixUniformNames.add("projectionMatrix");
  	
  	// PlayCanvas
  	projectionMatrixUniformNames.add("matrix_projection"); 
  	projectionMatrixUniformNames.add("matrix_viewProjection");
  	
  	// Sketchfab
  	projectionMatrixUniformNames.add("ProjectionMatrix");
  	
  	// Goo
  	projectionMatrixUniformNames.add("projectionMatrix"); 
  	projectionMatrixUniformNames.add("viewProjectionMatrix"); 


  	// MODELVIEW
  	
  	// WebGLLessons
  	modelViewMatrixUniformNames.add("uMVMatrix");
  	
  	// ThreeJS
  	modelViewMatrixUniformNames.add("modelViewMatrix"); 
  	
  	// PlayCanvas
  	modelViewMatrixUniformNames.add("matrix_view"); 
  	modelViewMatrixUniformNames.add("matrix_model");
  	
  	// Sketchfab
  	modelViewMatrixUniformNames.add("ModelViewMatrix"); 
  	
  	// Goo
  	modelViewMatrixUniformNames.add("viewMatrix"); 
  	modelViewMatrixUniformNames.add("worldMatrix"); 
  }
  
	private static float[] projectionMatrix = new float[16];
	private static float[] modelViewMatrix = new float[16];
	
  // These variables represent OpenGL states modifiable by a WebGL command
	private static boolean unpackPremultiplyAlpha = false;
	private static boolean unpackFlipY = false;
	
	private static float[] fromObjectToFloatArray(Object object) throws JSONException
	{
		float[] values = null;
		if (object instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)object;
			values = new float[jsonObject.length()];
		}
		else if (object instanceof JSONArray)
		{
			JSONArray jsonArray = (JSONArray)object;
			values = new float[jsonArray.length()];
		}
		else 
		{
			throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
		}
		fromObjectToFloatArray(object, values);
		return values;
	}

	private static void fromObjectToFloatArray(Object object, float[] values) throws JSONException
	{
		for (int i = 0; i < values.length; i++) 
		{
			Object o = null;
			if (object instanceof JSONObject)
			{
				JSONObject jsonObject = (JSONObject)object;
				o = jsonObject.get("" + i);
			}
			else if (object instanceof JSONArray)
			{
				JSONArray jsonArray = (JSONArray)object;
				o = jsonArray.get(i);
			}
			else 
			{
				throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
			}
			float value = 0;
			if (o instanceof Number)
			{
				value = ((Number)o).floatValue();
			}
			values[i] = value;
		}
		// =========================================
//		String s = "[";
//		for (int i = 0; i < values.length; i++) 
//		{
//			s += values[i] + (i < values.length - 1 ? ", " : "");
//		}
//		s += "]";
//		System.out.println("JUDAX: " + s);
		// =========================================
	}
	
	private static short[] fromObjectToShortArray(Object object) throws JSONException
	{
		short[] values = null;
		if (object instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)object;
			values = new short[jsonObject.length()];
		}
		else if (object instanceof JSONArray)
		{
			JSONArray jsonArray = (JSONArray)object;
			values = new short[jsonArray.length()];
		}
		else 
		{
			throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
		}
		fromObjectToShortArray(object, values);
		return values;
	}
	
	private static void fromObjectToShortArray(Object object, short[] values) throws JSONException
	{
		for (int i = 0; i < values.length; i++) 
		{
			Object o = null;
			if (object instanceof JSONObject)
			{
				JSONObject jsonObject = (JSONObject)object;
				o = jsonObject.get("" + i);
			}
			else if (object instanceof JSONArray)
			{
				JSONArray jsonArray = (JSONArray)object;
				o = jsonArray.get(i);
			}
			else 
			{
				throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
			}
			short value = 0;
			if (o instanceof Number)
			{
				value = ((Number)o).shortValue();
			}
			values[i] = value;
		}
	}
	
	private static int[] fromObjectToIntArray(Object object) throws JSONException
	{
		int[] values = null;
		if (object instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)object;
			values = new int[jsonObject.length()];
		}
		else if (object instanceof JSONArray)
		{
			JSONArray jsonArray = (JSONArray)object;
			values = new int[jsonArray.length()];
		}
		else 
		{
			throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
		}
		fromObjectToIntArray(object, values);
		return values;
	}
	
	private static void fromObjectToIntArray(Object object, int[] values) throws JSONException
	{
		for (int i = 0; i < values.length; i++) 
		{
			Object o = null;
			if (object instanceof JSONObject)
			{
				JSONObject jsonObject = (JSONObject)object;
				o = jsonObject.get("" + i);
			}
			else if (object instanceof JSONArray)
			{
				JSONArray jsonArray = (JSONArray)object;
				o = jsonArray.get(i);
			}
			else 
			{
				throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
			}
			int value = 0;
			if (o instanceof Number)
			{
				value = ((Number)o).intValue();
			}
			values[i] = value;
		}
	}
	
	private static byte[] fromObjectToByteArray(Object object) throws JSONException
	{
		byte[] values = null;
		if (object instanceof JSONObject)
		{
			JSONObject jsonObject = (JSONObject)object;
			values = new byte[jsonObject.length()];
		}
		else if (object instanceof JSONArray)
		{
			JSONArray jsonArray = (JSONArray)object;
			values = new byte[jsonArray.length()];
		}
		else 
		{
			throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
		}
		fromObjectToByteArray(object, values);
		return values;
	}

	private static void fromObjectToByteArray(Object object, byte[] values) throws JSONException
	{
		for (int i = 0; i < values.length; i++) 
		{
			Object o = null;
			if (object instanceof JSONObject)
			{
				JSONObject jsonObject = (JSONObject)object;
				o = jsonObject.get("" + i);
			}
			else if (object instanceof JSONArray)
			{
				JSONArray jsonArray = (JSONArray)object;
				o = jsonArray.get(i);
			}
			else 
			{
				throw new IllegalArgumentException("JUDAX: Could not identify the object nor as a JSONObject nor a JSONArray.");
			}
			byte value = 0;
			if (o instanceof Number)
			{
				value = ((Number)o).byteValue();
			}
			values[i] = value;
		}
		// =========================================
//		String s = "[";
//		for (int i = 0; i < values.length; i++) 
//		{
//			s += values[i] + (i < values.length - 1 ? ", " : "");
//		}
//		s += "]";
//		System.out.println("JUDAX: " + s);
		// =========================================
	}
		
	private static String fromWebGLNameToOpenGLName(String webGLFunctionName)
	{
		return "gl" + Character.toUpperCase(webGLFunctionName.charAt(0)) + webGLFunctionName.substring(1);	
	}
	
	// The original WebGL message and the JSON conversion of the message object
	private String message = null;
	private JSONObject messageJSON = null;
	private String webGLFunctionName = null;
	private JSONArray webGLFunctionArgs = null;
	private JSONArray supportedExtensions = null;

	private void getSupportedExtensions()
	{
		supportedExtensions = new JSONArray();
		String supportedExtensionsString = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		String[] supportedExtensionsStringArray = supportedExtensionsString.split("[\t ]");
		for (int i = 0; i < supportedExtensionsStringArray.length; i++)
		{
			supportedExtensions.put(supportedExtensionsStringArray[i]);
//			System.out.println("JUDAX: Extension " + supportedExtensionsStringArray[i] + " supported");
		}
	}
	
	public WebGLMessage(String message) throws Exception
	{
		this.message = message;
		this.messageJSON = new JSONObject(message);
		this.webGLFunctionName = messageJSON.getString("name");
		this.webGLFunctionArgs = messageJSON.getJSONArray("args");
	}
	
	public boolean isSynchronous()
	{
//		return false;
		return 
				webGLFunctionName.equals("getParameter") ||
				webGLFunctionName.equals("getActiveAttrib") ||
				webGLFunctionName.equals("getActiveUniform") ||
				webGLFunctionName.equals("getAttribLocation") || // This is a special case. The WebGL spec forces it to return a GLint so let's comply with the spec in case an engine is expecting that value
				webGLFunctionName.equals("getProgramParameter") ||
				webGLFunctionName.equals("getBufferParameter") ||
				webGLFunctionName.equals("getShaderPrecisionFormat") ||
				webGLFunctionName.equals("getShaderInfoLog") ||
				webGLFunctionName.equals("getShaderParameter");
	}
	
	public String fromWebGL2OpenGL()
	{
		String resultString = "";
		try 
		{
			// =========================================
			if (VERBOSE)
			{
				System.out.println("JUDAX: " + message);
			}
			// =========================================
			
			// These functions are not webgl messages directly but messages to be able to configure some aspects of the whole WebGL2OpenGL conversion
			if (webGLFunctionName.equals("configure"))
			{
				JSONObject webGL2OpenGLConfig = webGLFunctionArgs.getJSONObject(0);
				JSONArray projectionMatrixUniformNamesJSONArray = webGL2OpenGLConfig.getJSONArray("projectionMatrixUniformNames");
				for (int i = 0; i < projectionMatrixUniformNamesJSONArray.length(); i++)
				{
					projectionMatrixUniformNames.add(projectionMatrixUniformNamesJSONArray.getString(i));
				}
				JSONArray modelViewMatrixUniformNamesJSONArray = webGL2OpenGLConfig.getJSONArray("modelViewMatrixUniformNames");
				for (int i = 0; i < modelViewMatrixUniformNamesJSONArray.length(); i++)
				{
					modelViewMatrixUniformNames.add(modelViewMatrixUniformNamesJSONArray.getString(i));
				}
				return resultString;
			}
			
			// Some calls are very specific. 
			// The call to "createBuffer" does not have a direct match in native but to call "glGenBuffers" to create just one buffer.
			if (webGLFunctionName.equals("createBuffer") || webGLFunctionName.equals("createTexture") || webGLFunctionName.equals("createFramebuffer") || webGLFunctionName.equals("createRenderbuffer"))
			{
				int[] ids = new int[1];
				if (webGLFunctionName.equals("createBuffer"))
				{
					GLES20.glGenBuffers(1, ids, 0);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glGenBuffers(1, " + ids + ", 0) -> " + ids[0]);
					}
					// =========================================
				}
				else if (webGLFunctionName.equals("createTexture"))
				{
					GLES20.glGenTextures(1, ids, 0);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glGenTextures(1, " + ids + ", 0) -> " + ids[0]);
					}
					// =========================================
				}
				else if (webGLFunctionName.equals("createFramebuffer"))
				{
					GLES20.glGenFramebuffers(1, ids, 0);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glGenFramebuffers(1, " + ids + ", 0) -> " + ids[0]);
					}
					// =========================================
				}
				else if (webGLFunctionName.equals("createRenderbuffer"))
				{
					GLES20.glGenRenderbuffers(1, ids, 0);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glGenRenderbuffers(1, " + ids + ", 0) -> " + ids[0]);
					}
					// =========================================
				}
				// Make a association between the native id for the buffer and the id passed from the JS side.
				int jsId = messageJSON.getInt("webGL2OpenGLId");
				int nativeId = ids[0]; 
				jsIdsToNativeIds.put(jsId, nativeId);
			}
			// There is no 'getParameter' in OpenGL. Match to the corresponding 'glGetXXX' function.
			else if (webGLFunctionName.equals("getParameter"))
			{
				int target = webGLFunctionArgs.getInt(0);
				
				switch( target )
				{
					// Bool
					case GLES20.GL_BLEND : case GLES20.GL_CULL_FACE : case GLES20.GL_DEPTH_TEST : case GLES20.GL_DEPTH_WRITEMASK : case GLES20.GL_DITHER : case GLES20.GL_POLYGON_OFFSET_FILL :
					case GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE : case GLES20.GL_SAMPLE_COVERAGE : case GLES20.GL_SAMPLE_COVERAGE_INVERT : case GLES20.GL_SCISSOR_TEST :
					case GLES20.GL_SHADER_COMPILER : case GLES20.GL_STENCIL_TEST :
					{
						boolean[] values = new boolean[1];
						GLES20.glGetBooleanv(target, values, 0);
						resultString = "" + values[0];
						break;
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
						int[] values = new int[1];
						GLES20.glGetIntegerv(target, values, 0);
						resultString = "" + values[0];
						break;
					}
						
					//float
					case GLES20.GL_DEPTH_CLEAR_VALUE: case GLES20.GL_LINE_WIDTH: case GLES20.GL_POLYGON_OFFSET_FACTOR:
					case GLES20.GL_POLYGON_OFFSET_UNITS: case GLES20.GL_SAMPLE_COVERAGE_VALUE:
					{
						float[] values = new float[1];
						GLES20.glGetFloatv(target, values, 0);
						resultString = "" + values[0];
						break;
					}

					// Two float values
					case GLES20.GL_ALIASED_LINE_WIDTH_RANGE: case GLES20.GL_ALIASED_POINT_SIZE_RANGE: case GLES20.GL_DEPTH_RANGE:
					{
						float[] values = new float[2];
						GLES20.glGetFloatv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";
						break;
					}
					
					// bvec4
					case GLES20.GL_COLOR_WRITEMASK:
					{
						boolean[] values = new boolean[4];
						GLES20.glGetBooleanv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";
						break;
					}
						
					// ivec4
					case GLES20.GL_SCISSOR_BOX: case GLES20.GL_VIEWPORT:
					{
						int[] values = new int[4];
						GLES20.glGetIntegerv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";
						break;
					}
						
					// fvec4
					case GLES20.GL_BLEND_COLOR: case GLES20.GL_COLOR_CLEAR_VALUE:
					{
						float[] values = new float[4];
						GLES20.glGetFloatv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";
						break;
					}
				
					// list<int>
					case GLES20.GL_COMPRESSED_TEXTURE_FORMATS:
					{
						int[] count = new int[1];
						GLES20.glGetIntegerv(GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS, count, 0);
						int[] values = new int[count[0]];
						GLES20.glGetIntegerv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";
						break;
					}

					// list<int>
					case GLES20.GL_SHADER_BINARY_FORMATS:
					{
						int[] count = new int[1];
						GLES20.glGetIntegerv(GLES20.GL_NUM_SHADER_BINARY_FORMATS, count, 0);
						int[] values = new int[count[0]];
						GLES20.glGetIntegerv(target, values, 0);
						resultString = "[";
						for (int i = 0; i < values.length; i++) {
							resultString += values[i] + (i < values.length - 1 ? "," : "");
						}
						resultString += "]";						
						break;
					}
					default: 
					{
						System.err.println("JUDAX: Unhandled WebGL enum '" + target + "' in getParameter. Fallback to integer.");
						int[] values = new int[1];
						GLES20.glGetIntegerv(target, values, 0);
						resultString = "" + values[0];
					}
				}
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: getParameter(" + target + ") -> " + resultString + " - " + message);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("getProgramParameter"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int program = jsIdsToNativeIds.get(jsId);
				int param = webGLFunctionArgs.getInt(1);
				int[] values = new int[1];
				GLES20.glGetProgramiv(program, param, values, 0);
        switch (param) 
        {
            case GLES20.GL_LINK_STATUS:
            case GLES20.GL_DELETE_STATUS:
            case GLES20.GL_VALIDATE_STATUS: 
            	resultString = "" + (values[0] != 0);
            	break;
            default: 
            	resultString = "" + values[0];
        }
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: getProgramParameter(" + program + ", " + param + ") -> " + resultString + " - " + message);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("getShaderParameter"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int shader = jsIdsToNativeIds.get(jsId);
				int param = webGLFunctionArgs.getInt(1);
				int[] values = new int[1];
				GLES20.glGetShaderiv(shader, param, values, 0);
        switch (param) 
        {
            case GLES20.GL_COMPILE_STATUS:
            case GLES20.GL_DELETE_STATUS:
            	resultString = "" + (values[0] != 0);
            	break;
            default: 
            	resultString = "" + values[0];
        }
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: getShaderParameter(" + shader + ", " + param + ") -> " + resultString + " - " + message);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("getBufferParameter"))
			{
				int target = webGLFunctionArgs.getInt(0);
				int pname = webGLFunctionArgs.getInt(1);
				int[] values = new int[1];
				GLES20.glGetBufferParameteriv(target, pname, values, 0);
				resultString = "" + values[0];
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glGetBufferParameteriv(" + target + ", " + pname + ") -> " + resultString + " - " + message);
				}
				// =========================================
			}
			else if (
					webGLFunctionName.equals("deleteBuffer") || 
					webGLFunctionName.equals("deleteFramebuffer") || 
					webGLFunctionName.equals("deleteRenderbuffer") ||
					webGLFunctionName.equals("deleteTexture"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int target = jsIdsToNativeIds.get(jsId);
				int[] targets = { target };
				if (webGLFunctionName.equals("deleteBuffer"))
				{
					GLES20.glDeleteBuffers(1, targets, 0);
				}
				else if (webGLFunctionName.equals("deleteFramebuffer"))
				{
					GLES20.glDeleteFramebuffers(1, targets, 0);
				}
				else if (webGLFunctionName.equals("deleteRenderbuffer"))
				{
					GLES20.glDeleteRenderbuffers(1, targets, 0);
				}
				else if (webGLFunctionName.equals("deleteTexture"))
				{
					GLES20.glDeleteTextures(1, targets, 0);
				}
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: " + fromWebGLNameToOpenGLName(webGLFunctionName) + "(1, " + targets + ", 0) -> " + message);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("getShaderPrecisionFormat"))
			{
				int shaderType = webGLFunctionArgs.getInt(0);
				int precisionType = webGLFunctionArgs.getInt(1);
				int[] range = new int[2];
				int[] precision = new int[1];
				GLES20.glGetShaderPrecisionFormat(shaderType, precisionType, range, 0, precision, 0);
				JSONObject resultJSONObject = new JSONObject();
				resultJSONObject.put("rangeMin", range[0]);
				resultJSONObject.put("rangeMax", range[1]);
				resultJSONObject.put("precision", precision[0]);
				resultString = resultJSONObject.toString();
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: getShaderPrecisionFormat(" + shaderType + ", " + precisionType + ") -> " + resultString);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("getActiveAttrib") || webGLFunctionName.equals("getActiveUniform"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int program = jsIdsToNativeIds.get(jsId);
				int index = webGLFunctionArgs.getInt(1);
				byte[] name = new byte[500];
				int[] length = new int[1];
				int[] size = new int[1];
				int[] type = new int[1];
				if (webGLFunctionName.equals("getActiveAttrib"))
				{
					GLES20.glGetActiveAttrib(program, index, name.length, length, 0, size, 0, type, 0, name, 0);
				}
				else if (webGLFunctionName.equals("getActiveUniform"))
				{
					GLES20.glGetActiveUniform(program, index, name.length, length, 0, size, 0, type, 0, name, 0);
				}
				JSONObject resultJSONObject = new JSONObject();
				resultJSONObject.put("size", size[0]);
				resultJSONObject.put("type", type[0]);
				resultJSONObject.put("name", new String(name, 0, length[0]));
				resultString = resultJSONObject.toString();
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: " + fromWebGLNameToOpenGLName(webGLFunctionName) + "(" + program + ", " + index + ") -> " + resultString);
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("clearDepth"))
			{
				float depth = (float)webGLFunctionArgs.getDouble(0);
				GLES20.glClearDepthf(depth);
			}
			else if (webGLFunctionName.equals("pixelStorei"))
			{
				int pname = webGLFunctionArgs.getInt(0);
				Object value = webGLFunctionArgs.get(1);
        switch (pname)
        {
          case 0x9240: //UNPACK_FLIP_Y_WEBGL
            unpackFlipY = value instanceof Boolean ? (Boolean)value : (Integer)value > 0; 
            break;
          case 0x9241: //UNPACK_PREMULTIPLY_ALPHA_WEBGL
        		unpackPremultiplyAlpha = value instanceof Boolean ? (Boolean)value : (Integer)value > 0;
        		System.err.println("JUDAX: unpackPremultiplyAlpha has been set but it is still not supported!");
            break;
          case 0x9243:    //UNPACK_COLORSPACE_CONVERSION_WEBGL
            //TODO
            break;
          default:
            int param = webGLFunctionArgs.getInt(1);
            GLES20.glPixelStorei(pname,param);
            break;
        }
			}
			else if (webGLFunctionName.equals("getSupportedExtensions"))
			{
				if (supportedExtensions == null)
				{
					getSupportedExtensions();
				}
			}
			else if (webGLFunctionName.equals("getExtension"))
			{
				if (supportedExtensions == null)
				{
					getSupportedExtensions();
				}
			}
			else if (webGLFunctionName.equals("texImage2D"))
			{
				int target = webGLFunctionArgs.getInt(0);
				int level = webGLFunctionArgs.getInt(1);
				int internalFormat = webGLFunctionArgs.getInt(2);
				int type = 0;
				int border = 0;
				if (webGLFunctionArgs.length() == 6)
				{
					type = webGLFunctionArgs.getInt(4);
					String base64 = webGLFunctionArgs.getString(5);
					byte[] values = Base64.decode(base64, Base64.DEFAULT);
					final BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inScaled = false;				
					Bitmap bitmap = BitmapFactory.decodeByteArray(values, 0, values.length, options);
					
					if (unpackFlipY)
					{
						android.graphics.Matrix m = new android.graphics.Matrix();
				    m.preScale(1, -1);
				    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
//				    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
				  }
					
//					if (!unpackPremultiplyAlpha)
//					{
//						bitmap.setPremultiplied(false);
//					}
					
					GLUtils.texImage2D(target, level, internalFormat, bitmap, type, border);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glTexImage2D(" + target + ", " + level + ", " + internalFormat + ", " + bitmap + ", " + type + ", " + border + ")");
					}
					// =========================================
					bitmap.recycle();
				}
				else if (webGLFunctionArgs.length() == 9)
				{
					int width = webGLFunctionArgs.getInt(3);
					int height = webGLFunctionArgs.getInt(4);
					border = webGLFunctionArgs.getInt(5);
					int format = webGLFunctionArgs.getInt(6); 
					type = webGLFunctionArgs.getInt(7);
					GLES20.glTexImage2D(target, level, internalFormat, width, height, border, format, type, null);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glTexImage2D(" + target + ", " + level + ", " + internalFormat + ", " + width + ", " + height + ", " + border + ", " + format + ", " + type + ", " + null + ")");
					}
					// =========================================
				}
			}
			// The call to "bufferData" requires a very specific conversion of the values array and creation of the corresponding buffer.
			else if (webGLFunctionName.equals("bufferData"))
			{
				int target = webGLFunctionArgs.getInt(0);
				int usage = webGLFunctionArgs.getInt(2);
				Object valuesObject = webGLFunctionArgs.get(1);
				if (valuesObject instanceof Number)
				{
					int size = ((Number)valuesObject).intValue();
					byte[] values = new byte[size];
					ByteBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length)
							.order(ByteOrder.nativeOrder());
					valuesBuffer.put(values).position(0);
					GLES20.glBufferData(target, valuesBuffer.capacity(), valuesBuffer, usage);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() + ", " + valuesBuffer + ", " + usage + ")");
					}
					// =========================================
				}
				else 
				{
					int dataType = messageJSON.getInt("dataType");
					if (dataType == GLES20.GL_FLOAT) 
					{
						float[] values = fromObjectToFloatArray(valuesObject);
						FloatBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_FLOAT)
								.order(ByteOrder.nativeOrder())
								.asFloatBuffer();
						valuesBuffer.put(values).position(0);
						GLES20.glBufferData(target, valuesBuffer.capacity() * BYTES_PER_FLOAT, valuesBuffer, usage);
						// =========================================
						if (VERBOSE)
						{
							System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() * BYTES_PER_FLOAT + ", " + valuesBuffer + ", " + usage + ")");
						}
						// =========================================
					}
					else if (dataType == GLES20.GL_INT)
					{
						int[] values = fromObjectToIntArray(valuesObject);
						IntBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_INT)
								.order(ByteOrder.nativeOrder())
								.asIntBuffer();
						valuesBuffer.put(values).position(0);
						GLES20.glBufferData(target, valuesBuffer.capacity() * BYTES_PER_INT, valuesBuffer, usage);
						// =========================================
						if (VERBOSE)
						{
							System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() * BYTES_PER_INT + ", " + valuesBuffer + ", " + usage + ")");
						}
						// =========================================
					}
					else if (dataType == GLES20.GL_SHORT)
					{
						short[] values = fromObjectToShortArray(valuesObject);
						ShortBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_SHORT)
								.order(ByteOrder.nativeOrder())
								.asShortBuffer();
						valuesBuffer.put(values).position(0);
						GLES20.glBufferData(target, valuesBuffer.capacity() * BYTES_PER_SHORT, valuesBuffer, usage);
						// =========================================
						if (VERBOSE)
						{
							System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() * BYTES_PER_SHORT + ", " + valuesBuffer + ", " + usage + ")");
						}
						// =========================================
					}
					else if (dataType == GLES20.GL_BYTE)
					{
						byte[] values = fromObjectToByteArray(valuesObject);
						ByteBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length)
								.order(ByteOrder.nativeOrder());
						valuesBuffer.put(values).position(0);
						GLES20.glBufferData(target, valuesBuffer.capacity(), valuesBuffer, usage);
						// =========================================
						if (VERBOSE)
						{
							System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() + ", " + valuesBuffer + ", " + usage + ")");
						}
						// =========================================
					}
				}
			}
			// The call to "bufferSubData" requires a very specific conversion of the values array and creation of the corresponding buffer.
			else if (webGLFunctionName.equals("bufferSubData"))
			{
				int target = webGLFunctionArgs.getInt(0);
				int offset = webGLFunctionArgs.getInt(1);
				Object valuesObject = webGLFunctionArgs.get(2);
				int dataType = messageJSON.getInt("dataType");
				if (dataType == GLES20.GL_FLOAT) 
				{
					float[] values = fromObjectToFloatArray(valuesObject);
					FloatBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_FLOAT)
							.order(ByteOrder.nativeOrder())
							.asFloatBuffer();
					valuesBuffer.put(values).position(0);
					GLES20.glBufferSubData(target, offset, valuesBuffer.capacity() * BYTES_PER_FLOAT, valuesBuffer);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferSubData(" + target + ", " + offset + ", " + valuesBuffer.capacity() * BYTES_PER_FLOAT + ", " + valuesBuffer + ")");
					}
					// =========================================
				}
				else if (dataType == GLES20.GL_INT)
				{
					int[] values = fromObjectToIntArray(valuesObject);
					IntBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_INT)
							.order(ByteOrder.nativeOrder())
							.asIntBuffer();
					valuesBuffer.put(values).position(0);
					GLES20.glBufferSubData(target, offset, valuesBuffer.capacity() * BYTES_PER_INT, valuesBuffer);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferSubData(" + target + ", " + offset + ", " + valuesBuffer.capacity() * BYTES_PER_INT + ", " + valuesBuffer + ")");
					}
					// =========================================
				}
				else if (dataType == GLES20.GL_SHORT)
				{
					short[] values = fromObjectToShortArray(valuesObject);
					ShortBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_SHORT)
							.order(ByteOrder.nativeOrder())
							.asShortBuffer();
					valuesBuffer.put(values).position(0);
					GLES20.glBufferSubData(target, offset, valuesBuffer.capacity() * BYTES_PER_SHORT, valuesBuffer);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferSubData(" + target + ", " + offset + ", " + valuesBuffer.capacity() * BYTES_PER_SHORT + ", " + valuesBuffer + ")");
					}
					// =========================================
				}
				else if (dataType == GLES20.GL_BYTE)
				{
					byte[] values = fromObjectToByteArray(valuesObject);
					ByteBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length)
							.order(ByteOrder.nativeOrder());
					valuesBuffer.put(values).position(0);
					GLES20.glBufferSubData(target, offset, valuesBuffer.capacity(), valuesBuffer);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferData(" + target + ", " + offset + ", " + valuesBuffer.capacity() + ", " + valuesBuffer + ")");
					}
					// =========================================
				}
			}
			else if (webGLFunctionName.equals("uniform1i"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				int value;
				Object o = webGLFunctionArgs.get(1);
				if (o instanceof Boolean)
				{
					value = (Boolean)o ? 1 : 0;
				}
				else if (JSONObject.NULL.equals(o))
				{
					value = 0;
				}
				else 
				{
					value = webGLFunctionArgs.getInt(1);
				}
				GLES20.glUniform1i(location, value);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniform1i(" + location + ", " + value + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform1iv") || webGLFunctionName.equals("uniform2iv") || webGLFunctionName.equals("uniform3iv") ||webGLFunctionName.equals("uniform4iv"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				int[] values = fromObjectToIntArray(webGLFunctionArgs.get(1));
				int count = 1;
				int offset = 0;
				if (webGLFunctionName.equals("uniform1iv"))
				{
					GLES20.glUniform1iv(location, count, values, offset);
				}
				else if (webGLFunctionName.equals("uniform2iv"))
				{
					GLES20.glUniform2iv(location, count, values, offset);
				}				
				else if (webGLFunctionName.equals("uniform3iv"))
				{
					GLES20.glUniform3iv(location, count, values, offset);
				}
				else if (webGLFunctionName.equals("uniform4iv"))
				{
					GLES20.glUniform4iv(location, count, values, offset);
				}
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: " + fromWebGLNameToOpenGLName(webGLFunctionName) + "(" + location + ", " + values + ", " + offset + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform1f"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				float v1 = (float)webGLFunctionArgs.getDouble(1);
				GLES20.glUniform1f(location, v1);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniform1f(" + location + ", " + v1 + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform2f"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				float v1 = (float)webGLFunctionArgs.getDouble(1);
				float v2 = (float)webGLFunctionArgs.getDouble(2);
				GLES20.glUniform2f(location, v1, v2);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniform2f(" + location + ", " + v1 + ", " + v2 + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform3f"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				float v1 = (float)webGLFunctionArgs.getDouble(1);
				float v2 = (float)webGLFunctionArgs.getDouble(2);
				float v3 = (float)webGLFunctionArgs.getDouble(3);
				GLES20.glUniform3f(location, v1, v2, v3);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniform3f(" + location + ", " + v1 + ", " + v2 + ", " + v3 + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform4f"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				float v1 = (float)webGLFunctionArgs.getDouble(1);
				float v2 = (float)webGLFunctionArgs.getDouble(2);
				float v3 = (float)webGLFunctionArgs.getDouble(3);
				float v4 = (float)webGLFunctionArgs.getDouble(4);
				GLES20.glUniform4f(location, v1, v2, v3, v4);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniform4f(" + location + ", " + v1 + ", " + v2 + ", " + v3 + ", " + v4 + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniform1fv") || webGLFunctionName.equals("uniform2fv") || webGLFunctionName.equals("uniform3fv") || webGLFunctionName.equals("uniform4fv"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				float[] values = fromObjectToFloatArray(webGLFunctionArgs.get(1));
				int count = 1;
				int offset = 0;
				if (webGLFunctionName.equals("uniform1fv"))
				{
					GLES20.glUniform1fv(location, count, values, offset);
				}
				else if (webGLFunctionName.equals("uniform2fv"))
				{
					GLES20.glUniform2fv(location, count, values, offset);
				}				
				else if (webGLFunctionName.equals("uniform3fv"))
				{
					GLES20.glUniform3fv(location, count, values, offset);
				}
				else if (webGLFunctionName.equals("uniform4fv"))
				{
					GLES20.glUniform4fv(location, count, values, offset);
				}
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: " + fromWebGLNameToOpenGLName(webGLFunctionName) + "(" + location + ", " + values + ", " + offset + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("uniformMatrix3fv"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				int count = 1;
				boolean transpose = webGLFunctionArgs.getBoolean(1);
				float[] values = fromObjectToFloatArray(webGLFunctionArgs.get(2));
				int offset = 0;
				GLES20.glUniformMatrix3fv(location, count, transpose, values, offset);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniformMatrix3fv(" + location + ", " + count + ", " + transpose + ", " + values + ", " + offset + ")");
				}
				// =========================================
			}
			// The call to "uniformMatrix4fv" requires a very specific conversion of parameters.
			else if (webGLFunctionName.equals("uniformMatrix4fv"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("webGL2OpenGLId");
				int location = jsIdsToNativeIds.get(jsId);
				int count = 1;
				boolean transpose = webGLFunctionArgs.getBoolean(1);
				float[] values = null;
				// Check if the jsID matches a configured projection/modelview matrix to see if we need to use the matrices provided from the native side.
				if (projectionMatrixUniformJSIds.contains(jsId))
				{
					values = projectionMatrix;
				}
				else if (modelViewMatrixUniformsJSIds.contains(jsId))
				{
					values = fromObjectToFloatArray(webGLFunctionArgs.get(2));
					Matrix.multiplyMM(values, 0, modelViewMatrix, 0, values, 0);
				}
				else
				{
					values = fromObjectToFloatArray(webGLFunctionArgs.get(2));
				}
				int offset = 0;
				GLES20.glUniformMatrix4fv(location, count, transpose, values, offset);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glUniformMatrix4fv(" + location + ", " + count + ", " + transpose + ", " + values + ", " + offset + ")");
				}
				// =========================================
			}
			else if (webGLFunctionName.equals("lineWidth"))
			{
				float lineWidth = (float)webGLFunctionArgs.getDouble(0);
				GLES20.glLineWidth(lineWidth);
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glLineWidth(" + lineWidth + ")");
				}
				// =========================================
			}
			// Generic calls to native GL methods
			else 
			{
				Class<?>[] argsClasses = null;
				Object[] argsObjects = null;
				
				if (webGLFunctionArgs.length() > 0) 
				{
					argsClasses = new Class[webGLFunctionArgs.length()];
					argsObjects = new Object[webGLFunctionArgs.length()];
					for (int i = 0; i < webGLFunctionArgs.length(); i++)
					{
						Object arg = webGLFunctionArgs.get(i);
						argsObjects[i] = arg;
						if (arg instanceof String)
						{
							argsClasses[i] = String.class;
						}
						else if (arg instanceof Boolean)
						{
							argsClasses[i] = boolean.class;
						}
						else if (arg instanceof Float)
						{
							argsClasses[i] = float.class;
						}
						else if (arg instanceof Integer)
						{
							argsClasses[i] = int.class;
						}
						else if (arg instanceof JSONObject)
						{
							// If a JSON object has been passed as an argument 
							JSONObject argJSONObject = (JSONObject)arg;
							// Check if the JSON object has the 'webGL2OpenGLId' property and if so, consider it to be an id of a shader, program, uniform, ...
							if (argJSONObject.has("webGL2OpenGLId"))
							{
								argsClasses[i] = int.class;
								int jsId = argJSONObject.getInt("webGL2OpenGLId");
								int nativeId = jsIdsToNativeIds.get(jsId);
								argsObjects[i] = nativeId; 
							}
						}
						else if (JSONObject.NULL.equals(arg))
						{
							argsClasses[i] = int.class;
							argsObjects[i] = 0; 
						}
					}
				}

				String methodName = fromWebGLNameToOpenGLName(webGLFunctionName);
				Method method = null;
				method = GLES20.class.getMethod(methodName, argsClasses);
				Object result = method.invoke(null, argsObjects);
				
				if (!method.getReturnType().equals(void.class))
				{
					// Strings need to be escaped but I don't want to use another library for this. 
					// For that reason, a string is returned as an object with the "webGL2OpenGLResultString" property
					if (result instanceof String) 
					{
						JSONObject jo = new JSONObject();
						jo.put("webGL2OpenGLCallResultString", result.toString());
						resultString = jo.toString();
					}
					else 
					{
						resultString = result.toString();
					}
				}
				
				// =========================================
				if (VERBOSE)
				{
					String s = "JUDAX: " + methodName + "(";
					if (argsObjects != null) 
					{
						for (int i = 0; i < argsObjects.length; i++) 
						{
							s += "" + argsObjects[i].toString() + (i < argsObjects.length - 1 ? ", " : "");
						}
					}
					s += ") -> " + (!method.getReturnType().equals(void.class) ? result : "void");
					System.out.println(s);
				}
				// =========================================
				
				// These JS functions pass a JS id that should be matched to the native id.
				if (webGLFunctionName.equals("createShader") || 
						webGLFunctionName.equals("createProgram") ||
						webGLFunctionName.equals("getUniformLocation")) {
					// The jsId comes in the form of a separate property in the messageJSON structure (with an attribute called 'webGL2OpenGLId')
					int jsId = messageJSON.getInt("webGL2OpenGLId");
					int nativeId = (Integer)result; 
					jsIdsToNativeIds.put(jsId, nativeId);
					
					// Check if the uniform name is inside the configured projection/modelview matrix uniform name lists.
					// If the name matches, store the jsId for future possible use in uniformMatrix4fv calls
					if (webGLFunctionName.equals("getUniformLocation"))
					{
						String name = webGLFunctionArgs.getString(1);
						if (projectionMatrixUniformNames.contains(name))
						{
							projectionMatrixUniformJSIds.add(jsId);
						}
						else if (modelViewMatrixUniformNames.contains(name))
						{
							modelViewMatrixUniformsJSIds.add(jsId);
						}
					}
				}
			}
		}
		catch(Exception e) 
		{
			// TODO: How can we notify the JS side that something went wrong?
			// Remember: This is how a XWalk extension can notify information back to the JS side asynchronously: postMessage(instanceID, message);
			System.err.println("JUDAX: Exception while processing WebGL message '" + message + "' to OpenGL: " + e.toString());
			e.printStackTrace();
		}
		return resultString;
	}
	
	@Override
	public void run()
	{
		fromWebGL2OpenGL();
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public String getWebGLFunctionName()
	{
		return webGLFunctionName;
	}
	
	public int getNativeIdFromJSId(int jsId)
	{
		return jsIdsToNativeIds.get(jsId);
	}
	
	public static void setProjectionMatrixFromNative(float[] projectionMatrix)
	{
		Matrix.transposeM(WebGLMessage.projectionMatrix, 0, projectionMatrix, 0);
		
//		System.out.println("JUDAX: WebGL2NativeActivity.setProjectionMatrixFromNative: " + matrixToString(projectionMatrix));
	}

	public static void setModelViewMatrixFromNative(float[] modelViewMatrix)
	{
		Matrix.transposeM(WebGLMessage.modelViewMatrix, 0, modelViewMatrix, 0);
		
//		System.out.println("JUDAX: WebGL2NativeActivity.setModelViewMatrixFromNative: " + matrixToString(modelViewMatrix));
	}
}

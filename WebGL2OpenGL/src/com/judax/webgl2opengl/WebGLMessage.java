package com.judax.webgl2opengl;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
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
  	projectionMatrixUniformNames.add("uProjectionMatrix");
  	projectionMatrixUniformNames.add("uPMatrix");
  	
  	modelViewMatrixUniformNames.add("uMVMatrix");
  }
  
	private static float[] projectionMatrix = new float[16];
	private static float[] modelViewMatrix = new float[16];

  // These variables represent OpenGL states modifiable by a WebGL command
	private static boolean unpackPremultiplyAlpha = false;
	private static boolean unpackFlipY = false;
	
	private static float[] fromJSONObjectToFloatArray(JSONObject jsonObject) throws JSONException
	{
		float[] values = new float[jsonObject.length()];
		fromJSONObjectToFloatArray(jsonObject, values);
		return values;
	}

	private static void fromJSONObjectToFloatArray(JSONObject jsonObject, float[] values) throws JSONException
	{
		for (int i = 0; i < values.length; i++) 
		{
			Object o = jsonObject.get("" + i);
			float value = 0;
			if (o instanceof Number)
			{
				value = ((Number)jsonObject.get("" + i)).floatValue();
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
	
	private static short[] fromJSONObjectToShortArray(JSONObject jsonObject) throws JSONException
	{
		short[] values = new short[jsonObject.length()];
		for (int i = 0; i < values.length; i++) 
		{
			Object o = jsonObject.get("" + i);
			short value = 0;
			if (o instanceof Number)
			{
				value = ((Number)jsonObject.get("" + i)).shortValue();
			}
			values[i] = value;
		}
		return values;
	}
	
	// The original WebGL message and the JSON conversion of the message object
	private String message = null;
	private JSONObject messageJSON = null;
	private String webGLFunctionName = null;
	private JSONArray webGLFunctionArgs = null;

	public WebGLMessage(String message) throws Exception
	{
		this.message = message;
		this.messageJSON = new JSONObject(message);
		this.webGLFunctionName = messageJSON.getString("name");
		this.webGLFunctionArgs = messageJSON.getJSONArray("args");
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
			if (webGLFunctionName.equals("createBuffer") || webGLFunctionName.equals("createTexture"))
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
				// Make a association between the native id for the buffer and the id passed from the JS side.
				int jsId = messageJSON.getInt("extId");
				int nativeId = ids[0]; 
				jsIdsToNativeIds.put(jsId, nativeId);
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
			else if (webGLFunctionName.equals("texImage2D"))
			{
				String base64 = webGLFunctionArgs.getString(5);
				int target = webGLFunctionArgs.getInt(0);
				int level = webGLFunctionArgs.getInt(1);
				int internalFormat = webGLFunctionArgs.getInt(2);
				int type = webGLFunctionArgs.getInt(4);
				int border = 0;
				byte[] values = Base64.decode(base64, Base64.DEFAULT);
				final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;				
				Bitmap bitmap = BitmapFactory.decodeByteArray(values, 0, values.length, options);
				if (unpackFlipY)
				{
					android.graphics.Matrix m = new android.graphics.Matrix();
			    m.preScale(1, -1);
			    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
//			    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
			  }
				GLUtils.texImage2D(target, level, internalFormat, bitmap, type, border);
				bitmap.recycle();
				// =========================================
				if (VERBOSE)
				{
					System.out.println("JUDAX: glTexImage2D(" + target + ", " + level + ", " + internalFormat + ", " + bitmap + ", " + type + ", " + border + ")");
				}
				// =========================================
			}
			// The call to "bufferData" requires a very specific conversion of the values array and creation of the corresponding buffer.
			else if (webGLFunctionName.equals("bufferData"))
			{
				JSONObject valuesJSONObject = webGLFunctionArgs.getJSONObject(1);
				int dataType = messageJSON.getInt("dataType");
				int target = webGLFunctionArgs.getInt(0);
				int usage = webGLFunctionArgs.getInt(2);
				if (dataType == GLES20.GL_FLOAT) 
				{
					float[] values = fromJSONObjectToFloatArray(valuesJSONObject);
					FloatBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_FLOAT)
							.order(ByteOrder.nativeOrder())
							.asFloatBuffer();
					valuesBuffer.put(values).position(0);
					GLES20.glBufferData(target, valuesBuffer.capacity() * BYTES_PER_FLOAT, valuesBuffer, usage);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() * BYTES_PER_FLOAT + ", " + valuesBuffer + ", " + usage);
					}
					// =========================================
				}
				else if (dataType == GLES20.GL_SHORT)
				{
					short[] values = fromJSONObjectToShortArray(valuesJSONObject);
					ShortBuffer valuesBuffer = ByteBuffer.allocateDirect(values.length * BYTES_PER_SHORT)
							.order(ByteOrder.nativeOrder())
							.asShortBuffer();
					valuesBuffer.put(values).position(0);
					GLES20.glBufferData(target, valuesBuffer.capacity() * BYTES_PER_SHORT, valuesBuffer, usage);
					// =========================================
					if (VERBOSE)
					{
						System.out.println("JUDAX: glBufferData(" + target + ", " + valuesBuffer.capacity() * BYTES_PER_SHORT + ", " + valuesBuffer + ", " + usage);
					}
					// =========================================
				}
			}
			// The call to "uniformMatrix4fv" requires a very specific conversion of parameters.
			else if (webGLFunctionName.equals("uniformMatrix4fv"))
			{
				int jsId = webGLFunctionArgs.getJSONObject(0).getInt("extId");
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
					values = fromJSONObjectToFloatArray(webGLFunctionArgs.getJSONObject(2));
					Matrix.multiplyMM(values, 0, modelViewMatrix, 0, values, 0);
				}
				else
				{
					values = fromJSONObjectToFloatArray(webGLFunctionArgs.getJSONObject(2));
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
							// Check if the JSON object has the extId property and if so, consider it to be an id of a shader, program, uniform, ...
							if (argJSONObject.has("extId"))
							{
								argsClasses[i] = int.class;
								int jsId = argJSONObject.getInt("extId");
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

				String methodName = "gl" + Character.toUpperCase(webGLFunctionName.charAt(0)) + webGLFunctionName.substring(1);
				Method method = null;
				method = GLES20.class.getMethod(methodName, argsClasses);
				Object result = method.invoke(null, argsObjects);
				
				// =========================================
				if (VERBOSE || webGLFunctionName.equals("shaderSource"))
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
						webGLFunctionName.equals("getUniformLocation") ||
						webGLFunctionName.equals("getAttribLocation")) {
					// The jsId comes in the form of a separate property in the messageJSON structure (extId)
					int jsId = messageJSON.getInt("extId");
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
			System.err.println("JUDAX: " + e.toString());
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

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.judax.webgl2opengl.java;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.judax.webgl2opengl.WebGLMessageProcessorImpl;
import com.judax.webgl2opengl.xwalk.WebGLXWalkExtension;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class must
 * override the OpenGL ES drawing lifecycle methods:
 * <ul>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onDrawFrame}</li>
 * <li>{@link android.opengl.GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyGLSurfaceViewRenderer extends WebGLMessageProcessorImpl implements GLSurfaceView.Renderer
{

	private static final String TAG = "MyGLRenderer";
	private Triangle mTriangle;
	private Square mSquare;
	private com.judax.webgl2opengl.Triangle triangle;
	private WebGLXWalkExtension webGLXWalkExtension = null;
	
	// mMVPMatrix is an abbreviation for "Model View Projection Matrix"
	public final float[] mMVPMatrix = new float[16];
	public final float[] mProjectionMatrix = new float[16];
	public final float[] mViewMatrix = new float[16];
	public final float[] mRotationMatrix = new float[16];

	private float mAngle;

	public void setWebGLXWalkExtension(WebGLXWalkExtension webGLXWalkExtension)
	{
		this.webGLXWalkExtension = webGLXWalkExtension;		
	}
	
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config)
	{
		
		
		// Set the background frame color
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		mTriangle = new Triangle();
		mSquare = new Square();
		triangle = new com.judax.webgl2opengl.Triangle();
	}
	
	@Override
	public void onDrawFrame(GL10 unused)
	{
		float[] scratch = new float[16];

		// Draw background color
//		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// Set the camera position (View matrix)
//		Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
		Matrix.setIdentityM(mViewMatrix, 0);
		Matrix.translateM(mViewMatrix, 0, 0, 0, -7);
		
		// Calculate the projection and view transformation
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

		// Draw square
		mSquare.draw(mMVPMatrix);

		// Create a rotation for the triangle

		// Use the following code to generate constant rotation.
		// Leave this code out when using TouchEvents.
		// long time = SystemClock.uptimeMillis() % 4000L;
		// float angle = 0.090f * ((int) time);

		Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);

		// Combine the rotation matrix with the projection and camera view
		// Note that the mMVPMatrix factor *must be first* in order
		// for the matrix multiplication product to be correct.
		Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

		// Draw triangle
//		mTriangle.draw(scratch);
		
		
//		float[] pMatrix = {1.8106601f, 0.0f, 0.0f, 0.0f, 0.0f, 2.4142137f, 0.0f, 0.0f, 0.0f, 0.0f, -1.002002f, -1.0f, 0.0f, 0.0f, -0.2002002f, 0.0f};
//		triangle2.draw(pMatrix, mViewMatrix, scratch);

		triangle.draw(mProjectionMatrix, mViewMatrix, scratch);
		
		super.renderFrame();
		
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		// Adjust the viewport based on geometry changes,
		// such as screen rotation
		GLES20.glViewport(0, 0, width, height);

		float ratio = (float) width / height;

		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		Matrix.perspectiveM(mProjectionMatrix, 0, 45, (float)width / (float)height, 0.1f, 100);
//		Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 0.1f, 10.0f);
//		mProjectionMatrix = new float[]{1.00f, 0.00f, 0.00f, 0.00f, 0.00f, 1.00f, 0.00f, 0.00f, 0.00f, 0.00f, -1.00f, -2.00f, 0.00f, 0.00f, -1.00f, 0.00f};
	}

	/**
	 * Utility method for compiling a OpenGL shader.
	 * 
	 * <p>
	 * <strong>Note:</strong> When developing shaders, use the checkGlError()
	 * method to debug shader coding errors.
	 * </p>
	 * 
	 * @param type
	 *          - Vertex or fragment shader type.
	 * @param shaderCode
	 *          - String containing the shader code.
	 * @return - Returns an id for the shader.
	 */
	public static int loadShader(int type, String shaderCode)
	{

		// create a vertex shader type (GLES20.GL_VERTEX_SHADER)
		// or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
		int shader = GLES20.glCreateShader(type);

		// add the source code to the shader and compile it
		GLES20.glShaderSource(shader, shaderCode);
		GLES20.glCompileShader(shader);

		return shader;
	}

	/**
	 * Utility method for debugging OpenGL calls. Provide the name of the call
	 * just after making it:
	 * 
	 * <pre>
	 * mColorHandle = GLES20.glGetUniformLocation(mProgram, &quot;vColor&quot;);
	 * MyGLRenderer.checkGlError(&quot;glGetUniformLocation&quot;);
	 * </pre>
	 * 
	 * If the operation is not successful, the check throws an error.
	 * 
	 * @param glOperation
	 *          - Name of the OpenGL call to check.
	 */
	public static void checkGlError(String glOperation)
	{
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
		{
			Log.e(TAG, glOperation + ": glError " + error);
			throw new RuntimeException(glOperation + ": glError " + error);
		}
	}

	/**
	 * Returns the rotation angle of the triangle shape (mTriangle).
	 * 
	 * @return - A float representing the rotation angle.
	 */
	public float getAngle()
	{
		return mAngle;
	}

	/**
	 * Sets the rotation angle of the triangle shape (mTriangle).
	 */
	public void setAngle(float angle)
	{
		mAngle = angle;
	}
}
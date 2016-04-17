package com.judax.webgl2opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class Triangle 
{
	private static final String VERTEX_SHADER_GLSL_CODE = "\n    attribute vec3 aVertexPosition;\n\n    uniform mat4 uMVMatrix;\n    uniform mat4 uPMatrix;\n\n    void main(void) {\n        gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);\n    }\n";
	private static final String FRAGMENT_SHADER_GLSL_CODE = "\n    precision mediump float;\n\n    void main(void) {\n        gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);\n    }\n";
//	private static final String VERTEX_SHADER_GLSL_CODE = 
//			"attribute vec3 aVertexPosition;" +
//			"uniform mat4 uMVMatrix;" +
//			"uniform mat4 uPMatrix;" +
//			"void main(void) {" +
//			"  gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0); " +
//			"}";
//	private static final String FRAGMENT_SHADER_GLSL_CODE = 
//			"precision mediump float;" +
//			"void main(void) {" +
//			"  gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);" +
//			"}";
	
	private int fragmentShaderId;
	private int vertexShaderId;
	private int programId;
	private int aVertexPositionId;
	private int uPMatrixId;
	private int uMVMatrixId;
	private int bufferId;
	
	public Triangle()
	{
		fragmentShaderId = GLES20.glCreateShader(35632);
		GLES20.glShaderSource(fragmentShaderId, FRAGMENT_SHADER_GLSL_CODE);
		GLES20.glCompileShader(fragmentShaderId);
		vertexShaderId = GLES20.glCreateShader(35633);
		GLES20.glShaderSource(vertexShaderId, VERTEX_SHADER_GLSL_CODE);
		GLES20.glCompileShader(vertexShaderId);
		programId = GLES20.glCreateProgram();
		GLES20.glAttachShader(programId, fragmentShaderId);
		GLES20.glAttachShader(programId, vertexShaderId);
		GLES20.glLinkProgram(programId);
		GLES20.glUseProgram(programId);
		aVertexPositionId = GLES20.glGetAttribLocation(programId, "aVertexPosition");
		uPMatrixId = GLES20.glGetUniformLocation(programId, "uPMatrix");
		uMVMatrixId = GLES20.glGetUniformLocation(programId, "uMVMatrix");
		int[] bufferIds = new int[1];
		GLES20.glGenBuffers(1, bufferIds, 0);
		bufferId = bufferIds[0];
		GLES20.glBindBuffer(34962, bufferId);
		float[] triangleCoords = {0.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f};
		FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(triangleCoords.length * 4)
				.order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		vertexBuffer.put(triangleCoords).position(0);
		GLES20.glBufferData(34962, 36, vertexBuffer, 35044);
		GLES20.glBindBuffer(34962, 0);
		GLES20.glUseProgram(0);
	}
	
	public void draw(float[] pMatrix, float[] mvMatrix, float[] mvpMatrix)
	{
		GLES20.glUseProgram(programId);
//		GLES20.glEnable(2929);
		GLES20.glBindBuffer(34962, bufferId);
		GLES20.glEnableVertexAttribArray(aVertexPositionId);
		GLES20.glVertexAttribPointer(aVertexPositionId, 3, 5126, false, 0, 0);
//		float[] pMatrix = {1.8106601f, 0.0f, 0.0f, 0.0f, 0.0f, 2.4142137f, 0.0f, 0.0f, 0.0f, 0.0f, -1.002002f, -1.0f, 0.0f, 0.0f, -0.2002002f, 0.0f};
//		float[] pMatrix = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 2.4142137f, 0.0f, 0.0f, 0.0f, 0.0f, -1.002002f, -1.0f, 0.0f, 0.0f, -0.2002002f, 0.0f};
		GLES20.glUniformMatrix4fv(uPMatrixId, 1, false, pMatrix, 0);
//		float[] mvMatrix = {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -7.0f, 1.0f};
		GLES20.glUniformMatrix4fv(uMVMatrixId, 1, false, mvMatrix, 0);
		GLES20.glDrawArrays(4, 0, 3);
		GLES20.glDisableVertexAttribArray(aVertexPositionId);
		GLES20.glBindBuffer(34962, 0);
		GLES20.glUseProgram(0);
	}
}

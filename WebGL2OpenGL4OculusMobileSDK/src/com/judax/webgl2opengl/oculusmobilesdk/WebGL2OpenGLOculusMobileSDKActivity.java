package com.judax.webgl2opengl.oculusmobilesdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;

import com.judax.webgl2opengl.Triangle;
import com.judax.webgl2opengl.WebGLMessage;
import com.judax.webgl2opengl.WebGLMessageProcessorImpl;
import com.judax.webgl2opengl.xwalk.WebGLXWalkExtension;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class WebGL2OpenGLOculusMobileSDKActivity extends Activity implements SurfaceHolder.Callback
{
	private static final boolean USE_XWALK = true;
	private static final boolean DRAW_TRIANGLE = false;
	private static final boolean SHOW_XWALK_VIEW = false;
	
	private Triangle triangle = null;
	
	private float[] projectionMatrix = new float[16];
	private float[] modelViewMatrix = new float[16];
	private int jsCameraModelViewMatrixId;
	private int jsProjectionMatrixId;
	
	private String url;
	private boolean urlLoaded = false;
	private String webGL2OpenGLJS;
		
	// Load the gles3jni library right away to make sure JNI_OnLoad() gets called as the very first thing.
	static
	{
		System.loadLibrary( "WebGL2OpenGL4OculusMobileSDK" );
	}

	private FrameLayout layout;
	
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private long nativePointer = 0;
	
	private XWalkView xwalkView = null;
	private WebGLXWalkExtension webGLXWalkExtension = null;

	private static AlertDialog createAlertDialog(Context context, String title,
			String message, DialogInterface.OnClickListener onClickListener,
			int numberOfButtons, String yesButtonText, String noButtonText,
			String cancelButtonText)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();
		alertDialog.setTitle(title);
		alertDialog.setMessage(message);
		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, yesButtonText,
				onClickListener);
		if (numberOfButtons > 1)
		{
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, noButtonText,
					onClickListener);
		}
		if (numberOfButtons > 2)
		{
			alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, cancelButtonText,
					onClickListener);
		}
		return alertDialog;
	}
	
	private void updateFromNative()
	{
		if (DRAW_TRIANGLE)
		{
			if (triangle == null)
			{
				triangle = new Triangle();
			}
		}
		
		webGLMessageProcessor.update();
	}
	
	private void renderFrameFromNative()
	{
		webGLMessageProcessor.renderFrame();
		
		if (DRAW_TRIANGLE)
		{
			if (triangle != null)
			{
				triangle.draw(projectionMatrix, modelViewMatrix, null);
			}
		}
	}
	
	private static String readFromAssets(Context context, String filename)
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(context
				.getAssets().open(filename)));

		// do reading, usually loop until end of file reading
		StringBuilder sb = new StringBuilder();
		String mLine = reader.readLine();
		while (mLine != null)
		{
			sb.append(mLine + System.getProperty("line.separator")); // process line
			mLine = reader.readLine();
		}
		reader.close();
		return sb.toString();
	}	
	
	private WebGLMessageProcessorImpl webGLMessageProcessor = new WebGLMessageProcessorImpl();
	
	@Override protected void onCreate( Bundle icicle )
	{
		super.onCreate( icicle );

		layout = new FrameLayout(this);
		
		if (USE_XWALK)
		{
			// Create and setup the XWalkView and the extension.
			xwalkView = new XWalkView(this);
			xwalkView.clearCache(true);
			xwalkView.setResourceClient(new XWalkResourceClient(xwalkView) {
				@Override
				public void onLoadStarted(XWalkView view, String url) 
				{
	        super.onLoadStarted(view, url);
	        if (url.equals(WebGL2OpenGLOculusMobileSDKActivity.this.url))
	        {
	        	view.evaluateJavascript(webGL2OpenGLJS, null);
	        	System.out.println("JUDAX: WebGL2OpenGL injected!");
					}
				}				
			});
			webGLXWalkExtension = new WebGLXWalkExtension(webGLMessageProcessor);
		
			// Add the xwalkview to the layout.
			layout.addView(xwalkView);
		}
		
		// Create and setup the surfaceview.
		surfaceView = new SurfaceView( this );
		
		// Add the surfaceview to the layout (on top of the xwalkview). 
		layout.addView(surfaceView);
		surfaceView.getHolder().addCallback( this );
		
		setContentView(layout);
		
		// Force the screen to stay on, rather than letting it dim and shut off
		// while the user is watching a movie.
		getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

		// Force screen brightness to stay at maximum
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.screenBrightness = 1.0f;
		getWindow().setAttributes( params );
		
		// Load the WebGL2OpenGL.js file
		try
		{
			webGL2OpenGLJS = readFromAssets(this, "test/js/WebGL2OpenGL.js");
		}
		catch(IOException e)
		{
			System.err.println("JUDAX: IOException while reading the WebGL2OpenGL.js file.");
			e.printStackTrace();
			createAlertDialog(this, "Error loading extension file", "IOException while reading the extension file. Load the page anyway?", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					if (which == DialogInterface.BUTTON2)
					{
						WebGL2OpenGLOculusMobileSDKActivity.this.finish();
					}
				}
			}, 2, "Yes", "No", null);
		}
		
		// Check if a URL has been passed with an intent
		url = "";
		Intent intent = getIntent();
		if (intent != null)
		{
			url = intent.getDataString(); 
			surfaceView.setZOrderOnTop(true);
		}
		
		// Create the native side
		nativePointer = nativeOnCreate( this );
	}

	@Override protected void onStart()
	{
		super.onStart();
		nativeOnStart( nativePointer );
	}

	@Override protected void onResume()
	{
		super.onResume();
		nativeOnResume( nativePointer );
	}

	@Override protected void onPause()
	{
		nativeOnPause( nativePointer );
		super.onPause();
	}

	@Override protected void onStop()
	{
		nativeOnStop( nativePointer );
		super.onStop();
	}

	@Override protected void onDestroy()
	{
		if ( surfaceHolder != null )
		{
			nativeOnSurfaceDestroyed( nativePointer );
		}
		nativeOnDestroy( nativePointer );
		super.onDestroy();
		nativePointer = 0;
	}

	@Override public void surfaceCreated( SurfaceHolder holder )
	{
		if ( nativePointer != 0 )
		{
			nativeOnSurfaceCreated( nativePointer, holder.getSurface() );
			surfaceHolder = holder;
		}
		
		if (USE_XWALK && !urlLoaded)
		{
			xwalkView.load(url, null);
			urlLoaded = true;			
		}
	}

	@Override public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
	{
		if ( nativePointer != 0 )
		{
			nativeOnSurfaceChanged( nativePointer, holder.getSurface() );
			surfaceHolder = holder;
		}
	}
	
	@Override public void surfaceDestroyed( SurfaceHolder holder )
	{
		if ( nativePointer != 0 )
		{
			nativeOnSurfaceDestroyed( nativePointer );
			surfaceHolder = null;
		}
	}

	@Override 
	public boolean dispatchKeyEvent( KeyEvent event )
	{
		boolean result = true;
		if ( nativePointer != 0 )
		{
			int keyCode = event.getKeyCode();
			int action = event.getAction();
			if ( action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP )
			{
				return super.dispatchKeyEvent( event );
			}
			if ( action == KeyEvent.ACTION_UP )
			{
//				Log.v( TAG, "GLES3JNIActivity::dispatchKeyEvent( " + keyCode + ", " + action + " )" );
			}
			nativeOnKeyEvent( nativePointer, keyCode, action );
		}
		if (xwalkView != null)
		{
			result = xwalkView.dispatchKeyEvent(event);
		}
		return result;
	}

	@Override 
	public boolean dispatchTouchEvent( MotionEvent event )
	{
		boolean result = true;
		if ( nativePointer != 0 )
		{
			int action = event.getAction();
			float x = event.getRawX();
			float y = event.getRawY();
			if ( action == MotionEvent.ACTION_UP )
			{
//				Log.v( TAG, "GLES3JNIActivity::dispatchTouchEvent( " + action + ", " + x + ", " + y + " )" );
			}
			nativeOnTouchEvent( nativePointer, action, x, y );
		}
		if (xwalkView != null)
		{
			result = xwalkView.dispatchTouchEvent(event);
		}
		return result;
	}
	
	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent ev)
	{
		boolean result = true;
		if (xwalkView != null)
		{
			result = xwalkView.dispatchGenericMotionEvent(ev);
		}
		return result; 
	}
	
	private static String matrixToString(float[] matrix)
	{
		String s = "[";
		for (int i = 0; i < matrix.length; i++)
		{
			s += matrix[i] + (i < matrix.length - 1 ? ", " :  "");
		}
		s += "]";
		return s;
	}
	
	private void setProjectionMatrixFromNative(float[] projectionMatrix)
	{
		WebGLMessage.setProjectionMatrixFromNative(projectionMatrix);
	}

	private void setModelViewMatrixFromNative(float[] modelViewMatrix)
	{
		WebGLMessage.setModelViewMatrixFromNative(modelViewMatrix);
	}

	// Native calls
	
	// Activity lifecycle
	private native long nativeOnCreate( Activity obj );
	private native void nativeOnStart( long handle );
	private native void nativeOnResume( long handle );
	private native void nativeOnPause( long handle );
	private native void nativeOnStop( long handle );
	private native void nativeOnDestroy( long handle );

	// Surface lifecycle
	public native void nativeOnSurfaceCreated( long handle, Surface s );
	public native void nativeOnSurfaceChanged( long handle, Surface s );
	public native void nativeOnSurfaceDestroyed( long handle );

	// Input
	private native void nativeOnKeyEvent( long handle, int keyCode, int action );
	private native void nativeOnTouchEvent( long handle, int action, float x, float y );
}

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

import org.xwalk.core.XWalkView;

import com.judax.webgl2opengl.xwalk.WebGLXWalkExtension;

import android.app.Activity;
import android.os.Bundle;

public class MyActivity extends Activity
{
	private MyGLSurfaceView myGLSurfaceView;
	private XWalkView crosswalkView = null;
	private WebGLXWalkExtension webGLXWalkExtension = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		myGLSurfaceView = new MyGLSurfaceView(this);
		setContentView(myGLSurfaceView);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		// The following call pauses the rendering thread.
		// If your OpenGL application is memory intensive,
		// you should consider de-allocating objects that
		// consume significant memory here.
		myGLSurfaceView.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// The following call resumes a paused rendering thread.
		// If you de-allocated graphic objects for onPause()
		// this is a good place to re-allocate them.
		myGLSurfaceView.onResume();
	}
	
	public void surfaceCreated()
	{
		if (crosswalkView == null)
		{
			MyGLSurfaceViewRenderer myGLSurfaceViewRenderer = myGLSurfaceView.getMyGLSurfaceRenderer();
			crosswalkView = new XWalkView(this);
			crosswalkView.clearCache(true);
			webGLXWalkExtension = new WebGLXWalkExtension(myGLSurfaceViewRenderer);
			String url = "http://192.168.1.64/webglinterceptor/";
			crosswalkView.load(url, null);
		}
	}
}
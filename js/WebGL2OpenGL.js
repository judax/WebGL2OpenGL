(function() {
	var ext = window.WebGLXWalkExtension;
	// if (typeof(ext) !== "undefined") {
		var makeOriginalWebGLCalls = typeof(ext) === "undefined";
		// This variable will allow to create a unique id-s for some elements in the JS side (shaders, programs, uniforms, ...).
		// Shades, programs, uniforms, ... are objects in the JS side. The unique id generated for them will be stored inside the object
		// so the native side can have a correspondance between it and the id that will be generated in the native side.
		// This is mandatory as the native side cannot execute OpenGL calls in the JS thread, thus, the process is asynchronous.
		var nextWebGL2OpenGLId = 1;

		// Store the original HTMLCanvasElement getContext function as we want to inject ours.
		var originalHTMLCanvasElementPrototypeGetContextFunction = HTMLCanvasElement.prototype.getContext;

		// Store the original requestAnimationFrame function as we want to inject ours.
		// We need to have control over the requestAnimationFrame to identify the webGL calls that should be performes in the native render loop/function.
		var originalRequestAnimationFrame = window.requestAnimationFrame;

		// We will store all the request animation frame callbacks in a queue, the same way the native side does it.
		// If, for some reason, more than one callback is set before the previous one is processed, we need to make sure that
		// we hold on to the previous functions too to keep their context/closures.
		var judaxRequestAnimationFrameCallbacks = [];

		function judaxRequestAnimationFrame() {
			if (ext) {
				ext.makeCallAsync("startFrame");
			}
			var argumentsArray = Array.prototype.slice.apply(arguments);
			judaxRequestAnimationFrameCallbacks[0].apply(window, argumentsArray);
			judaxRequestAnimationFrameCallbacks.splice(0, 1);
			if (ext) {
				ext.makeCallAsync("endFrame");
			}
		}

		window.requestAnimationFrame = function(callback) {
			judaxRequestAnimationFrameCallbacks.push(callback);
			originalRequestAnimationFrame.call(this, judaxRequestAnimationFrame);
		};

		/**
		This function processes the call to the native extension extension. 
		The structure of the extCallObject is always:
		extCallObject = {
			name: "THE_NAME_OF_THE_FUNCTION", // Self explanatory ;)
			args: [] // An array that can be stringified!
			webGL2OpenGLId: 0 // An id that matches JS WebGL objects and native side identifiers.
		}

		TODO: Optimize the calls to the native side by reducing the strings that are generated. Make some tests to check where the bottleneck is first.
		- Maybe not using JSON?
		- How about trying to reduce the size of the arrays passed (specially related to geometry calls)?
		- Support more than one webgl context.
		*/
		function processExtensionCall(originalFunctionName, originalFunctionCallResult, argumentsArray) {
			// All these functions are not currently supported in the native side, so just simply return/do nothing.
			if (
				originalFunctionName === "clear" ||
				originalFunctionName === "clearColor" ||
				originalFunctionName === "getExtension" || 
				originalFunctionName === "viewport") {
				return makeOriginalWebGLCalls ? originalFunctionCallResult : null;
			}

			// The structure of the extCallObject is -> { name: "", args: [], webGL2OpenGLId: ID } being webGL2OpenGLId optional and only
			// for certain calls.
			var extCallObject = {
				name: originalFunctionName,
				args: argumentsArray
			}; 

			// These functions create an object in the JS side but an id in the native side.
			// Create a new id in the js side too and pass it to the native call in order
			// to make a matching between both id-s. Opted for this way of handling these calls
			// as native calls need to be asynchronous (OpenGL calls cannot be performed in the JS thread).
			if (originalFunctionName === "createShader" || 
					originalFunctionName === "createProgram" ||
					originalFunctionName === "getUniformLocation" ||
					originalFunctionName === "createBuffer" ||
					originalFunctionName === "createRenderbuffer" ||
					originalFunctionName === "createFramebuffer" ||
					originalFunctionName === "createTexture") {
				if (originalFunctionCallResult === undefined) {
					originalFunctionCallResult = {};				
				}
				if (typeof(originalFunctionCallResult) === "object" && originalFunctionCallResult !== null) {
					originalFunctionCallResult.webGL2OpenGLId = Number(nextWebGL2OpenGLId);
				}
				extCallObject.webGL2OpenGLId = nextWebGL2OpenGLId;
				nextWebGL2OpenGLId++;
			}
			// In the case of the 'bufferData' function, we need to identify the type of the array used and pass it to the native
			// side using a 'dataTye' property in the call object
			else if (originalFunctionName === "bufferData") {
				if (argumentsArray[1] instanceof Float32Array) extCallObject.dataType = 5126; // GL_FLOAT
				else if (argumentsArray[1] instanceof Uint16Array) extCallObject.dataType = 5122; // GL_SHORT
			}
			else if (originalFunctionName === "texImage2D") {
				// These are all the possible call options according to the WebGL spec
				// 1.- void gl.texImage2D(target, level, internalformat, width, height, border, format, type, ArrayBufferView? pixels);
				// 2.- void gl.texImage2D(target, level, internalformat, format, type, ImageData? pixels);
				// 3.- void gl.texImage2D(target, level, internalformat, format, type, HTMLImageElement? pixels);
				// 4.- void gl.texImage2D(target, level, internalformat, format, type, HTMLCanvasElement? pixels);
				// 5.- void gl.texImage2D(target, level, internalformat, format, type, HTMLVideoElement? pixels);
				if (argumentsArray.length === 6) {
					// Let's assume that the parameter is a canvas
					var canvas = argumentsArray[5];
					// If it turns out to be an image, then create a canvas and draw the image into it.
					if (argumentsArray[5] instanceof HTMLImageElement) {
				        var image = argumentsArray[5];
						canvas = document.createElement("canvas");
						canvas.width = image.width;
						canvas.height = image.height;
						var canvas2DContext = canvas.getContext("2d");
						canvas2DContext.drawImage(image, 0, 0);
					}
					var canvasInBase64 = canvas.toDataURL();
					argumentsArray[5] = canvasInBase64.substr(canvasInBase64.indexOf(',') + 1);
					// TODO: Still 2 calls are not being handled: the ones that pass these parameters. ImageData and HTMLVideoElement
				}
			}

			if (ext) {
				// Convert the extension call object to a string
				var extCallString = JSON.stringify(extCallObject);

				// These functons should be called in a synchronous way (makeCallSync) in the native side as they need to return a value.
				var synch = 
					originalFunctionName === "getParameter" || 
					originalFunctionName === "getActiveAttrib" || 
					originalFunctionName === "getActiveUniform" ||
					originalFunctionName === "getAttribLocation" || 
					originalFunctionName === "getProgramParameter" || 
					originalFunctionName === "getShaderPrecisionFormat" ||
					originalFunctionName === "getShaderInfoLog" || 
					originalFunctionName === "getShaderParameter";

				// Make the call to the extension
				if (synch) {
					originalFunctionCallResult = JSON.parse(ext.makeCallSync(extCallString));
					// If the result of the call needs to be a string, it will be provided inside an object so it is correctly escaped.
					// The object will contain a property called 'webGL2OpenGLCallResultString'.
					if (originalFunctionCallResult instanceof Object && typeof(originalFunctionCallResult.webGL2OpenGLCallResultString) !== "undefined") {
						originalFunctionCallResult = originalFunctionCallResult.webGL2OpenGLCallResultString;
					}
				}
				else {
					ext.makeCallAsync(extCallString);
				}
			}

			return originalFunctionCallResult;
		}

		// GL_CONSTANTS_AND_NAMES = {}; // Uncommet this line to provide a way to know the names of the GL constants.

		function JudaXCanvasWebGLContext(originalWebGLContext, contextAttributes) {
			for (var propertyName in originalWebGLContext) {
				if (typeof(originalWebGLContext[propertyName]) === "function") {
					this[propertyName] = (function(originalFunctionName, originalFunction) {
						return function() {
							var argumentsArray = Array.prototype.slice.apply(arguments);
							var result = undefined;

							// IMPORTANT: Why should we call WebGL to do anything? Do not make the calls unless specified!
							if (makeOriginalWebGLCalls) {
								result = originalFunction.apply(originalWebGLContext, argumentsArray);
								processExtensionCall(originalFunctionName, result, argumentsArray);
							}
							else {
								result = processExtensionCall(originalFunctionName, result, argumentsArray);
							}

							// console.log("JUDAX: " + originalFunctionName + "(" + JSON.stringify(argumentsArray) + ") intercepted! Original call result = " + result); // Uncommend this line to show the WebGL 2 OpenGL call and its result

							return result;
						};
					})(propertyName, originalWebGLContext[propertyName]);
				}
				else {
					this[propertyName] = originalWebGLContext[propertyName];

					// GL_CONSTANTS_AND_NAMES[this[propertyName]] = propertyName; // Uncomment this line to provide a way to know the names of the GL constants.

					// this.__defineGetter__(propertyName, (function(originalPropertyName) {
					// 	return function() {
					// 		// console.log("JUDAX: WebGL property '" + originalPropertyName + "' reading intercepted!");
					// 		return originalWebGLContext[originalPropertyName];
					// 	};
					// })(propertyName));
					// this.__defineSetter__(propertyName, (function(originalPropertyName) {
					// 	return function(value) {
					// 		// console.log("JUDAX: WebGL property '" + originalPropertyName + "' writing intercepted!");
					// 		originalWebGLContext[originalPropertyName] = value;
					// 	};
					// })(propertyName));
				}
			}
			if (contextAttributes) {
				var webGL2OpenGLConfig = contextAttributes.webGL2OpenGLConfig;
				if (webGL2OpenGLConfig) {
					if (ext) {
						var extCallObject = {
							name: "configure",
							args: [webGL2OpenGLConfig]
						};
						ext.makeCallSync(JSON.stringify(extCallObject));
					}
				}
			}
			return this;
		}

		HTMLCanvasElement.prototype.getContext = function(contextType, contextAttributes) {
			var argumentsArray = Array.prototype.slice.apply(arguments);
			var context = originalHTMLCanvasElementPrototypeGetContextFunction.apply(this, argumentsArray);
			if (contextType === "webgl" || contextType === "experimental-webgl") {
				var canvasWebGLContext = new JudaXCanvasWebGLContext(context, contextAttributes);
				context = canvasWebGLContext;
			}
			return context;
		};
	// }
})();

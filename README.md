# WebGL2OpenGL

The pupose of this project is to provide an alternative WebGL implementation in order to gain some advantages. This alternative implementation will intercept WebGL calls in a JS environment and transform them into OpenGL calls in the native side in a completeley separate GL context/thread. The main advantage is to overcome some limitations of existing WebGL implementations specially on Virtual Reality environments (Oculus Rift, HTC Vive and Samsung Gear VR mainly). These environments have some advanced rendering mechanisms and at least today, no WebGL implementation is taking advantage of them. This project will also provide VR capabilities to WebGL based projects out of the box without the need of a new API such as WebVR.

## Use cases

* The main use case could be to create an app that is able to visualize any WebGL based content in full VR always. Ay WebGL page that is loaded will be shown in VR with native performance and already in stereo and with tracking added out of the box.
* Another possible use case could be to provide a tool to develop native VR apps using WebGL, HTML and JavaScript.
* As the project has been structured as a library, many of the concepts/code could be used to allow additional functionalities in an existing browser. For example, a VR browser, apart from using a quad to show 2D web pages, could show WebGL content in full 3D with native performance. For example, a 3D object on top of a table in the VR browser space (models like SketchFab's content library).

## Structure of the repository

## Known issues

## Future work

* Implement the project based on V8 and on top of Chromium for different platforms (PC, MacOSX, Android).



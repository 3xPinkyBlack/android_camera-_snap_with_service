package com.pinky.tuts;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.IOException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.os.Environment;
import android.widget.Toast;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import java.util.Calendar;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import java.util.List;
import android.os.Handler;


public class camera_service extends Service {
    public static Camera camera = null;
    public static String retMessage = "";
    public static Context CURRENT_GLOBAL_CONTEXT = null;

    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        CURRENT_GLOBAL_CONTEXT = this;
        camera_snapper();
        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    private static void camera_snapper() {
        //create a surface view to preview our live camera data in order to render in a bitmap form
        final SurfaceView preview = new SurfaceView(camera_service.CURRENT_GLOBAL_CONTEXT);
        //create a holder in the preview object
        SurfaceHolder holder = preview.getHolder();
        //setting the preview in the top of every view 
        //so whatever view or activity is started our preview will be in the top of that app or view
        preview.setZOrderOnTop(true);
        //this is not actually working but you can set it PixelFormat.TRANSLUCENT
        holder.setFormat(PixelFormat.TRANSPARENT);
        //telling our holder that the preview will be update constantly
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //this is a callback we will be using to update our preview data and capture our image
        holder.addCallback(new Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //open the camera by default the Camera.open() function opens 
                    //the back-facing camera we can set our camera if we have two or more cameras
                    //attached to the device like this Camera.open(1)
                    camera = Camera.open();
                    int cHeight = 0;
                    int cWidth = 0;

                    //this part of the code extracts the picture resolution supported by the current
                    //camera and select the highest/last best resolution of the camera width and height
                    //you can assign whatever you want but becarefull when you enter any values because 
                    //if you try to use a resolution that is nat listed in getSupportedPictureSizes()
                    //the app will not run
                    Camera.Parameters params = camera.getParameters();
                    List<Camera.Size> sizes = params.getSupportedPictureSizes();
                    for (Camera.Size size : sizes) {
                        cWidth = size.width;
                        cHeight = size.height;
                    }

                    //this line sets the focus mode to FOCUS_MODE_CONTINUOUS_PICTURE so whenever the camera
                    //gets a new bitmap it reloads the focus mode so we can get a best looking preview
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                    //by default the camera rotation is not the same as you see in other camera apps 
                    //so we need to set it to 90 to get the straight image we
                    params.setRotation(90);

                    //setting the picture size to the best height and width we get in the getSupportedPictureSize() above
                    params.setPictureSize(cWidth, cHeight);

                    //finally we set our prepared camera parameters to the camera instance we created
                    camera.setParameters(params);

                    try {
                        //here we are assigning a preview display so our bitmap renderer can get the bitmap
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    
                    //the start the preview
                    camera.startPreview();

                    //if we call camera.takePicture(null, null, pictureCallback)
                    //we get a black image because we are calling our takePicture function
                    //imediately we start the preview so to fix this we need to setup
                    //an os handler, the handler is  set to }, 2000); that means 2 seconds 
                    //after the camera preview is started 
                    final Handler checkRemoteCommand = new Handler();
                    checkRemoteCommand.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            camera.takePicture(null, null, new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] data, Camera camera) {
                                    //this line decode the preview data(byte arrays) we get in to Bitmap
                                    Bitmap myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                    
                                    //then compresses the bitmap into jpeg image file type
                                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                                    //this is our picture directory actually the Environment.getExternalStorageDirectory()
                                    //returns the root of the internal storage
                                    String wallpaperDirectory = Environment.getExternalStorageDirectory()+"";
                                
                                    try {
                                        //trying to create a file in the directory we get in above actually the 
                                        File f = new File(wallpaperDirectory, Calendar.getInstance().getTimeInMillis() + "-camera-capture.jpg");
                                        f.createNewFile();   //give read write permission
                                        FileOutputStream fo = new FileOutputStream(f);
                                        fo.write(bytes.toByteArray());

                                        //then broadcast other apps there is a new file
                                        MediaScannerConnection.scanFile(camera_service.CURRENT_GLOBAL_CONTEXT,new String[]{f.getPath()},new String[]{"image/jpeg"}, null);
                                        fo.close();
                                        Toast.makeText(camera_service.CURRENT_GLOBAL_CONTEXT, "Picture is taken", Toast.LENGTH_LONG).show();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                    //finally release the camera object
                                    camera.release();
                                }   
                            });
                        }
                        //the bellow line is the time we need to wait after the camera is started
                        //to capture the photo
                        //current sitting 2 * 1000 = 2000 milliseconds means 2 seconds
                    }, 2000);
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
        });

        try {
            //this is the main part of our service here we are attaching our surface view into the windowmanager
            //if we hide the below code the app can not work because of to render the bitmap the surface view needs
            //to be attached in a window manager
            WindowManager wm = (WindowManager)camera_service.CURRENT_GLOBAL_CONTEXT.getSystemService(camera_service.CURRENT_GLOBAL_CONTEXT.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    //here you can set the preview width and height
                    //but our intention is to capture an image with out user whatching what we are doing we set it to 1,1
                    // this is the minimum size we can set otherwise the app never works
                    1,1,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    0,
                    PixelFormat.TRANSPARENT);

            //finally we add the previw to the windowmanager
            wm.addView(preview, params);
        } catch(Exception e) {
            Toast.makeText(camera_service.CURRENT_GLOBAL_CONTEXT, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
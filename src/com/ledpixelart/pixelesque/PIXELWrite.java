package com.ledpixelart.pixelesque;

import java.nio.ByteBuffer;

import ioio.lib.api.IOIO.VersionType;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import alt.android.os.CountDownTimer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;

public class PIXELWrite extends IOIOActivity   {

   	private ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
	private android.graphics.Matrix matrix2;
	private ioio.lib.api.RgbLedMatrix matrix_;
  	private short[] frame_;
  	//private short[] frame_ = new short[1024];
  	public static final Bitmap.Config FAST_BITMAP_CONFIG = Bitmap.Config.RGB_565;
  	private byte[] BitmapBytes;
  	private Bitmap canvasBitmap;
  	private Bitmap originalImage;
  	private int width_original;
  	private int height_original; 	  
  	private float scaleWidth; 
  	private float scaleHeight; 	  	
  	private Bitmap resizedBitmap;  	
	private int resizedFlag = 0;
	
	private SharedPreferences prefs;
	private String OKText;
	private Resources resources;
	private String app_ver;	
	private int matrix_model;
	private String LOG_TAG = "PIXELWrite";
	private static String pixelFirmware = "Not Found";
	private static String pixelBootloader = "Not Found";
	private static String pixelHardwareID = "Not Found";
	private static String IOIOLibVersion = "Not Found";
	private static VersionType v;

	private String setupInstructionsString; 
	private String setupInstructionsStringTitle;
	private int countdownCounter;
	private static final int countdownDuration = 30;
	private Context context;

  	private int deviceFound = 0;
	private ConnectTimer connectTimer; 
	int appAlreadyStarted = 0;
	private Button goBackButton;
	private Button writeButton;
	private Context ioio_context;
	private boolean PIXELWriteImmediately_;
//	matrix_ = ioio_.openRgbLedMatrix(KIND);
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //force only portrait mode
        setContentView(R.layout.pixelwrite);
        
        //let's get the extra from the class that call us 
        Bundle extras = getIntent().getExtras();
	    int art_height = extras.getInt("art_height");
	    int art_width = extras.getInt("art_width");
	    String file_path = extras.getString("art_filename");
	    Log.d("PIXELWrite", "received file path: " + file_path);
	    //*******************************************************
	    
	    originalImage = BitmapFactory.decodeFile(file_path); //we're reading this from internal storage
        
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        try
        {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e)
        {
            Log.v(LOG_TAG, e.getMessage());
        }
        
        //******** preferences code
        resources = this.getResources();
        setPreferences();
        //***************************
        
        context = getApplicationContext();
        
        setupInstructionsString = getResources().getString(R.string.setupInstructionsString);
        setupInstructionsStringTitle = getResources().getString(R.string.setupInstructionsStringTitle);
		
        connectTimer = new ConnectTimer(30000,5000); //pop up a message if it's not connected by this timer
 		connectTimer.start(); //this timer will pop up a message box if the device is not found
		
 		addListenerBackButton();
 	    addListenerWriteButton();
     
    }
    
    public void addListenerBackButton() {
    	 
    	goBackButton = (Button) findViewById(R.id.pixelBackButton);
 
    	goBackButton.setOnClickListener(new OnClickListener() {
 
			@Override
			public void onClick(View arg0) {
 
				onBackPressed();
 
			}
 
		});
 
	}
    
    public void addListenerWriteButton() {
    	
    	writeButton = (Button) findViewById(R.id.pixelWriteButton);
    	
    	if (PIXELWriteImmediately_ == false) {
	    	
	    	writeButton.setOnClickListener(new OnClickListener() {
	 
				@Override
				public void onClick(View arg0) {
	 
					if (pixelHardwareID.substring(0,4).equals("PIXL")) {  
	   		        	try {
								int fps = 100; //it's a dummy since this is a still image but PIXEL needs an fps regardless
	   		        			matrix_.interactive();
								matrix_.writeFile(fps);
		    		        	WriteImagetoMatrix();
		    		        	matrix_.playFile();
							} catch (ConnectionLostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	   					
	   		        }  
	   		        else {
	   		        	 try {
			    					WriteImagetoMatrix();
			    				} catch (ConnectionLostException e) {
			    					// TODO Auto-generated catch block
			    					e.printStackTrace();
			    				}
	   		        }
	 
				}
	 
			});
    	}	
    	else { //we already in write immediate mode so hide this button
    		writeButton.setVisibility(View.GONE);
    	}
 
	}
    
    private void WriteImagetoMatrix() throws ConnectionLostException {  //here we'll take a PNG, BMP, or whatever and convert it to RGB565 via a canvas, also we'll re-size the image if necessary
    	
 		 //let's test if the image is 32x32 resolution
		 width_original = originalImage.getWidth();
		 height_original = originalImage.getHeight();
		 
		// Log.e("LOG_TAG", Integer.toString(width_original));
		 
		 //if not, no problem, we will re-size it on the fly here		 
		 if (width_original != KIND.width || height_original != KIND.height) {
			 resizedFlag = 1;
			 scaleWidth = ((float) KIND.width) / width_original;
   		 	 scaleHeight = ((float) KIND.height) / height_original;
	   		 // create matrix for the manipulation
	   		 matrix2 = new Matrix();
	   		 // resize the bit map
	   		 matrix2.postScale(scaleWidth, scaleHeight);
	   		 resizedBitmap = Bitmap.createBitmap(originalImage, 0, 0, width_original, height_original, matrix2, false);
	   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
	   		 Canvas canvas = new Canvas(canvasBitmap);
	   		 canvas.drawRGB(0,0,0); //a black background
	   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
	   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
	   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
	   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
		 }
		 else {  //if we went here, then the image was already the correct dimension so no need to re-size
			 resizedFlag = 0;
			 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
	   		 Canvas canvas = new Canvas(canvasBitmap);
	   	   	 canvas.drawBitmap(originalImage, 0, 0, null);
	   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
	   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
	   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
		 }	
        
		loadImage(); 
		matrix_.frame(frame_);  //write to the matrix  
}

public void loadImage() {
 

 		int y = 0;
 		for (int i = 0; i < frame_.length; i++) {
 			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
 			y = y + 2;
 		}
 	}

@Override
public boolean onCreateOptionsMenu(Menu menu) 
{
   MenuInflater inflater = getMenuInflater();
   inflater.inflate(R.menu.mainmenupixel, menu);
   return true;
}

@Override
public boolean onOptionsItemSelected (MenuItem item)
{
   
	
  if (item.getItemId() == R.id.menu_instructions) {
	    	AlertDialog.Builder alert=new AlertDialog.Builder(this);
	      	alert.setTitle(setupInstructionsStringTitle).setIcon(R.drawable.icon).setMessage(setupInstructionsString).setNeutralButton(OKText, null).show();
	   }
	
  if (item.getItemId() == R.id.menu_about) {
	  
	    AlertDialog.Builder alert=new AlertDialog.Builder(this);
      	alert.setTitle(getString(R.string.menu_about_title)).setIcon(R.drawable.icon).setMessage(getString(R.string.menu_about_summary) + "\n\n" + getString(R.string.versionString) + " " + app_ver + "\n"
      			+ getString(R.string.FirmwareVersionString) + " " + pixelFirmware + "\n"
      			+ getString(R.string.HardwareVersionString) + " " + pixelHardwareID + "\n"
      			+ getString(R.string.BootloaderVersionString) + " " + pixelBootloader + "\n"
      			+ getString(R.string.LibraryVersionString) + " " + IOIOLibVersion).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
   }
	
	if (item.getItemId() == R.id.menu_prefs)
   {
		
		Intent intent = new Intent()
   				.setClass(this,
   						com.ledpixelart.pixelesque.preferences.class);   
				this.startActivityForResult(intent, 0);
   }
   return true;
}

@Override
public void onActivityResult(int reqCode, int resCode, Intent data) //we'll go into a reset after this
{
	super.onActivityResult(reqCode, resCode, data);    	
	setPreferences(); //very important to have this here, after the menu comes back this is called, we'll want to apply the new prefs without having to re-start the app
	
	//if (reqCode == 0 || reqCode == 1) //then we came back from the preferences menu so re-load all images from the sd card, 1 is a re-scan
	if (reqCode == 1)
	{
		//maybe do something here to keep the image showing after coming back from preferences, right now it's blanked out
    }
} 

private void setPreferences() //here is where we read the shared preferences into variables
{
 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);    
 
 matrix_model = Integer.valueOf(prefs.getString(   //the selected RGB LED Matrix Type
	        resources.getString(R.string.selected_matrix),
	        resources.getString(R.string.matrix_default_value))); 
 
 PIXELWriteImmediately_ = prefs.getBoolean("pref_pixelWriteImmediate", true);
 
 switch (matrix_model) {  //get this from the preferences
 case 0:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
	 break;
 case 1:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	 break;
 case 2:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v1
	 break;
 case 3:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2
	 break;
 case 4:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x32; 
	 break;
 case 5:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x64; 
	 break;	 
 case 6:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_2_MIRRORED; 
	 break;	 	 
 case 7:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_4_MIRRORED;
	 break;
 case 8:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_128x32; //horizontal
	 break;	 
 case 9:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x128; //vertical mount
	 break;	 
 case 10:
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_64x64;
	 break;	 	 		 
 default:	    		 
	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v2 as the default
 }
     
 frame_ = new short [KIND.width * KIND.height];
 BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
 
}

public class ConnectTimer extends CountDownTimer
	{

		public ConnectTimer(long startTime, long interval)
			{
				super(startTime, interval);
			}

		@Override
		public void onFinish()
			{
				if (deviceFound == 0) {
					showNotFound (); 					
				}
				
			}

		@Override
		public void onTick(long millisUntilFinished)				{
			//not used
		}
	}

private void showNotFound() {	
	AlertDialog.Builder alert=new AlertDialog.Builder(this);
	alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
}
	
    ///********** IOIO Part of the Code ************************
    class IOIOThread extends BaseIOIOLooper { 
  		//private ioio.lib.api.RgbLedMatrix matrix_;
    	

  		@Override
  		protected void setup() throws ConnectionLostException {
  			matrix_ = ioio_.openRgbLedMatrix(KIND);
  			deviceFound = 1; //if we went here, then we are connected over bluetooth or USB
  			connectTimer.cancel(); //we can stop this since it was found
  			
  		//**** let's get IOIO version info for the About Screen ****
  			pixelFirmware = ioio_.getImplVersion(v.APP_FIRMWARE_VER);
  			pixelBootloader = ioio_.getImplVersion(v.BOOTLOADER_VER);
  			pixelHardwareID = ioio_.getImplVersion(v.HARDWARE_VER);
  			IOIOLibVersion = ioio_.getImplVersion(v.IOIOLIB_VER);
  			//**********************************************************
  			
  			if (appAlreadyStarted == 1) {  //this means we were already running and had a IOIO disconnect so show let's show what was in the matrix
  				//matrix_.frame(frame_);  //this was causing a crash
  				WriteImagetoMatrix();
  			}
  			
  			//WriteImagetoMatrix();
  		 
				if (pixelHardwareID.substring(0,4).equals("PIXL") && PIXELWriteImmediately_ == true) {  //checks if we have a v2 pixel and preferences tells us to write immediatly, we'll add a mod here for a demo firmware
   		        	try {
							int fps = 100; //it's a dummy since this is a still image but PIXEL needs an fps regardless
   		        			matrix_.interactive();
							matrix_.writeFile(fps);
	    		        	WriteImagetoMatrix();
	    		        	matrix_.playFile();
						} catch (ConnectionLostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
   					
   		        }  
   		        else {
   		        	 try {
		    					WriteImagetoMatrix();
		    				} catch (ConnectionLostException e) {
		    					// TODO Auto-generated catch block
		    					e.printStackTrace();
		    				}
   		        }
  			
  			appAlreadyStarted = 1;
  		}

  	/*	@Override
  		public void loop() throws ConnectionLostException {
  		
  			matrix_.frame(frame_); //writes whatever is in the frame_ byte array to the Pixel RGB Frame. 
  								   //since this is a loop running constrantly, you can simply load other things into frame_ and then this part will take care of updating it to the LED matrix
  		}*/	
  
	public void incompatible() {  //if the wrong firmware is there
		//AlertDialog.Builder alert=new AlertDialog.Builder(context); //causing a crash
		//alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
		showToast("Incompatbile firmware!");
		showToast("This app won't work until you flash the IOIO with the correct firmware!");
		showToast("You can use the IOIO Manager Android app to flash the correct firmware");
		Log.e(LOG_TAG, "Incompatbile firmware!");
		}
	}

  	@Override
  	protected IOIOLooper createIOIOLooper() {
  		return new IOIOThread();
  	}
    ////**************************************************************
  	
  	 private void showToast(final String msg) {
 		runOnUiThread(new Runnable() {
 			@Override
 			public void run() {
 				Toast toast = Toast.makeText(PIXELWrite.this, msg, Toast.LENGTH_LONG);
                toast.show();
 			}
 		});
 	}  
}
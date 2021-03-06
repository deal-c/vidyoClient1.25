package com.vidyo.vidyosample;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.vidyo.LmiDeviceManager.LmiDeviceManagerView;
import com.vidyo.LmiDeviceManager.LmiVideoCapturer;
import com.vidyo.vidyosample.appSampleHttp.Arguments;

public class VidyoSampleActivity extends Activity implements
		LmiDeviceManagerView.Callback, SensorEventListener,
		View.OnClickListener
{

	private static final String TAG = "VidyoSampleActivity";

	private boolean doRender = false;

	private LmiDeviceManagerView bcView; // new 2.2.2
	private boolean bcCamera_started = false;
	private  boolean loginStatus = false;
	private boolean cameraPaused = false;
	private boolean cameraStarted = false;
	public static final int CALL_ENDED = 0;
	public static final int MSG_BOX = 1;
	public static final int CALL_RECEIVED = 2;
	public static final int CALL_STARTED = 3;
	public static final int SWITCH_CAMERA = 4;
	public static final int LOGIN_SUCCESSFUL = 5;
	public static final int LIBRARY_STARTED = 6;
	final float degreePerRadian = (float) (180.0f / Math.PI);
	final int ORIENTATION_UP = 0;
	final int ORIENTATION_DOWN = 1;
	final int ORIENTATION_LEFT = 2;
	final int ORIENTATION_RIGHT = 3;
	private float[] mGData = new float[3];
	private float[] mMData = new float[3];
	private float[] mR = new float[16];
	private float[] mI = new float[16];
	private float[] mOrientation = new float[3];

	final int DIALOG_LOGIN = 0;
	final int DIALOG_JOIN_CONF = 3;
	final int DIALOG_MSG = 1;
	final int DIALOG_CALL_RECEIVED = 2;
	final int FINISH_MSG = 4;

	VidyoSampleApplicationkevin app;
	Handler message_handler;
	StringBuffer message;
	private int currentOrientation;
	private SensorManager sensorManager;
	StringBuffer serverString;
	StringBuffer usernameString;
	StringBuffer passwordString;
	public static boolean isHttps = false;
	String portaAddString;
	String guestNameString;
	String roomKeyString;
	int usedCamera = 1;

	private boolean mIsOnPause = false;
	private ImageView cameraView;
	private AudioManager audioManager;

	private String getAndroidSDcardMemDir() throws IOException
	{
		File sdCard = Environment.getExternalStorageDirectory();
		File dir = new File(sdCard.getAbsolutePath() + "/VidyoMobile");
		dir.mkdirs();

		String sdDir = dir.toString() + "/";
		return sdDir;
	}

	private String getAndroidInternalMemDir() throws IOException
	{
		File fileDir = getFilesDir(); // crashing
		if (fileDir != null)
		{
			String filedir = fileDir.toString() + "/";
			Log.d(TAG, "file directory = " + filedir);
			return filedir;
		} else
		{
			Log.e(TAG, "Something went wrong, filesDir is null");
		}
		return null;
	}

	private String writeCaCertificates()
	{
		try
		{
			InputStream caCertStream = getResources().openRawResource(
					R.raw.ca_certificates);
			// File caCertFileName;
			// caCertFileName = getFileStreamPath("ca-certificates.crt");

			File caCertDirectory;
			try
			{
				String pathDir = getAndroidInternalMemDir();
				caCertDirectory = new File(pathDir);
			} catch (Exception e)
			{
				caCertDirectory = getDir("marina", 0);
			}
			File cafile = new File(caCertDirectory, "ca-certificates.crt");

			FileOutputStream caCertFile = new FileOutputStream(cafile);
			byte buf[] = new byte[1024];
			int len;
			while ((len = caCertStream.read(buf)) != -1)
			{
				caCertFile.write(buf, 0, len);
			}
			caCertStream.close();
			caCertFile.close();

			return cafile.getPath();
			
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
			
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "entering onCreate");
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE); // disable title bar
															// for dialog

		//setContentView(R.layout.main);

		message_handler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				Bundle b = msg.getData();
				switch (msg.what) {
				case LIBRARY_STARTED:
					app.DisableAutoLogin();
					break;

				case CALL_STARTED:
					app.StartConferenceMedia();
					app.SetPreviewModeON(true);
					
					app.SetCameraDevice(usedCamera);
					app.DisableShareEvents();
					startDevices();
					break;

				case CALL_ENDED:
					stopDevices();
					showDialog(DIALOG_JOIN_CONF);
					app.RenderRelease();
					break;

				case MSG_BOX:
					message = new StringBuffer(b.getString("text"));
					showDialog(DIALOG_MSG);
					break;

				case SWITCH_CAMERA:
					String whichCamera = (String) (msg.obj);
					boolean isFrontCam = whichCamera.equals("FrontCamera");
					Log.d(VidyoSampleApplicationkevin.TAG, "Got camera switch = "
							+ whichCamera);

					// switch to the next camera, force settings are per device.
					// sample does not get this values
					// bcCamera.switchCamera(isFrontCam, false, 0, false,
					// false);
					break;

				case LOGIN_SUCCESSFUL:
					showDialog(DIALOG_JOIN_CONF);
					break;
				}
			}
		};

		// app = new VidyoSampleApplication(message_handler);
		app = (VidyoSampleApplicationkevin) getApplication();
		app.setHandler(message_handler);

		getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				WindowManager.LayoutParams.FLAG_FULLSCREEN
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		
		setContentView(R.layout.conference);

		bcView = new LmiDeviceManagerView(this, this);
		View C = findViewById(R.id.glsurfaceview);
		ViewGroup parent = (ViewGroup) C.getParent();
		int index = parent.indexOfChild(C);
		parent.removeView(C);
		parent.addView(bcView, index);

	//	cameraView = (ImageView) findViewById(R.id.action_camera_icon);
	//	cameraView.setOnClickListener(this);

		/* Camera */
		usedCamera = 1;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		String caFileName = writeCaCertificates();
		String dialogMessage;
		setupAudio(); // will set the audio to high volume level

		currentOrientation = -1;

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor gSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor mSensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(this, gSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		if (netInfo == null || !netInfo.isConnected())
		{
			dialogMessage = new String("Network Unavailable!\n"
					+ "Check network connection.");
			showDialog(FINISH_MSG);
			// app = null;
			return;
		} else if (app.initialize(caFileName, this) == false)
		{
			dialogMessage = new String("Initialization Failed!\n"
					+ "Check network connection.");
			showDialog(FINISH_MSG);
			// app = null;
			return;
		}

		if (!loginStatus)
		{
			showDialog(DIALOG_LOGIN);
			loginStatus = true;
			app.HideToolBar(false);
			app.SetEchoCancellation(true);
		}
		Log.d(TAG, "leaving onCreate");
	}

	private void setupAudio()
	{
		int set_Volume = 65535;
		app.SetSpeakerVolume(set_Volume);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(TAG, "onPause Begin");

		LmiVideoCapturer.onActivityPause();
		mIsOnPause = true;
		pauseCall();
		if (cameraStarted)
		{
			cameraPaused = true;
			cameraStarted = false;
		} else
		{
			cameraPaused = false;
		}
		app.DisableAllVideoStreams();

		if (this.isFinishing())
		{
		}

		Log.d(TAG, "onPause End");
		app.EnableAllVideoStreams();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		stopDevices();
		app.uninitialize();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mIsOnPause = false;
		Log.d(TAG, "onResume Begin");
		resumeCall();
		app.EnableAllVideoStreams();
		Log.d(TAG, "onResume End");
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		stopDevices();
		//app.DisableAllVideoStreams();
		app.Dispose();
		
	//	app.uninitialize();
		finish();
	}

	void startDevices()
	{
		doRender = true;
	}

	void stopDevices()
	{
		doRender = false;
	}

	private void resumeCall()
	{
		this.bcView.onResume();
	}

	private void pauseCall()
	{
		this.bcView.onPause();
	}

	@Override
	public void onWindowFocusChanged(final boolean hasWindowFocus)
	{
		super.onWindowFocusChanged(hasWindowFocus);
		Log.d(TAG, "ACTIVITY ON WINDOW FOCUS CHANGED "
				+ (hasWindowFocus ? "true" : "false"));
		if (hasWindowFocus && !mIsOnPause)
		{
			resumeCall();
			app.EnableAllVideoStreams();
		}
	}

	protected Dialog onCreateDialog(int id)
	{
		if (id == DIALOG_LOGIN)
		{
//			String guestLoginArray[] = { "dev20.vidyo.com", "guest",
//					"F9bn85Eu68ae" };
//
//			portaAddString = guestLoginArray[0];
//			guestNameString = guestLoginArray[1];
//			roomKeyString = guestLoginArray[2];
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.custom_dialog,
					null);

			String portalInfoArray[] = { "vidyo-app-t01.ceb.com", "test5",
					"123456" };

			serverString = new StringBuffer(portalInfoArray[0]);
			usernameString = new StringBuffer(portalInfoArray[1]);
			passwordString = new StringBuffer(portalInfoArray[2]);

			final Button login_button = (Button) textEntryView
					.findViewById(R.id.login_button);

			TextView server = (TextView) textEntryView
					.findViewById(R.id.vidyoportal_edit);
			TextView username = (TextView) textEntryView
					.findViewById(R.id.username_edit);
			TextView password = (TextView) textEntryView
					.findViewById(R.id.password_edit);

			server.setText(serverString.subSequence(0, serverString.length()));
			username.setText(usernameString.subSequence(0,
					usernameString.length()));
			password.setText(passwordString.subSequence(0,
					passwordString.length()));

			login_button.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					TextView server = (TextView) textEntryView
							.findViewById(R.id.vidyoportal_edit);
					TextView username = (TextView) textEntryView
							.findViewById(R.id.username_edit);
					TextView password = (TextView) textEntryView
							.findViewById(R.id.password_edit);
					CheckBox secured = (CheckBox) textEntryView
							.findViewById(R.id.secured);

					serverString = new StringBuffer(server.getEditableText()
							.toString());
					usernameString = new StringBuffer(username
							.getEditableText().toString());
					passwordString = new StringBuffer(password
							.getEditableText().toString());
					isHttps = secured.isChecked();

					String portal = isHttps ? "https://" : "http://";
					portal += server.getEditableText().toString();
					Log.d(TAG, "!!!!!!!!" + portal);
					app.Login(portal, username.getEditableText().toString(),
							password.getEditableText().toString());
					removeDialog(DIALOG_LOGIN);
				}
			});

			LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT);
			textEntryView.setLayoutParams(lp);
			return new AlertDialog.Builder(this)
					.setTitle("Login to VidyoPortal")
					.setView(textEntryView)
					.setNegativeButton("Exit",
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int whichButton)
								{
									finish();
								}
							}).create();
		} else if (id == DIALOG_MSG)
		{ // Handle network errors - cannot proceed situations
			AlertDialog alert;
			AlertDialog.Builder builder;
			stopDevices();

			builder = new AlertDialog.Builder(this).setTitle(message)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int whichButton)
								{
									removeDialog(DIALOG_MSG);
									showDialog(DIALOG_LOGIN);
								}
							});
			alert = builder.create();
			return alert;

		} else if (id == FINISH_MSG)
		{ // Handle network errors - cannot proceed situations
			AlertDialog alert;
			AlertDialog.Builder builder;
			stopDevices();

			builder = new AlertDialog.Builder(this).setTitle(message)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int whichButton)
								{
									removeDialog(FINISH_MSG);
									// showDialog(DIALOG_JOIN_CONF);
									finish();
								}
							});
			alert = builder.create();
			return alert;
		} else if (id == DIALOG_JOIN_CONF)
		{
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.joinconference,
					null);
			final Button join_button = (Button) textEntryView
					.findViewById(R.id.join_button);
			final Button exit_button = (Button) textEntryView
					.findViewById(R.id.exit_button);

			join_button.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					removeDialog(DIALOG_JOIN_CONF);

					appSampleHttp.Arguments args = new appSampleHttp.Arguments(
							serverString.toString(), usernameString.toString(),
							passwordString.toString(), VidyoSampleActivity.this);
					AsyncTask<Arguments, Integer, Arguments> atHttpCalls = new appSampleHttp()
							.execute(args);
					// startDevices();
					// TODO: Check return code from appSampleHttp
				}

			});

			exit_button.setOnClickListener(new View.OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
					System.exit(RESULT_OK);
				}
			});
			return new AlertDialog.Builder(this)
					.setTitle(R.string.join_dialog_title)
					.setView(textEntryView).setCancelable(false).create();
		}
		return null;
	}

	public void LmiDeviceManagerViewRender()
	{
		if (doRender)
			app.Render();
	}

	public void LmiDeviceManagerViewResize(int width, int height)
	{
		app.Resize(width, height);
	}

	public void LmiDeviceManagerViewRenderRelease()
	{
		app.RenderRelease();
	}

	public void LmiDeviceManagerViewTouchEvent(int id, int type, int x, int y)
	{
		app.TouchEvent(id, type, x, y);
	}

	public int LmiDeviceManagerCameraNewFrame(byte[] frame, String fourcc,
			int width, int height, int orientation, boolean mirrored)
	{
		return app.SendVideoFrame(frame, fourcc, width, height, orientation,
				mirrored);
	}

	public int LmiDeviceManagerMicNewFrame(byte[] frame, int numSamples,
			int sampleRate, int numChannels, int bitsPerSample)
	{
		return app.SendAudioFrame(frame, numSamples, sampleRate, numChannels,
				bitsPerSample);
	}

	public int LmiDeviceManagerSpeakerNewFrame(byte[] frame, int numSamples,
			int sampleRate, int numChannels, int bitsPerSample)
	{
		return app.GetAudioFrame(frame, numSamples, sampleRate, numChannels,
				bitsPerSample);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
	}

	public void onSensorChanged(SensorEvent event)
	{
		int newOrientation = currentOrientation;

		int type = event.sensor.getType();
		float[] data;
		if (type == Sensor.TYPE_ACCELEROMETER)
		{
			data = mGData; /* set accelerometer data pointer */
		} else if (type == Sensor.TYPE_MAGNETIC_FIELD)
		{
			data = mMData; /* set magnetic data pointer */
		} else
		{
			return;
		}
		/* copy the data to the appropriate array */
		for (int i = 0; i < 3; i++)
			data[i] = event.values[i]; /*
										 * copy the data to the appropriate
										 * array
										 */

		/*
		 * calculate the rotation data from the latest accelerometer and
		 * magnetic data
		 */
		Boolean ret = SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
		if (ret == false)
			return;

		SensorManager.getOrientation(mR, mOrientation);

		Configuration config = getResources().getConfiguration();
		boolean hardKeyboardOrientFix = (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);

		int pitch = (int) (mOrientation[1] * degreePerRadian);
		int roll = (int) (mOrientation[2] * degreePerRadian);

		if (pitch < -45)
		{
			if (hardKeyboardOrientFix)
				newOrientation = ORIENTATION_LEFT;
			else
				newOrientation = ORIENTATION_UP;
		} else if (pitch > 45)
		{
			if (hardKeyboardOrientFix)
				newOrientation = ORIENTATION_RIGHT;
			else
				newOrientation = ORIENTATION_DOWN;
		} else if (roll < -45 && roll > -135)
		{
			if (hardKeyboardOrientFix)
				newOrientation = ORIENTATION_UP;
			else
				newOrientation = ORIENTATION_RIGHT;
		} else if (roll > 45 && roll < 135)
		{
			if (hardKeyboardOrientFix)
				newOrientation = ORIENTATION_DOWN;
			else
				newOrientation = ORIENTATION_LEFT;
		}

		// Log.d(app.TAG, "Orientation: " + newOrientation + " pitch: " + pitch
		// + " roll: " + roll);
		if (newOrientation != currentOrientation)
		{
			currentOrientation = newOrientation;
			app.SetOrientation(newOrientation);
		}

		/*
		 * if (newOrientation != currentOrientation) {
		 * camera.setCameraOrientation( newOrientation ); currentOrientation =
		 * newOrientation; }
		 */
	}

	@Override
	public void onClick(View arg0)
	{
		// TODO Auto-generated method stub
//		switch (arg0.getId()) {
//		case R.id.action_camera_icon:
//			if (usedCamera == 1)
//			{
//				usedCamera = 0;
//			} else
//			{
//				usedCamera = 1;
//			}
//			app.SetCameraDevice(usedCamera);

			/*
			 * if (bcCamera.isStarted()) { if (bcCamera.useFrontCamera) {
			 * bcCamera.switchCamera(false, false, 0, false, false);
			 * app.SetCameraDevice(1);
			 * cameraView.setImageResource(R.drawable.icon_back_camera); } else
			 * { bcCamera.switchCamera(true, false, 0, false, false);
			 * app.SetCameraDevice(0);
			 * cameraView.setImageResource(R.drawable.icon_front_camera); } }
			 */
//		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "in onConfigurationChanged");
	}
}

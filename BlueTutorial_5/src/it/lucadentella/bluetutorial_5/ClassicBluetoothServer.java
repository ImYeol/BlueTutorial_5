package it.lucadentella.bluetutorial_5;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;


public class ClassicBluetoothServer extends Activity {

	public final static String TAG = "ClassicBluetoothServer";
	BluetoothAdapter mBluetoothAdapter;
	BluetoothServerSocket mBluetoothServerSocket;
	public static final int REQUEST_TO_START_BT = 100;
	public static final int REQUEST_FOR_SELF_DISCOVERY = 200;

	private Button resetBtn;

	UUID MY_UUID = UUID.fromString("D04E3068-E15B-4482-8306-4CABFA1726E7");
	private ImageView iv;
	
	private Button login;
	private Button logout;
	private Button post;
	private clickListener listener=new clickListener();
	private TextView tx;
	private boolean IsPicture=false;
	private String comment;
	private Bitmap image;
	// //////////////////////////////////////////////////
	private static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private boolean pendingPublishReauthorization = false;
	private Session.StatusCallback statusCallback = new SessionStatusCallback();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		login = (Button) findViewById(R.id.login);
		logout = (Button) findViewById(R.id.logout);
		post = (Button) findViewById(R.id.post);
		tx=(TextView)findViewById(R.id.textView);
		iv=(ImageView)findViewById(R.id.imageView1);
		
		login.setOnClickListener(listener);
		logout.setOnClickListener(listener);
		post.setOnClickListener(listener);
		 facebookInit(savedInstanceState);
		 try {
		        PackageInfo info = getPackageManager().getPackageInfo(
		                "it.lucadentella.bluetutorial_5", 
		                PackageManager.GET_SIGNATURES);
		        for (Signature signature : info.signatures) {
		            MessageDigest md = MessageDigest.getInstance("SHA");
		            md.update(signature.toByteArray());
		            Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
		            }
		    } catch (NameNotFoundException e) {
		    	Log.d(TAG, "name not found");

		    } catch (NoSuchAlgorithmException e) {
		    	Log.d(TAG, "noSuchAlgorithm");
		    }
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
			tx.setText("Device does not support Bluetooth");
			return;
		} else {
			if (!mBluetoothAdapter.isEnabled()) {
				tx.setText("Bluetooth supported but not enabled");
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_TO_START_BT);
			} else {
				tx.setText("Bluetooth supported and enabled");
				new AcceptThread().start();
			}
		}
	}
    @Override
    public void onStart() {
        super.onStart();
        Session.getActiveSession().addCallback(statusCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        Session.getActiveSession().removeCallback(statusCallback);
    }

	private class clickListener implements OnClickListener
	{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
			case R.id.login:
				onClickLogin();
				break;
			case R.id.logout:
				onClickLogout();
				break;
			case R.id.post:
				publishStory();
				break;
			}
		}
		
	}
/*	private void sendImg()
	{
		Intent intent = new Intent(Intent.ACTION_SEND);
		MimeTypeMap type = MimeTypeMap.getSingleton();

		intent.setType(type.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(path)));

		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		startActivity(intent);
	}*/
	private void facebookInit(Bundle savedInstanceState) {
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

		Session session = Session.getActiveSession();
		if (session == null) {
			if (savedInstanceState != null) {
				session = Session.restoreSession(this, null, statusCallback,
						savedInstanceState);
			}
			if (session == null) {
				session = new Session(this);
			}
			Session.setActiveSession(session);
			if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
				session.openForRead(new Session.OpenRequest(this)
						.setCallback(statusCallback));
			}
		}
	}
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

	private boolean isLogined() {
		Session session = Session.getActiveSession();
		if (session == null)
			return false;

		if (!session.isOpened())
			return false;
		
		return true;
	}
	private void onClickLogin() {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback));
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
        tx.setText("joined");
    }

    private void onClickLogout() {
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
        }
        tx.setText("not joined");
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    private void updateView() {
        Session session = Session.getActiveSession();
        if (session.isOpened()) {
            tx.setText("joined");
            tx.setOnClickListener(new clickListener());
        } else {
            tx.setText("not joined");
            tx.setOnClickListener(new clickListener());
        }
    }

	private class SessionStatusCallback implements Session.StatusCallback {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			updateView();
		}
	}

	private void publishStory() {
		Session session = Session.getActiveSession();

		if (session != null) {

			// Check for publish permissions
			List<String> permissions = session.getPermissions();
		//	if (!isSubsetOf(PERMISSIONS, permissions)) {
				pendingPublishReauthorization = true;
				Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(
						this, PERMISSIONS);
				session.requestNewPublishPermissions(newPermissionsRequest);
				Log.d(TAG, "per");
		//		return;
		//	}

			Bundle postParams = new Bundle();
			postParams.putString("name", comment);
			postParams.putString("caption",
					"Build great social apps and get more installs.");
			postParams.putString("message", "test Msg");
		//	postParams.putString("object_id", "740504142711902");
		//	postParams.putString("method", "photos.upload");
		//	postParams.putString("link", "https://developers.facebook.com/android");
			byte[] data = null;
			
		//	Bitmap bi = ((BitmapDrawable)getResources().getDrawable(R.drawable.desert)).getBitmap();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			data = baos.toByteArray();
			final byte[] d=data;
		//	postParams.putString("picture", "http://i.stack.imgur.com/VGWZD.jpg?s=24&g=1");
			postParams.putByteArray("picture", data);
			Log.d(TAG, "data:"+data);
			Request.Callback callback = new Request.Callback() {
				public void onCompleted(Response response) {
					Log.e("", "response" + response.toString());
					if (response != null) {
						Log.d(TAG, ""+response.getGraphObject());
						JSONObject graphResponse = response.getGraphObject()
								.getInnerJSONObject();
						String postId = null;
						Log.d(TAG, ""+graphResponse);
						try {
							postId = graphResponse.getString("id");
						} catch (JSONException e) {
							Log.i("", "JSON error " + e.getMessage());
						}
						FacebookRequestError error = response.getError();
						if (error != null) {
							Toast.makeText(
									getApplicationContext()
											.getApplicationContext(),
									error.getErrorMessage(), Toast.LENGTH_SHORT)
									.show();
						
						} else {
							Toast.makeText(
									getApplicationContext()
											.getApplicationContext(), postId,
									Toast.LENGTH_LONG).show();
							
						}
					}
				}
			};

			Request request = new Request(session, "/photos", postParams,
					HttpMethod.POST, callback);

			RequestAsyncTask task = new RequestAsyncTask(request);
			task.execute();
			tx.setText("publish");
		}
	}
/*	@Override
	protected void onResume() {
	  super.onResume();

	  // Logs 'install' and 'app activate' App Events.
	  AppEventsLogger.activateApp(this);
	}
	@Override
	protected void onPause() {
	  super.onPause();

	  // Logs 'app deactivate' App Event.
	  AppEventsLogger.deactivateApp(this);
	}*/
	private boolean isSubsetOf(Collection<String> subset,
			Collection<String> superset) {
		for (String String : subset) {
			if (!superset.contains(String)) {
				return false;
			}
		}
		return true;
	}

	private class AcceptThread extends Thread {
		private BluetoothServerSocket mServerSocket;

		public AcceptThread() {
			try {
				mServerSocket = mBluetoothAdapter
						.listenUsingRfcommWithServiceRecord(
								"ClassicBluetoothServer", MY_UUID);
			} catch (IOException e) {
				Log.d(TAG, "get listend sock is error");
				final IOException ex = e;
				runOnUiThread(new Runnable() {
					public void run() {
						tx.setText(ex.getMessage());
					}
				});
			}
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				try {
					runOnUiThread(new Runnable() {
						public void run() {
							tx.setText(tx.getText()
									+ "\n\nWaiting for Bluetooth Client ...");
						}
					});
					Log.d(TAG, "ready to listen");
					socket = mServerSocket.accept(); // blocking call

				} catch (IOException e) {
					Log.v(TAG, e.getMessage());
					break;
				}
				Log.d(TAG, "socket is accepted");
				// If a connection was accepted
				if (socket != null) {
					// Do work in a separate thread
					new ConnectedThread(socket, mHandler).start();
					Log.d(TAG, "connectedThread is called");
					try {
						mServerSocket.close();
					} catch (IOException e) {
						Log.v(TAG, e.getMessage());
					}
					break;
				}
			}
		}
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				tx.setText("msg: " + msg.obj);
			}
		}
	};

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mSocket;
		private int bytesRead;
		private Handler h;
		private InputStream reader;
		private BufferedWriter writer;

		public ConnectedThread(BluetoothSocket socket, Handler h) {
			mSocket = socket;
			this.h = h;
			InputStream tmp = null;
			try {
				tmp = socket.getInputStream();
				// reader = new BufferedReader(new
				// InputStreamReader(socket.getInputStream()));
				// writer = new BufferedWriter(new
				// OutputStreamWriter(socket.getOutputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			reader = tmp;
		}

		public void run() {
			int bufferSize = 512;
			byte[] buffer = new byte[bufferSize];
			ByteArrayOutputStream receivedImage = new ByteArrayOutputStream();
			try {
				// BufferedInputStream BuffInputStream=new
				// BufferedInputStream(instream);
			//	ByteArrayOutputStream receivedImage = new ByteArrayOutputStream();
				bytesRead = -1;
				comment="";
				Log.d(TAG, "ready to receive");
				while ((bytesRead = reader.read(buffer)) > 0) {
					if (bytesRead == 1)
					{
						if(buffer[0]==1)
						{
							IsPicture=true;
						}
						else if(buffer[0]== 2)
						{
							IsPicture=false;
							break;
						}
					}
					else
					{
						if(IsPicture)
						{
							receivedImage.write(buffer, 0, bytesRead);
						}
						else
						{
							comment=comment+new String(buffer,0,bytesRead);
						}
					}
					Log.d(TAG, "bytesRead=" + bytesRead );
				}
				Log.d(TAG, "receive success");
			} catch (IOException e) {
				Log.d(TAG, "error to get img");
				e.printStackTrace();
			}
				final Bitmap bm = BitmapFactory.decodeByteArray(
						receivedImage.toByteArray(), 0,
						receivedImage.toByteArray().length);
				image=bm;
			//	final Bitmap b = bm.copy(Config.ARGB_8888, true);
				final String c = comment; 
				runOnUiThread(new Runnable() {
					public void run() {
						iv.setImageBitmap(bm);
						tx.setText(c.toString());
						Log.d(TAG, "set bitmap");
						publishStory();
					}
				});
				try {
					reader.close();
					// BuffInputStream.close();
					if(mSocket.isConnected())
						mSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
		}
	}
}

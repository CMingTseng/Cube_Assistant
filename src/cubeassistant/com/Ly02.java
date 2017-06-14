package cubeassistant.com;

import cubeassistant.com.ColorDecoder;
import cubeassistant.com.Preview;
import cubeassistant.com.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

public class Ly02 extends Activity {

	byte[] frontFace;//紀錄現在拍的是哪面
	byte[] backFace;
	byte[] leftFace;
	byte[] rightFace;
	byte[] upFace;
	byte[] downFace;

	public ColorDecoder decoder;//宣告顏色辨識(ColorDecoder.class)
	Camera mCamera = null;//宣告相機功能
	private PowerManager.WakeLock wl;//對Android設備的電源進行管理
	private static final int DIALOG_NO_CAMERA = 0;//設CAMERA旗標 判斷相機狀態
	private static final String TAG = "Main";//

	ImageButton buttonClick;
	Preview preview;//相機預覽
	Guides mGuides;//相機九宮格
	int pictureState = 0;//設相片旗標 判斷相片狀態

	String face;
	Intent intent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ly02);

		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");//保持CPU 運轉，保持屏幕高亮顯示，鍵盤燈也保持亮度

		preview = new Preview(this);
		// decoder = new ColorDecoder(getCacheDir().getAbsolutePath());

		Bundle b = this.getIntent().getExtras();
		face = getIntent().getStringExtra("face");

		loadFaces(b);
		if (face.equals("FRONT")) {// getString(R.string.front1)
			decoder = new ColorDecoder(getCacheDir().getAbsolutePath());
		} else {
			decoder = b.getParcelable("DECODER");
			if (decoder == null)
				intent = new Intent(Ly02.this, Ly01.class);
		}

		preview = new Preview(this);

		getCamera();

		((FrameLayout) findViewById(R.id.preview)).addView(preview);//實作相機預覽

		mGuides = new Guides(this, face);//傳值face給相機九宮格
		((RelativeLayout) findViewById(R.id.guides)).addView(mGuides);//實作相機九宮格
		mGuides.getLayoutParams().width = LayoutParams.FILL_PARENT;//設定相機九宮格
		mGuides.getLayoutParams().height = LayoutParams.FILL_PARENT;////實作相機九宮格相機九宮格

		buttonClick = (ImageButton) findViewById(R.id.media);
		buttonClick.setOnClickListener(btn);
//相機圖案隨拍照轉
		Animation rotateAnim = AnimationUtils.loadAnimation(this,
				R.anim.rotation);
		LayoutAnimationController animController = new LayoutAnimationController(
				rotateAnim, 0);
		RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
		layout.setLayoutAnimation(animController);

	}

	private ImageButton.OnClickListener btn = new ImageButton.OnClickListener() {
		public void onClick(View v) {
			if (pictureState == 2) {
				pictureState = 0;
				mCamera.startPreview();
			} else if (pictureState == 0) {
				pictureState = 1;

				/*
				 * mCamera.takePicture(shutterCallback, rawCallback,
				 * jpegCallback);
				 */

				mCamera.autoFocus(new AutoFocusCallback() {

					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						camera.takePicture(shutterCallback, rawCallback,
								jpegCallback);
					}
				});

			}// if

		}// onclick
	};
//判斷現在拍的是哪面
	private void loadFaces(Bundle b) {
		frontFace = b.getByteArray("FRONT");// getString(R.string.front1)
		backFace = b.getByteArray("BACK");
		leftFace = b.getByteArray("LEFT");
		rightFace = b.getByteArray("RIGHT");
		upFace = b.getByteArray("UP");
		downFace = b.getByteArray("DOWN");
	}
//傳值現在拍的是哪面
	private Intent makeIntent(Class<?> cls, Bundle b) {
		Intent i = new Intent(this, cls);
		b.putParcelable("DECODER", decoder);
		b.putByteArray("FRONT", frontFace);// getString(R.string.front1)
		b.putByteArray("BACK", backFace);
		b.putByteArray("LEFT", leftFace);
		b.putByteArray("RIGHT", rightFace);
		b.putByteArray("UP", upFace);
		b.putByteArray("DOWN", downFace);
		i.putExtras(b);
		return i;
	}

	// 解決橫豎屏切換時重新載入的問題
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "Orient change------");
		// rotateControls();
	}

	@Override
	public void onPause() {
		super.onPause();
		releaseCamera();
		wl.release();
	}

	@Override
	public void onResume() {
		super.onResume();
		getCamera();
		wl.acquire();
	}

	private void getCamera() {
		try {
			if (mCamera == null) {
				mCamera = Camera.open();
				if (mCamera == null)
					showDialog(DIALOG_NO_CAMERA);
				else
					preview.setCamera(mCamera);
			}
		} catch (Exception ex) {
			showDialog(DIALOG_NO_CAMERA);
		}
		//
	}

	private void releaseCamera() {
		if (mCamera != null) {
			preview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
	}
//相機快門聲
	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			Log.d(TAG, "onShutter'd");
		}
	};

	/** Handles data for raw picture */
	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "onPictureTaken - raw");
		}
	};

	/** Handles data for jpeg picture */
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Bitmap picture = BitmapFactory
					.decodeByteArray(data, 0, data.length);
			new DecodeImageTask().execute(picture);
			// picture.getHeight();
			/*
			 * FileOutputStream outStream = null; try { // write to local
			 * sandbox file system // outStream = //
			 * CameraDemo.this.openFileOutput(String.format("%d.jpg", //
			 * System.currentTimeMillis()), 0); // Or write to sdcard outStream
			 * = new FileOutputStream(String.format( "/sdcard/rubik/%d.jpg",
			 * System.currentTimeMillis())); outStream.write(data);
			 * outStream.close(); Log.d(TAG, "onPictureTaken - wrote bytes: " +
			 * data.length); } catch (FileNotFoundException e) {
			 * e.printStackTrace(); } catch (IOException e) {
			 * e.printStackTrace(); } finally { }
			 */
			Log.d(TAG,
					String.format("onPictureTaken - jpeg - %d",
							picture.getHeight()));
			pictureState = 2;
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_CAMERA)
				|| (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
			if (pictureState == 2) {
				pictureState = 0;
				mCamera.startPreview();
			} else if (pictureState == 0) {
				pictureState = 1;
				mCamera.autoFocus(new AutoFocusCallback() {

					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						camera.takePicture(null, null, jpegCallback);
					}
				});
			}

		}
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			Intent inten = new Intent(this, Ly01.class);
			inten.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(inten);
		}
		return super.onKeyDown(keyCode, event);
	}
//相片進入colorDecoder.class做顏色辨識處理
	private class DecodeImageTask extends AsyncTask<Bitmap, Void, byte[]> {
		ProgressDialog pd;

		@Override
		protected byte[] doInBackground(Bitmap... bitmap) {
			Log.d(TAG,
					String.format("Async - jpeg - %d", bitmap[0].getHeight()));
			Bitmap img = bitmap[0];

			if (img.getWidth() > 600) {
				int newWidth = 600;
				int newHeight = 360;
				float scaleWidth = ((float) newWidth) / img.getWidth();
				float scaleHeight = ((float) newHeight) / img.getHeight();
				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				img = Bitmap.createBitmap(img, 0, 0, img.getWidth(),
						img.getHeight(), matrix, true);
			}
			return decoder.decode(img);

			// return new byte[]{-1,-1,-1,-1,-1,-1,-1,-1,-1};
		}

		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(Ly02.this, "", "Reading cube...", true);

		}

		@Override
		protected void onPostExecute(byte[] result) {
			pd.dismiss();
			// Toast.makeText(getApplicationContext(),
			// "AA",Toast.LENGTH_LONG).show();
			/*
			 * String text =
			 * String.format("RESULTS : %d %d %d | %d %d %d | %d %d %d",
			 * result.get(0), result.get(1), result.get(2), result.get(3),
			 * result.get(4), result.get(5), result.get(6), result.get(5),
			 * result.get(8));
			 */
			Bundle b = new Bundle();
			// Toast.makeText(getApplicationContext(),
			// "BB",Toast.LENGTH_LONG).show();
			b.putByteArray("SCAN_RESULTS", result);
			b.putString("SCAN_FACE", face);
			Intent i = makeIntent(Ly03.class, b);
			i.putExtras(b);
			startActivity(i);
			String str = getString(R.string.remind);
			Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG)
					.show();
			// Ly02.this.finish();
			// Toast.makeText(LoadCube.this, text, Toast.LENGTH_LONG);
			// Log.d(TAG, text);
		}
	}

}

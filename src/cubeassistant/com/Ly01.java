package cubeassistant.com;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.markupartist.android.widget.ActionBar;

import android.net.Uri;
import android.os.Bundle;
//import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

//import com.droidtools.rubiksolver.LoadCube;
//import com.markupartist.android.widget.actionbar.*;

public class Ly01 extends Activity implements ViewFactory, OnTouchListener {

	private static final int DIALOG_HISTORY = 1;
	Dialog dialog;
	private ImageView ly01iv01, ly01iv02, ly01iv03, ly01iv04;
	private ImageSwitcher ly01is01;
	//欲放到Switcher內的圖片，先寫入array
	private int[] arrayPictures = { R.drawable.solution0,
			R.drawable.solution1s1, R.drawable.solution1, R.drawable.solution2,
			R.drawable.solution3, R.drawable.solution4, R.drawable.solution5,
			R.drawable.solution6 };
	private int pictureIndex; // 顯示的圖片在數組中得index
	private float touchDownX; // 左右滑手指按下的X座標
	private float touchUpX;// 左右滑手指鬆開的X座標

	java.util.ArrayList<ByteBuffer> historyDataSolution;
	java.util.ArrayList<ByteBuffer> historyDataCubeState;
	java.util.ArrayList<ByteBuffer> historyDataColors;
	java.util.ArrayList<Integer> historyId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(cubeassistant.com.R.layout.ly01);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);// 直立畫面

		setupComponent();
		ly01is01.setFactory(this);
		ly01is01.setOnTouchListener(this);
	}

	private void setupComponent() {
		// TODO Auto-generated method stub
		ly01iv01 = (ImageView) findViewById(R.id.ly01iv01);
		ly01iv02 = (ImageView) findViewById(R.id.ly01iv02);
		ly01iv03 = (ImageView) findViewById(R.id.ly01iv03);
		ly01iv04 = (ImageView) findViewById(R.id.ly01iv04);
		ly01is01 = (ImageSwitcher) findViewById(R.id.ly01is01);

		ly01iv02.setOnClickListener(toLy04);
		ly01iv03.setOnClickListener(toLy03);
		ly01iv04.setOnClickListener(About);

	}
	//歷史紀錄Button
	private OnClickListener toLy04 = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// Intent intent=new Intent(Ly01.this,Ly04.class);
			// startActivity(intent);
			// Ly01.this.finish();
			showDialog(DIALOG_HISTORY);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
		case DIALOG_HISTORY:
			// 歷史紀錄彈跳視窗
			dialog = makeHistoryDialog();
			break;

		default:
			dialog = null;
		}
		return dialog;
	}
	//歷史紀錄的Dialog
	private Dialog makeHistoryDialog() {
		//取得歷史紀錄清單
		java.util.ArrayList<String> histories = new java.util.ArrayList<String>();
		historyDataSolution = new java.util.ArrayList<ByteBuffer>();
		historyDataCubeState = new java.util.ArrayList<ByteBuffer>();
		historyDataColors = new java.util.ArrayList<ByteBuffer>();
		historyId = new java.util.ArrayList<Integer>();
		
		Cursor cursor = getContentResolver().query(HistoryProvider.CONTENT_URI,
				HistoryProvider.PROJECTION, null, null,
				HistoryProvider.DEFAULT_SORT_ORDER);

		if (cursor != null && cursor.getCount() != 0) {
			cursor.moveToFirst();
			do {
				historyId.add(cursor.getInt(cursor
						.getColumnIndexOrThrow(HistoryProvider.ID)));
				histories.add(cursor.getString(cursor
						.getColumnIndexOrThrow(HistoryProvider.NAME)));
				historyDataSolution.add(ByteBuffer.wrap(cursor.getBlob(cursor
						.getColumnIndexOrThrow(HistoryProvider.MOVES))));
				historyDataCubeState.add(ByteBuffer.wrap(cursor.getBlob(cursor
						.getColumnIndexOrThrow(HistoryProvider.STATE))));
				historyDataColors.add(ByteBuffer.wrap(cursor.getBlob(cursor
						.getColumnIndexOrThrow(HistoryProvider.COLORS))));
			} while (cursor.moveToNext());

		}
		String[] items = new String[histories.size()];
		histories.toArray(items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.history));
		cursor.close();
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@SuppressWarnings("unchecked")
			public void onClick(DialogInterface dialog, int item) {
				ArrayList<RubikMove> result = null;
				Bundle b = new Bundle();
				byte[] data = historyDataSolution.get(item).array();
				ByteArrayInputStream bais = new ByteArrayInputStream(data);

				try {
					ObjectInputStream ois = new ObjectInputStream(bais);
					result = (ArrayList<RubikMove>) ois.readObject();
					ois.close();
					bais.close();
				} catch (OptionalDataException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				b.putParcelableArrayList("SOLUTION", result);
				// b.putParcelableArrayList("SOLUTION",
				// RubikMove.fromNative(RubikCube.nativeSolve(RubikCube.getCubeState(historyDataCubeState.get(item).array()).nativeStringState())));
				b.putByteArray("CUBE_STATE", historyDataCubeState.get(item)
						.array());
				b.putByteArray("COLORS", historyDataColors.get(item).array());
				b.putString("HID", Integer.toString(historyId.get(item)));
				Intent inten = new Intent(Ly01.this, Ly04.class);
				inten.putExtras(b);
				startActivity(inten);
			}
		});
		return builder.create();
	}
	//進入相機功能
	private OnClickListener toLy03 = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent inten = new Intent(v.getContext(), Ly02.class);
			inten.putExtra("face", "FRONT");// getString(R.string.front1)
			startActivity(inten);
		}
	};
   //當滑動Switcher時的觸發事件
	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		// TODO Auto-generated method stub
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			touchDownX = event.getX();// 取得左右滑動手指按下的X座標
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP) {

			touchUpX = event.getX();// 取得左右滑動時手指鬆開的X座標

			if (touchUpX - touchDownX > 100) { // 左往右，看前一張

				pictureIndex = pictureIndex == 0 ? arrayPictures.length - 1 // 取得當前要看的圖片的index
						: pictureIndex - 1;

				ly01is01.setInAnimation(AnimationUtils.loadAnimation(this,
						android.R.anim.slide_in_left));
				ly01is01.setOutAnimation(AnimationUtils.loadAnimation(this,
						android.R.anim.slide_out_right));

				ly01is01.setImageResource(arrayPictures[pictureIndex]); // 設置當前要看的圖片

			} else if (touchDownX - touchUpX > 100) { // 右往左，看下一張

				if (pictureIndex == arrayPictures.length - 1) {// 取得當前要看的圖片的index
					pictureIndex = 0;
				} else {
					pictureIndex = pictureIndex + 1;
				}

				// pictureIndex = pictureIndex == arrayPictures.length - 1 ? 0
				// : pictureIndex + 1;

				ly01is01.setInAnimation(AnimationUtils.loadAnimation(this,
						R.anim.slide_in_right)); // android預設未定義的動畫，自定義
				ly01is01.setOutAnimation(AnimationUtils.loadAnimation(this,
						R.anim.slide_out_left)); // android預設未定義的動畫，自定義

				ly01is01.setImageResource(arrayPictures[pictureIndex]); // 設置當前要看的圖片
			}
			return true;
		}
		return false;
	}
	//將圖片array放到ImageView裡(Switcher)
	@Override
	public View makeView() {

		// TODO Auto-generated method stub
		ImageView imageView = new ImageView(this);
		imageView.setImageResource(arrayPictures[pictureIndex]);
		return imageView;
	}
	//關於我的Button監聽及觸發事件
	private OnClickListener About = new OnClickListener() {

		@Override
		public void onClick(View v) {

			// TODO Auto-generated method stub
			dialog = new Dialog(Ly01.this);

			dialog.setCancelable(true);
			dialog.setContentView(R.layout.about);
			dialog.setTitle(R.string.AboutTitle);
			
			Button btngoHome = (Button) dialog.findViewById(R.id.ly03btngoHome);
			btngoHome.setOnClickListener(goHome);
			
			TextView  aboutV12 = (TextView) dialog.findViewById(R.id.aboutV12);
			aboutV12.setOnClickListener(sendmail);
			
			TextView  aboutV21 = (TextView) dialog.findViewById(R.id.aboutV21);
			aboutV21.setOnClickListener(openN);
			
			TextView  aboutV31 = (TextView) dialog.findViewById(R.id.aboutV31);
			aboutV31.setOnClickListener(openA);
			
			TextView  aboutV41 = (TextView) dialog.findViewById(R.id.aboutV41);
			aboutV41.setOnClickListener(openS);
		
			dialog.show();

			return;
		}
	};
	//關於我的Dialog內的寄信給原作者Button監聽及觸發
	public OnClickListener sendmail = new OnClickListener() {
		public void onClick(View v) {
			Uri uri = Uri.parse("mailto:droidtools@gmail.com");
			Intent it = new Intent(Intent.ACTION_SENDTO, uri);
			startActivity(it);
		}
	};
	//關於我的Dialog內的寄信給參考來源Button監聽及觸發
	public OnClickListener openN = new OnClickListener() {
		public void onClick(View v) {
			Uri uri = Uri.parse(getString(R.string.aboutV21));
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(it);
		}
	};
	//關於我的Dialog內的寄信給參考來源Button監聽及觸發
	public OnClickListener openA = new OnClickListener() {
		public void onClick(View v) {
			Uri uri = Uri.parse(getString(R.string.aboutV31));
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(it);
		}
	};
	public OnClickListener openS = new OnClickListener() {
		public void onClick(View v) {
			Uri uri = Uri.parse(getString(R.string.aboutV41));
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(it);
		}
	};
	
	
	//回首頁的Button
	public OnClickListener goHome = new OnClickListener() {
		public void onClick(View v) {
			dialog.cancel();//取消Dialog視窗
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(Ly01.this);
			dialog.setTitle(getString(R.string.makesure));
			dialog.setPositiveButton(getString(R.string.Sure),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							finish();

						}
					});
			dialog.setNegativeButton(getString(R.string.No),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							dialog.cancel();
						}
					});
			dialog.show();

		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
}

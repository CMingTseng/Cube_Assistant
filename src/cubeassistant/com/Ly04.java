package cubeassistant.com;

import java.util.ArrayList;
import cubeassistant.com.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

public class Ly04 extends Activity {
	GridView mSolution;
	Button stepSolutionButton,delsolutionButton;
	ArrayList<RubikMove> sol;
	String hid;
	Bundle bundle;
	byte[] cubeState;
	byte[] colorArray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.ly04);

		if (savedInstanceState != null)
			bundle = savedInstanceState;
		else
			bundle = this.getIntent().getExtras();
		sol = bundle.getParcelableArrayList("SOLUTION");
		cubeState = bundle.getByteArray("CUBE_STATE");
		colorArray = bundle.getByteArray("COLORS");
		hid = bundle.getString("HID");
		//解答步驟的GridView
		mSolution = (GridView)(findViewById(R.id.ly04gv01));//從R資源中取得介面元件
		mSolution.setAdapter(new SolutionAdapter(sol));//設置Adapter
		//監聽Adapter的item
		mSolution
		.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			//Adapter中的item被觸發的事件
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				((SolutionAdapter) mSolution.getAdapter()).mData.get(position).setDone();
				((SolutionAdapter) mSolution.getAdapter()).notifyDataSetChanged();
			}

		});
		
		
		//宣告回首頁的Button，與觸發功能
		stepSolutionButton = (Button) findViewById(R.id.ly04bt01);
		stepSolutionButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				goHome();
			}
		});
		//宣告刪除資料的Button，與觸發功能
		delsolutionButton = (Button)findViewById(R.id.ly04bt02);
		delsolutionButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				delsolution();//刪除本筆記錄
				goHome();//返回首頁
			}
		});
		
		setTitle(String.format("解題步驟  -共 %d 步", sol.size()));
	}
	//回首頁的method
	public void goHome(){
		Intent intent = new Intent(Ly04.this, Ly01.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
	//刪除本筆記錄
	public void delsolution(){
		String where = String.format("%s='%s'", HistoryProvider.ID, hid);//取得單筆記錄
		getContentResolver().delete(HistoryProvider.CONTENT_URI, where, null);//刪除單筆記錄
	}
	 //產生OptionsMenu(Menu)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();//利用inflater指令找到R資源中的XML
		inflater.inflate(R.menu.solution, menu);//並實體化
		return true;
	}
	//建立Menu選單的內容
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		
		case R.id.goHome:
			goHome();//回首頁
			return true;
			
		case R.id.deleteSol:
			delsolution();//刪除本筆記錄
			goHome();//返回首頁
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle out) {
		out.putParcelableArrayList("SOLUTION", sol);
		out.putByteArray("CUBE_STATE", cubeState);
		out.putByteArray("COLORS", colorArray);
		out.putString("HID", hid);
	}
	
	
	//返回鍵
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
//	    	Intent inten = new Intent(this, Ly01.class);
//	    	inten.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(inten);
	    	finish();
	    }
	    return super.onKeyDown(keyCode, event);
	}

}
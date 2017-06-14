package cubeassistant.com;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cubeassistant.com.R.color;

//import com.droidtools.rubiksolver.CheckFace;
//import com.droidtools.rubiksolver.R;
//import com.droidtools.rubiksolver.CheckFace.SelectColorOnClickListener;
//import com.droidtools.rubiksolver.CheckFace;
//import com.droidtools.rubiksolver.HistoryProvider;
//import com.droidtools.rubiksolver.RubikCube;
//import com.droidtools.rubiksolver.Solution;
//import com.droidtools.rubiksolver.CheckFace.GetSolution;

public class Ly03 extends Activity {

    ListView mColorsList;

    GridView mColorsGrid;

    Button mNextFace;

    Button mTryAgain;

    Button mly03btngoHome;

    String scanFace;

    int changeFacelet;

    ColorDecoder decoder;

    byte[] frontFace;

    byte[] backFace;

    byte[] leftFace;

    byte[] rightFace;

    byte[] upFace;

    byte[] downFace;

	/*
     * List<Byte> color1;
	 * 
	 * List<Byte> color2;
	 * 
	 * List<Byte> color3;
	 * 
	 * List<Byte> color4;
	 * 
	 * List<Byte> color5;
	 * 
	 * List<Byte> color6;
	 */

    List<List<Byte>> colorFix;

    List<Byte> ungroupedColors;//用不到

    ColorsAdapter ungroupAdapter;//用不到

    ColorsAdapter groupAdapter;//用不到

    Button nextSimilarButton;//用不到

    TextView similarText, mly03tv02;

    int colorPage;

    private static final int DIALOG_CHECK = 0;

    private static final int DIALOG_INVALID_CUBE = 1;//提醒顏色種類小於六 煩請重拍

    private static final int DIALOG_MORE_COLORS = 2;//用不到

    private static final int DIALOG_FIX_COLORS = 3;//提醒顏色種類大於六 煩請重拍

    private static final int DIALOG_CHECK_COLORS = 4;//用不到

    java.util.ArrayList<ByteBuffer> historyDataSolution;
    java.util.ArrayList<ByteBuffer> historyDataCubeState;
    java.util.ArrayList<ByteBuffer> historyDataColors;
    java.util.ArrayList<Integer> historyId;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // if (LoadCube.decoder == null) {

        // LoadCube.goHome(this);

        // }

        setContentView(R.layout.ly03);

        mColorsList = (ListView) findViewById(R.id.colorsList);

        mColorsGrid = (GridView) findViewById(R.id.colorsGrid);

        mly03btngoHome = (Button) findViewById(R.id.ly03btngoHome);

        mly03tv02 = (TextView) findViewById(R.id.ly03tv02);

        mNextFace = (Button) findViewById(R.id.nextFace);

        mTryAgain = (Button) findViewById(R.id.tryAgain);

        // Ly01.makeActionBar(this);//????????????????

        Bundle b = this.getIntent().getExtras();

        decoder = b.getParcelable("DECODER");

        // String ss=decoder.toString();

        // Toast.makeText(Ly03.this, ss, Toast.LENGTH_LONG).show();

        // Log.d(TAG, text);

        // if (decoder == null)

        // //LoadCube.goHome(this);

        loadFaces(b);//拿目前所在是哪面

        scanFace = b.getString("SCAN_FACE");//把傳來的放這
        //底下做換面 "DOWN"就開始解題  "FRONT"-->"RIGHT"-->"BACK".....
        if (scanFace.equals("DOWN"))

            mNextFace.setText(R.string.solveCube);// 去要抓STRNG才對

        else

            mNextFace.setText(R.string.nextFace);

        if (scanFace.equals("FRONT")) {
            mly03tv02.setText(getString(R.string.NowPageF));
            mly03tv02.setTextColor(color.red);
        } else if (scanFace.equals("RIGHT")) {
            mly03tv02.setText(getString(R.string.NowPageR));
            mly03tv02.setTextColor(color.red);

        } else if (scanFace.equals("BACK")) {

            mly03tv02.setText(getString(R.string.NowPageB));
            mly03tv02.setTextColor(color.red);

        } else if (scanFace.equals("LEFT")) {
            mly03tv02.setText(getString(R.string.NowPageL));
            mly03tv02.setTextColor(color.red);

        } else if (scanFace.equals("UP")) {
            mly03tv02.setText(getString(R.string.NowPageU));
            mly03tv02.setTextColor(color.red);
        } else {
            mly03tv02.setText("");
        }

        mColorsList.setAdapter(new ColorsAdapter(decoder));//把顏色辨識完的放這

        mColorsGrid.setAdapter(new FaceAdapter(b.getByteArray("SCAN_RESULTS"),
                decoder));//把顏色辨識完的放這

        mColorsGrid
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,

                                            int position, long id) {

                        changeFacelet = position;

                        showDialog(DIALOG_CHECK);//修改顏色功能

                    }

                });

        mNextFace.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                nextFace();

            }

        });

        mTryAgain.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                tryAgain();

            }

        });

        mly03btngoHome.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                Intent it = new Intent(Ly03.this, Ly01.class);
                it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(it);
            }

        });

        setDb();

    }

    // @Override
    // protected void onResume() {
    // super.onResume();
    // if (decoder == null || decoder.colorSize() == 0) {
    // LoadCube.goHome(this);
    // }
    // }
    public void setDb() {
        java.util.ArrayList<String> histories = new java.util.ArrayList<String>();
        historyDataSolution = new java.util.ArrayList<ByteBuffer>();
        historyDataCubeState = new java.util.ArrayList<ByteBuffer>();
        historyDataColors = new java.util.ArrayList<ByteBuffer>();
        historyId = new java.util.ArrayList<Integer>();
        Cursor cursor = getContentResolver().query(HistoryProvider.CONTENT_URI,
                HistoryProvider.PROJECTION, null, null,
                HistoryProvider.DEFAULT_SORT_ORDER);
    }

    //清掉上一面拍的顏色辨識
    void tryAgain() {

        for (byte i = decoder.firstNewCol; decoder.hasId(i); i++) {

            decoder.removeColor(i);

        }

        decoder.nextId = (byte) (decoder.firstNewCol - 1);

        // decoder.clear();

        Intent inten = makeIntent(Ly02.class, new Bundle());

        inten.putExtra("face", scanFace);

        startActivity(inten);

    }

    private List<Byte> toByteList(byte[] values) {

        // byte[] ret = new byte[list.size()];

        List<Byte> ret = new ArrayList<Byte>();

        for (int i = 0; i < values.length; i++) {

            ret.add(values[i]);

        }

        return ret;

    }

    void nextFace() {

		/*
		 * if (true) {
		 * 
		 * new GetSolution().execute(frontFace, backFace, leftFace, rightFace,
		 * upFace, downFace);
		 * 
		 * return;
		 * 
		 * }
		 */
        //把顏色辨識結果放這
        byte[] finalFace = ((FaceAdapter) mColorsGrid.getAdapter()).mData;

        // TODO(bbrown): This doesn't work because it makes a copy of the data,
        // but I believe the

        // fix colors dialog relies on the reference to the data so it can
        // manipulate it. Crazy!

        // byte[] finalFace = ((FaceAdapter)
        // mColorsGrid.getAdapter()).getData();

        String face = null;

        Set<Byte> usedColors = new HashSet<Byte>();

        if (scanFace.equals("FRONT")) {// getString(R.string.front1)

            frontFace = finalFace;

            face = "RIGHT";

        } else if (scanFace.equals("RIGHT")) {

            rightFace = finalFace;

            face = "BACK";

        } else if (scanFace.equals("BACK")) {

            backFace = finalFace;

            face = "LEFT";

        } else if (scanFace.equals("LEFT")) {

            leftFace = finalFace;

            face = "UP";

        } else if (scanFace.equals("UP")) {

            upFace = finalFace;

            face = "DOWN";

        } else if (scanFace.equals("DOWN")) {

            downFace = finalFace;

            face = null;

        }

        if (frontFace != null)

            usedColors.addAll(toByteList(frontFace));

        if (backFace != null)

            usedColors.addAll(toByteList(backFace));

        if (leftFace != null)

            usedColors.addAll(toByteList(leftFace));

        if (rightFace != null)

            usedColors.addAll(toByteList(rightFace));

        if (upFace != null)

            usedColors.addAll(toByteList(upFace));

        if (downFace != null)

            usedColors.addAll(toByteList(downFace));

		/*
		 * for (int i = decoder.colorSize() - 1; i >= 0; i--) {
		 * 
		 * if (!usedColors.contains(i)) {
		 * 
		 * decoder.removeColor(i);
		 * 
		 * }
		 * 
		 * }
		 */

        // for (Map.Entry<Integer, Parcelable[]> entry : decoder.entrySet()) {

        decoder.removeUnusedColors(usedColors);

        ((ColorsAdapter) mColorsList.getAdapter()).notifyDataSetChanged();//用不到

        ((FaceAdapter) mColorsGrid.getAdapter()).notifyDataSetChanged();//用不到

        // showDialog(DIALOG_FIX_COLORS);

        if (scanFace.equals("DOWN")) {

            if (decoder.colorSize() == 6) {

                // Disconnect the views from the adapters because they are
                // backed by the decoder

                // which will be cleared during the solution calculation.

                mColorsList.setAdapter(null);

                mColorsGrid.setAdapter(null);

                new GetSolution().execute(frontFace, backFace, leftFace,
                        rightFace, upFace, downFace);

            } else if (decoder.colorSize() > 6) {
                android.util.Log.d(
                        "SOLVER",
                        String.format("Invalid colors size %d",
                                decoder.colorSize()));
                showDialog(DIALOG_FIX_COLORS);

            } else {

                android.util.Log.d(
                        "SOLVER",
                        String.format("Invalid colors size %d",
                                decoder.colorSize()));

                showDialog(DIALOG_INVALID_CUBE);

            }

        } else {

            Intent inten = makeIntent(Ly02.class, new Bundle());

            inten.putExtra("face", face);

            startActivity(inten);

        }

    }

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

    private class GetSolution extends

            AsyncTask<byte[], Void, ArrayList<RubikMove>> {

        ProgressDialog pd;

        String historyId;

        byte[] cubeState;

        byte[] colorArray;

        @Override
        protected ArrayList<RubikMove> doInBackground(byte[]... faces) {

            RubikCube cube = new RubikCube();

            // RubikCube cube2 = new RubikCube();

            // RubikCube cube3 = new RubikCube();

            cube.cube.get("FRONT").setValues(faces[0]);// getString(R.string.front1)

            cube.cube.get("BACK").setValues(faces[1]);

            cube.cube.get("LEFT").setValues(faces[2]);

            cube.cube.get("RIGHT").setValues(faces[3]);

            cube.cube.get("UP").setValues(faces[4]);

            cube.cube.get("DOWN").setValues(faces[5]);

			/*
			 * 
			 * cube2.cube.get("FRONT").setValues(faces[0]);
			 * 
			 * cube2.cube.get("BACK").setValues(faces[1]);
			 * 
			 * cube2.cube.get("LEFT").setValues(faces[2]);
			 * 
			 * cube2.cube.get("RIGHT").setValues(faces[3]);
			 * 
			 * cube2.cube.get("UP").setValues(faces[4]);
			 * 
			 * cube2.cube.get("DOWN").setValues(faces[5]);
			 */

			/*
			 * android.util.Log.d("CUBE", cube.cube.get("FRONT").toString());
			 * 
			 * android.util.Log.d("CUBE", cube.cube.get("BACK").toString());
			 * 
			 * android.util.Log.d("CUBE", cube.cube.get("LEFT").toString());
			 * 
			 * android.util.Log.d("CUBE", cube.cube.get("RIGHT").toString());
			 * 
			 * android.util.Log.d("CUBE", cube.cube.get("UP").toString());
			 * 
			 * android.util.Log.d("CUBE", cube.cube.get("DOWN").toString());
			 */

            cubeState = cube.getCubeState().toByteArray();

            colorArray = decoder.colorArray();

            decoder.clear();

			/*
			 * 
			 * 
			 * 
			 * if (!cube.solveCube(CheckFace.this)) {
			 * 
			 * 
			 * 
			 * return null;
			 * 
			 * }
			 * 
			 * ArrayList<RubikMove> optMoveList =
			 * RubikCube.optomizeSolution(RubikCube
			 * .cubeStateOptimization(cube.cubeList, cube.moveList));
			 */

            String rc = cube.nativeSolve();

            if (rc.startsWith("Error:")) {

                return null;

            }

            ArrayList<RubikMove> optMoveList = RubikMove.fromNative(rc);

			/*
			 * if (cube2.executeMoves(optMoveList)) {
			 * 
			 * cube2.writeCubeDebug(CheckFace.this);
			 * 
			 * android.util.Log.d("SOLVE", "SOLVED CUBE SUPER OPT - " +
			 * optMoveList.size());
			 * 
			 * } else {
			 * 
			 * android.util.Log.d("SOLVE", "FAILED TO SOLVE CUBE SUPER OPT - " +
			 * optMoveList.size());
			 * 
			 * }
			 */

			/*
			 * 
			 * ArrayList<RubikMove> rms =
			 * RubikCube.optomizeSolution(cube.moveList);
			 * 
			 * if (cube3.executeMoves(rms)) {
			 * 
			 * android.util.Log.d("SOLVE", "SOLVED CUBE OPTIMIZED - " +
			 * rms.size());
			 * 
			 * } else {
			 * 
			 * android.util.Log.d("SOLVE", "FAILED TO SOLVE CUBE OPTIMIZED - " +
			 * rms.size());
			 * 
			 * }
			 */
//宣告用ByteArrayOutputStream傳值到SQLite
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // ArrayList<RubikMove> sol = new ArrayList<RubikMove>();

            try {

                ObjectOutputStream oos = new ObjectOutputStream(baos);

                // oos.writeObject(cube.moveList);

                oos.writeObject(optMoveList);

                oos.close();

                ContentValues cv = new ContentValues();

                cv.put(HistoryProvider.NAME, DateFormat.getDateTimeInstance()
                        .format(new Date(System

                                .currentTimeMillis())));

                cv.put(HistoryProvider.MOVES, baos.toByteArray());

                cv.put(HistoryProvider.STATE, cubeState);

                cv.put(HistoryProvider.COLORS, colorArray);

                Uri result = Ly03.this.getContentResolver().insert(
                        HistoryProvider.CONTENT_URI, cv);

                historyId = result.getPathSegments().get(1);

                baos.close();

            } catch (IOException e) {

                android.util.Log.e("CheckFace", e.getMessage());

            }

            Cursor cursor = getContentResolver().query(
                    HistoryProvider.CONTENT_URI,

                    HistoryProvider.PROJECTION, null, null,

                    HistoryProvider.DEFAULT_SORT_ORDER);

            if (cursor.getCount() >= 6) {

                Toast.makeText(getApplicationContext(), "66666",
                        Toast.LENGTH_LONG).show();
                cursor.moveToFirst();

                String where = String.format("%s='%s'", HistoryProvider.ID,
                        cursor.getInt(cursor

                                .getColumnIndexOrThrow(HistoryProvider.ID)));

                getContentResolver().delete(HistoryProvider.CONTENT_URI, where,
                        null);

            }

            cursor.close();

            return optMoveList;

        }

        @Override
        protected void onPreExecute() {

            pd = ProgressDialog.show(Ly03.this, "", "Solving cube...",

                    true);

        }

        @Override
        protected void onPostExecute(ArrayList<RubikMove> result) {

            pd.dismiss();

            if (result == null) {

                showDialog(DIALOG_INVALID_CUBE);

            } else {

                Bundle b = new Bundle();

                b.putParcelableArrayList("SOLUTION", result);

                b.putByteArray("CUBE_STATE", cubeState);

                b.putByteArray("COLORS", colorArray);

                b.putString("HID", historyId);

                Intent inten = new Intent(Ly03.this, Ly04.class);

                inten.putExtras(b);

                startActivity(inten);
                finish();

            }

        }

    }

    Dialog editFaceDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // builder.setTitle("Colors");

        // builder.setAdapter(arg0, arg1)

        SelectColorAdapter selectColorAdapter = new SelectColorAdapter(decoder);

        builder.setAdapter(selectColorAdapter, new SelectColorOnClickListener(
                selectColorAdapter) {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                ((FaceAdapter) mColorsGrid.getAdapter()).setItem(

                        changeFacelet, (Byte) mAdapter.getItem(item));

                mColorsGrid.invalidate();

                dialog.dismiss();

                // mAdapter = null;

                // builder.

            }

        });

        AlertDialog alert = builder.create();

        return alert;

    }

    Dialog invalidCubeLessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.invalidCube)
                .setMessage(R.string.invalidCubeLess)
                .setCancelable(false)
                .setNeutralButton(R.string.goHome,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Toast.makeText(getApplicationContext(),
                                // "invalidCubeDialog", Toast.LENGTH_LONG)
                                // .show();
                                Intent intent = new Intent(Ly03.this,
                                        Ly01.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);

                                // LoadCube.goHome(CheckFace.this);
                            }
                        });
        AlertDialog alert = builder.create();
        return alert;
    }

    Dialog invalidCubeMoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.invalidCube)
                .setMessage(R.string.invalidCubenMore)
                .setCancelable(false)
                .setNeutralButton(R.string.goHome,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Toast.makeText(getApplicationContext(),
                                // "invalidCubeDialog", Toast.LENGTH_LONG)
                                // .show();
                                Intent intent = new Intent(Ly03.this,
                                        Ly01.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);

                                // LoadCube.goHome(CheckFace.this);
                            }
                        });
        AlertDialog alert = builder.create();
        return alert;
    }

    private abstract class DialogOnClickListener implements OnClickListener

    {

        Dialog mDialog;

        DialogOnClickListener(Dialog d) {

            mDialog = d;

        }

    }

    private abstract class SelectColorOnClickListener implements
            DialogInterface.OnClickListener

    {

        SelectColorAdapter mAdapter;

        SelectColorOnClickListener(SelectColorAdapter sca) {

            mAdapter = sca;

        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {

        // dialog 原本不是這樣

        Dialog dialog = null;

        switch (id) {

            case DIALOG_CHECK:

                dialog = editFaceDialog();

                break;

            case DIALOG_INVALID_CUBE:
                dialog = invalidCubeLessDialog();
                break;

            case DIALOG_MORE_COLORS:
                // dialog = tooManyColorsDialog();
                break;

            case DIALOG_CHECK_COLORS:
                // dialog = checkColorsDialog();
                break;

            case DIALOG_FIX_COLORS:
                dialog = invalidCubeMoreDialog();
                break;

            default:

                dialog = null;

        }

        return dialog;

    }

}

package cubeassistant.com;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SelectColorAdapter extends BaseAdapter {

    ColorDecoder mDecoder;

    public SelectColorAdapter(ColorDecoder decoder) {
        mDecoder = decoder;
    }

    @Override
    public int getCount() {
        return mDecoder.colorSize();
    }

    @Override
    public Object getItem(int position) {
        Byte colorByte = mDecoder.getSortedId(position);
        return colorByte != null ? colorByte : new Byte((byte) 0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View nv;
        if (convertView == null) {
            nv = new SelectFaceletView(parent.getContext());
        } else { // Reuse/Overwrite the View passed
            nv = convertView;
        }
        Byte colorId = mDecoder.getSortedId(position);
        Bitmap bitmap = mDecoder.getBitmap(colorId);
        ((SelectFaceletView) nv).updateView(String.format("Color %02d", colorId), bitmap);

        return nv;
    }

}

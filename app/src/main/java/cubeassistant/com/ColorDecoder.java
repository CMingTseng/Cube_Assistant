package cubeassistant.com;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * ColorDecoder is NOT thread safe. Working on it.
 */
public class ColorDecoder implements Parcelable {
    //private List<HColor> colors;
    //private List<Bitmap> images;
    private ConcurrentNavigableMap<Byte, Parcelable[]> ids;
    byte firstNewCol;
    byte nextId;
    String cacheDir;

	/*static {
		System.loadLibrary("colordecoder");
    }*/

    public ColorDecoder(String cache) {
        //colors = new ArrayList<HColor>();
        //images = new ArrayList<Bitmap>();
        ids = new ConcurrentSkipListMap<Byte, Parcelable[]>();
        //firstNewCol = 0;
        nextId = 0;
        cacheDir = cache;
    }

//	private void free() {
//		for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
//			Log.d("DECODER", "(free) Recycling bitmap " + entry.getKey());
//			((Bitmap)entry.getValue()[1]).recycle();
//		}
//	}

    private static int sobel(Bitmap image, int x, int y) {
        int r, g, b, color, horizSobel, vertSobel;
        int[][] sob = new int[3][3];
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                color = image.getPixel(x + i, y + j);
                r = (color >> 16) & 0xFF;
                g = (color >> 8) & 0xFF;
                b = color & 0xFF;
                sob[i + 1][j + 1] = (int) (r * 299.0 / 1000 + g * 587.0 / 1000 + b * 114.0 / 1000);
            }
        }
        horizSobel = -(sob[1 - 1][1 - 1]) + (sob[1 + 1][1 - 1])
                - (sob[1 - 1][1]) - (sob[1 - 1][1]) + (sob[1 + 1][1])
                + (sob[1 + 1][1]) - (sob[1 - 1][1 + 1]) + (sob[1 + 1][1 + 1]);
        vertSobel = -(sob[1 - 1][1 - 1]) - (sob[1][1 - 1]) - sob[1][1 - 1]
                - (sob[1 + 1][1 - 1]) + (sob[1 - 1][1 + 1]) + (sob[1][1 + 1])
                + (sob[1][1 + 1]) + (sob[1 + 1][1 + 1]);
        return Math.min(255, Math.max(0, (horizSobel + vertSobel) / 2));
    }

    //protected static native int[][] nativeSobelData(Bitmap bitmap);
//	
//	protected static int[][] sobelData(Bitmap image) {
//		int r,g,b,color,horizSobel,vertSobel;
//		int imWidth = image.getWidth();
//		int imHeight = image.getHeight();
//		//Log.d("DAT", String.format("Width - %d Height %d", imWidth, imHeight));
//		int[][] out = new int[imWidth][imHeight];  
//		int[][] sob = new int[3][3];
//		for (int x=1; x<imWidth-1; x++) {
//			for (int y=1; y<imHeight-1; y++) {
//				for (int i=-1; i<=1; i++) {
//					for (int j=-1; j<=1; j++) {
//						color = image.getPixel(x+i,y+j);
//						r = (color >> 16) & 0xFF;
//						g = (color >> 8) & 0xFF;
//						b = color & 0xFF;
//						sob[i+1][j+1] = (int) (r * 299.0/1000 + g * 587.0/1000 + b * 114.0/1000);
//					}
//				}
//				horizSobel = -(sob[1-1][1-1]) + 
//			      (sob[1+1][1-1]) - 
//			      (sob[1-1][1]) - (sob[1-1][1]) +
//			      (sob[1+1][1]) + (sob[1+1][1]) -
//			      (sob[1-1][1+1]) + 
//			      (sob[1+1][1+1]);
//	            vertSobel =  -(sob[1-1][1-1]) - 
//	            (sob[1][1-1]) - sob[1][1-1] - 
//	            (sob[1+1][1-1]) +
//	            (sob[1-1][1+1]) + 
//	            (sob[1][1+1]) + (sob[1][1+1]) + 
//	            (sob[1+1][1+1]);
//	           out[x][y] = Math.min(255, Math.max(0, (horizSobel+vertSobel)/2));
//			}
//		}
//		return out;
//	}

    private HColor avg(List<HColor> L) {
        double h, l, s;
        int r, g, b;
        h = l = s = 0;
        r = g = b = 0;
        for (HColor color : L) {
            h += color.h;
            r += color.r;
            g += color.g;
            b += color.b;
            l += color.l;
            s += color.s;
        }
        int sz = L.size();
        if (sz == 0) return new HColor(0.0, 0.0, 0.0, 0, 0, 0);
        return new HColor(h / sz, l / sz, s / sz, r / sz, g / sz, b / sz);
    }

    public HColor getColor(byte key) {
        Parcelable[] color = ids.get(key);
        if (color != null) {
            return (HColor) color[0];
        }
        return null;
    }

    /**
     * Returns the bitmap for the given id.
     *
     * @param id of the Bitmap
     * @return Bitmap with the given id or null if not found
     */
    public Bitmap getBitmap(Byte id) {
        Log.d("DECODER", "Trying to access key " + id);
        Log.d("DECODER", "ids size = " + ids.size());
        Parcelable[] color = ids.get(id);
        if (color != null) {
            return (Bitmap) color[1];
        }
        return null;
    }

    /**
     * Removes the color with the given id including the underlying
     * bitmap file on disk.
     *
     * @param id of the color to remove
     */
    public void removeColor(byte id) {
        Log.d("DECODER", "Removing key " + id);
        Parcelable[] color = ids.get(id);
        HColor v = (HColor) color[0];
        File outPath = new File(cacheDir, v.toString());
        if (outPath.exists()) {
            if (!outPath.delete()) {
                Log.e("DECODER", "Failed to delete file: " + outPath.getAbsolutePath());
            }
        }
        if (!ids.remove(id, color)) {
            throw new ConcurrentModificationException(
                    "Value at ID " + id + " was modified while trying to be removed.");
        }
    }

    /**
     * Number of colors that have been decoded.
     *
     * @return number of colors
     */
    public int colorSize() {
        return ids.size();
    }

    public boolean hasId(byte id) {
        return ids.containsKey(id);
    }

    /**
     * Returns the first (0th) color key.
     *
     * @return the first key or null if no keys
     */
    public Byte getFirstId() {
        if (!ids.isEmpty()) {
            return ids.firstKey();
        }
        return null;
    }

    /**
     * Gets an unmodifiable sorted set of the color keys.
     *
     * @return the sorted set of keys
     */
    public SortedSet<Byte> getIds() {
        return Collections.unmodifiableSortedSet(ids.keySet());
    }

    /**
     * Gets an unmodifiable sorted set of the color keys. If
     * their is 1 or less keys and empty set is returned.
     *
     * @return the sorted set of keys
     */
    public SortedSet<Byte> getAllButFirstIds() {
        if (ids.size() > 1) {
            return Collections.unmodifiableSortedSet(
                    ids.keySet().tailSet(ids.firstKey(), false));
        } else {
            return Collections.unmodifiableSortedSet(new TreeSet<Byte>());
        }
    }


    private void deleteImages() {
        for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
            HColor v = (HColor) entry.getValue()[0];
            // try {
            File outPath = new File(cacheDir, v.toString());
            if (outPath.exists()) {
                if (!outPath.delete()) {
                    Log.e("DECODER", "Failed to delete file: " + outPath.getAbsolutePath());
                }
            }
            //} catch (Exception e)
            //{
            //}
        }
    }

    public byte[] colorArray() {
        byte[] ret = new byte[5 * ids.size()];
        int i = 0;
        for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
            if (i >= ret.length) {
                throw new ConcurrentModificationException("Keys were added to ids while trying to create the colorArray");
            }
            ret[i] = entry.getKey();
            i++;
            System.arraycopy(((HColor) entry.getValue()[0]).asByteArray(), 0, ret, i, ((HColor) entry.getValue()[0]).asByteArray().length);
            i += ((HColor) entry.getValue()[0]).asByteArray().length;
        }
        if (i < ret.length) {
            throw new ConcurrentModificationException("Keys were removed from ids while trying to create the colorArray");
        }
        return ret;
    }

    public void clear() {
        //colors.clear();
        //images.clear();
        Log.d("DECODER", "(Clear) Clearing ids");
        // This recycles the bitmaps, but because the views may still have
        // references to the bitmaps we should not explicitly recycle them
        // free();
        deleteImages();
        ids.clear();
    }

    public byte[] decode(Bitmap im) {
        firstNewCol = (byte) (nextId + 1);
        long s = System.currentTimeMillis();
		/*int[][] sobelDat;
		if (android.os.Build.VERSION.SDK_INT >= 8 ) {
			sobelDat = nativeSobelData(im); 
		} 
		else {
			sobelDat = sobelData(im); 
		}*/
        long e = System.currentTimeMillis();
        long sobelTime = e - s;
        byte[] ret = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1};//new ArrayList<Byte>();
        s = System.currentTimeMillis();
        int retCount = 0;
        ArrayList<HColor> subCubes = new ArrayList<HColor>();
        //ArrayList<HColor> hues = new ArrayList<HColor>();
        ArrayList<HColor> cubeVals = new ArrayList<HColor>();
        HColor c;
        int x0, y0, x1, y1, xc, yc, l, h;
        int width = im.getWidth();
        int height = im.getHeight();
        int margin = (int) (Math.min(width, height) * .1);
        int sideLength = Math.min(width, height) - margin;
        for (int i = 0; i < 9; i++) {
            subCubes.clear();
            x0 = (width - sideLength) / 2 + (sideLength / 3) * ((i / 3) % 3);
            y0 = height - margin / 2 - (sideLength / 3) * (i % 3);//margin/2 + sideLength * (3 - (i % 3))/3;
            xc = x0 + (sideLength / 6);
            yc = y0 - (sideLength / 6);
            x1 = x0 + (sideLength / 3);
            y1 = y0 - (sideLength / 3);

            // Q1
            for (int x = xc; x > x0; x--) {
                if (sobel(im, x, yc) > 20) break;
                for (int y = yc; y < y0; y++) {
                    //Log.d("DSSD", String.format("%d %d %d %d", x,y,y0,width));
                    if (sobel(im, x, y) > 20) break;
                    c = new HColor(im.getPixel(x, y));
                    subCubes.add(c);
                }

            }

            // Q2
            for (int x = xc; x > x0; x--) {
                if (sobel(im, x, yc) > 20) break;
                for (int y = yc; y > y1; y--) {
                    if (sobel(im, x, y) > 20) break;
                    c = new HColor(im.getPixel(x, y));
                    subCubes.add(c);
                }

            }

            // Q3
            for (int x = xc; x < x1; x++) {
                if (sobel(im, x, yc) > 20) break;
                for (int y = yc; y < y0; y++) {
                    if (sobel(im, x, y) > 20) break;
                    c = new HColor(im.getPixel(x, y));
                    subCubes.add(c);
                }

            }

            // Q4
            for (int x = xc; x < x1; x++) {
                if (sobel(im, x, yc) > 20) break;
                for (int y = yc; y > y1; y--) {
                    if (sobel(im, x, y) > 20) break;
                    c = new HColor(im.getPixel(x, y));
                    subCubes.add(c);
                }

            }

            Collections.sort(subCubes);
            l = (int) (subCubes.size() * .35);
            h = (int) (subCubes.size() * .65);
            c = avg(subCubes.subList(l, h));
            cubeVals.add(c);
        }
		/*for (int i=1; i<9; i++)
		{
			Log.d("DISTANCE", String.format("%d - %.5f units", i, cubeVals.get(0).distance(cubeVals.get(i))));
		}*/
		/*HColor base = new HColor(0,0,0,0,0,0);
		for (int i=0; i<9; i++)
		{
			Log.d("DISTANCE", String.format("%d - %.5f units", i, base.distance(cubeVals.get(i))));
		}*/
        for (int i = 0; i < 9; i++) {
            //int cz = ids.keySet().size();
            boolean foundCol = false;
            //for (int j=0; j < cz; j++) {

            Byte key = cubeVals.get(i).mostSimilar(new ArrayList<Byte>(ids.keySet()), this, 35);
            if (key != null) {
                foundCol = true;
                ret[retCount] = key;
                retCount += 1;

                getColor(key).usurp(cubeVals.get(i));
            }
			/*
			for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
				if (cubeVals.get(i).isSimilar((HColor) entry.getValue()[0])) {
					//ret.add(entry.getKey());
					ret[retCount] = entry.getKey();
					retCount+=1;
					foundCol = true;
					break;
				}
			}*/
            if (!foundCol) {
                x1 = (width - sideLength) / 2 + (sideLength / 3) * ((i / 3) % 3);
                y1 = height - margin / 2 - (sideLength / 3) * (i % 3) - (sideLength / 3);
                nextId++;
                int[] colors = new int[100 * 100];
                java.util.Arrays.fill(colors, 0, 100 * 100, cubeVals.get(i).getColor());
                //ids.put(nextId, new Parcelable[]{cubeVals.get(i), Bitmap.createBitmap(colors, 100, 100, Bitmap.Config.ARGB_8888)});
                Bitmap imref = Bitmap.createBitmap(im, x1, y1, sideLength / 3, sideLength / 3);
                if (imref.getWidth() > 100) {
                    int newWidth = 100;
                    int newHeight = 100;
                    float scaleWidth = ((float) newWidth) / imref.getWidth();
                    float scaleHeight = ((float) newHeight) / imref.getHeight();
                    Matrix matrix = new Matrix();
                    matrix.postScale(scaleWidth, scaleHeight);
                    imref = Bitmap.createBitmap(imref, 0, 0,
                            imref.getWidth(), imref.getHeight(), matrix, true);
                }
                Log.d("DECODER", "(decode) Adding key " + nextId);
                ids.put(nextId, new Parcelable[]{cubeVals.get(i), imref});
                //images.add(Bitmap.createBitmap(im, x1, y1, sideLength / 3, sideLength / 3));
                //ret.add(colors.size()-1);
                ret[retCount] = nextId;
                retCount += 1;
            }
        }
        e = System.currentTimeMillis();
        long funcTime = e - s;
        Log.d("DECODER", String.format("Sobel time - %dms", sobelTime));
        Log.d("DECODER", String.format("Func time - %dms", funcTime));
        return ret;

    }

    private ColorDecoder(Parcel in) {
        this(in.readString());
        //in.readTypedList(colors, HColor.CREATOR);
        //in.readTypedList(images, Bitmap.CREATOR);
        //in.readMap(ids, Map.class.getClassLoader());
        //Object[] keys = in.readArray(Integer.class.getClassLoader());
        int bysz = in.readInt();
        byte[] keys = new byte[bysz];
        in.readByteArray(keys);
        Bundle b = in.readBundle();
        for (int i = 0; i < keys.length; i++) {
            b.setClassLoader(HColor.class.getClassLoader());
            Parcelable[] v = b.getParcelableArray("" + keys[i]);
            //int[] colors = new int[100*100];
            //java.util.Arrays.fill(colors, 0, 100*100, ((HColor)v[0]).getColor());
            //Parcelable[] tw = {v[0], Bitmap.createBitmap(colors, 100, 100, Bitmap.Config.ARGB_8888)};
            File inPath = new File(cacheDir, ((HColor) v[0]).toString());
            FileInputStream inStream;
            Bitmap f = null;
            try {
                inStream = new FileInputStream(inPath);
                f = BitmapFactory.decodeStream(inStream);
                inStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            //((Bitmap)entry.getValue()[1]).compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            //ids.put(keys[i], b.getParcelableArray(""+keys[i]));
            Parcelable[] tw = {v[0], f};
            Log.d("DECODER", "(ColorDecoder) Adding key " + keys[i]);
            ids.put(keys[i], tw);
        }
        //Log.d("Parceling", ids.size()+"");
        firstNewCol = in.readByte();
        nextId = in.readByte();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private byte[] toByteArray(Set<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        int i = 0;
        for (Byte by : bytes) {
            ret[i] = by;
            i++;
        }
        return ret;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //out.writeTypedList(colors);
        //out.writeTypedList(images);
        //out.write
        Bundle b = new Bundle();
        //Integer[] = new int[];
        for (Map.Entry<Byte, Parcelable[]> entry : ids.entrySet()) {
            Parcelable[] v = {entry.getValue()[0]};
            b.putParcelableArray("" + entry.getKey(), v);
            try {
                File outPath = new File(cacheDir, ((HColor) entry.getValue()[0]).toString());
                //if (!outPath.exists()) {
                FileOutputStream outStream = new FileOutputStream(outPath);
                ((Bitmap) entry.getValue()[1]).compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                outStream.close();
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //out.writeArray((Integer[]) ids.keySet().toArray(new Byte[0]));
        out.writeString(cacheDir);
        out.writeInt(ids.keySet().size());
        out.writeByteArray(toByteArray(ids.keySet()));
        out.writeBundle(b);
        out.writeByte(firstNewCol);
        out.writeByte(nextId);
    }

    public static final Parcelable.Creator<ColorDecoder> CREATOR = new Parcelable.Creator<ColorDecoder>() {
        public ColorDecoder createFromParcel(Parcel in) {
            return new ColorDecoder(in);
        }

        public ColorDecoder[] newArray(int size) {
            return new ColorDecoder[size];
        }
    };

    /**
     * Gets the id at the given position from the sorted list of ids.
     *
     * @param position of the id
     * @return Byte id at the given position
     */
    public Byte getSortedId(int position) {
        int i = 0;
        for (Byte key : ids.keySet()) {
            if (i == position) {
                return key;
            }
            i++;
        }
        return null;
    }

    /**
     * Removes any colors that are not in the used colors set.
     *
     * @param usedColors set of colors that are used
     */
    public void removeUnusedColors(Set<Byte> usedColors) {
        ids.keySet().retainAll(usedColors);
    }
}

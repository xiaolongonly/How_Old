package com.xl.how_old;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;


public class MainActivity extends ActionBarActivity implements OnClickListener {
    private static final int PICK_CODE = 0x110;
    private ImageView mphoto;
    private Bitmap mphotoImg;
    private Button mGetImage;
    private Button mDetect;
    private TextView mtip;
    private String mCurrentPhotoStr;
    private View mwaitting;
    private Paint mPaint;
    private final String TAG="MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvents();
        mPaint= new Paint();
    }

    private void initEvents() {
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if( requestCode ==PICK_CODE)
        {
            if(intent!=null)
            {
                Uri uri = intent.getData();

                String type = intent.getType();
                Log.d(TAG, "uri is " + uri);
                if (uri.getScheme().equals("file") && (type.contains("image/"))) {
                    String path = uri.getEncodedPath();
                    Log.d(TAG, "path1 is " + path);
                    if (path != null) {
                        path = Uri.decode(path);
                        Log.d(TAG, "path2 is " + path);
                        ContentResolver cr = this.getContentResolver();
                        StringBuffer buff = new StringBuffer();
                        buff.append("(")
                                .append(Images.ImageColumns.DATA)
                                .append("=")
                                .append("'" + path + "'")
                                .append(")");
                        Cursor cur = cr.query(
                                Images.Media.EXTERNAL_CONTENT_URI,
                                new String[] { Images.ImageColumns._ID },
                                buff.toString(), null, null);
                        int index = 0;
                        for (cur.moveToFirst(); !cur.isAfterLast(); cur
                                .moveToNext()) {
                            index = cur.getColumnIndex(Images.ImageColumns._ID);
                            // set _id value
                            index = cur.getInt(index);
                        }
                        if (index == 0) {
                            //do nothing
                        } else {
                            Uri uri_temp = Uri
                                    .parse("content://media/external/images/media/"
                                            + index);
                            Log.d(TAG, "uri_temp is " + uri_temp);
                            if (uri_temp != null) {
                                uri = uri_temp;
                            }
                        }
                    }
                }
                Log.e(TAG, uri.toString());
                Cursor cursor = getContentResolver().query(uri,null,null,null,null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(Images.ImageColumns.DATA);
                mCurrentPhotoStr =cursor.getString(idx);
                cursor.close();
                resizePhoto();


                mphoto.setImageBitmap(mphotoImg);
                mtip.setText("Click DeTect ===>");
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(mCurrentPhotoStr,options);
        double ratio  = Math.max(options.outWidth *1.0d/1024f,options.outHeight*1.0d/1024f);
        options.inSampleSize = (int)Math.ceil(ratio);
        options.inJustDecodeBounds=false;
        mphotoImg =BitmapFactory.decodeFile(mCurrentPhotoStr,options);
    }
    private  static final int MSG_SUCCESS =0x111;
    private static  final int MSG_ERROR= 0X112;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_SUCCESS:
                    mwaitting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    prepareRsBitmap(rs);
                    mphoto.setImageBitmap(mphotoImg);
                    break;
                case MSG_ERROR:
                    mwaitting.setVisibility(View.GONE);
                    String erroeMsg = (String) msg.obj;
                    if(TextUtils.isEmpty(erroeMsg))
                    {
                        mtip.setText("Error");
                    }
                    else
                    {
                        mtip.setText(erroeMsg);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap =Bitmap.createBitmap(mphotoImg.getWidth(),mphotoImg.getHeight(),mphotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mphotoImg, 0, 0, null);

        try {
            JSONArray faces  = rs.getJSONArray("face");
            int faceCount =faces.length();
            mtip.setText("find"+faceCount);

            for(int i=0;i<faceCount;i++)
            {
                JSONObject face =faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");
                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");
                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");
                x =x/100 *bitmap.getWidth();
                y =y/100 *bitmap.getHeight();
                w =w/100 *bitmap.getWidth();
                h =h/100 *bitmap.getHeight();
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);
                //画box
                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);
                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender =face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                Bitmap ageBitmap = buildAgeBitmap(age,"Male".equals(gender));
                int ageWidth =ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();
                if(bitmap.getWidth()<mphoto.getWidth()&&bitmap.getHeight()<mphoto.getHeight())
                {
                    float ratio = Math.max(bitmap.getWidth()*1.0f/mphoto.getWidth(),bitmap.getHeight()*1.0f/mphoto.getHeight());
                    ageBitmap =Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);
                }
                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);

                mphotoImg=bitmap;

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView) findViewById(R.id.id_age_and_gender);
        tv.setText(age + "");
        if(isMale)
        {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);
        }else
        {
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return bitmap;
    }

    private void initViews() {
        mphoto = (ImageView) findViewById(R.id.id_photo);
        mGetImage = (Button) findViewById(R.id.id_get_image);
        mDetect = (Button) findViewById(R.id.id_detect);
        mtip = (TextView) findViewById(R.id.id_tip);
        mwaitting = findViewById(R.id.id_waitting);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.id_get_image:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PICK_CODE);
                break;
            case R.id.id_detect:
                mwaitting.setVisibility(View.VISIBLE);
                if(mCurrentPhotoStr!=null && !mCurrentPhotoStr.trim().equals(""))
                {
                    resizePhoto();
                }else
                {
                    mphotoImg=BitmapFactory.decodeResource(this.getResources(), R.drawable.t5);
                }
                FaceppDetect.detect(mphotoImg, new FaceppDetect.Callback() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what =MSG_SUCCESS;
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what =MSG_ERROR;
                        msg.obj = exception;
                        mHandler.sendMessage(msg);
                    }
                });
                break;
        }

    }


}

package com.shoravsuriyal.image_classifier3;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button upload;
    Button classify;
    ImageView image;
    private static Bitmap bitmap;
    TextView textView;
    private TensorFlowInferenceInterface tensorFlowInferenceInterface;
    private static final int SELECTED_PIC = 1;
    int index;
    private static final String GRAPH_FILE = "file:///android_asset/optimized_graph.pb";
    private static final String INPUT_NODE = "input";
    private static final String OUTPUT_NODE = "final_result";
    private static final String TEXT_LABEL= "file:///android_asset/retrained_labels.txt";
    private static final int IMAGE_SIZE = 224;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    static {

        System.loadLibrary("tensorflow_inference");
    }

    private float[] getPixelData(Bitmap bitmap){

        boolean landscape=bitmap.getWidth()>bitmap.getHeight();
        Log.d("bitmap","I am calling bitmap");
        Log.d("landscape","I am inside get pixels data function "+bitmap.getWidth());

        float scale_factor;
        if (landscape) scale_factor=(float)IMAGE_SIZE/bitmap.getHeight();
        else scale_factor=(float)IMAGE_SIZE/bitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(scale_factor,scale_factor);

//        Bitmap croppedBitmap;
//        if(landscape){
//
//            int start=(bitmap.getWidth()-bitmap.getHeight())/2;
//            croppedBitmap= Bitmap.createBitmap(bitmap,start,0,bitmap.getHeight()-1,bitmap.getHeight()-1,matrix,true);
//
//        } else {
//
//            int start= (bitmap.getHeight()-bitmap.getWidth())/2;
//            croppedBitmap=Bitmap.createBitmap(bitmap,0,start,bitmap.getWidth()-1,bitmap.getWidth()-1,matrix,true);
//
//        }

        int pixels[]=new int[IMAGE_SIZE*IMAGE_SIZE];

        //croppedBitmap.getPixels(pixels,0,IMAGE_SIZE,0,0,IMAGE_SIZE,IMAGE_SIZE);
        float[] retPixels=new float[3*pixels.length];
        for (int i=0; i<pixels.length; ++i){

            final int val=pixels[i];
            retPixels[i* 3+ 0]=(((val>>16)& 0xFF)-IMAGE_MEAN)/(IMAGE_STD);
            retPixels[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / (IMAGE_STD);
            retPixels[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / (IMAGE_STD);

        }
        return retPixels;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        upload = findViewById(R.id.upload);
        classify = findViewById(R.id.classify);
        textView=findViewById(R.id.textView);
        image = findViewById(R.id.imageView);
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(getAssets(), GRAPH_FILE);
        upload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, SELECTED_PIC);

            }

        });
        classify.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                float[] outputs= new float[2];
                String str;
                float[] pixels= getPixelData(MainActivity.getBitmap());
                //Bitmap f=getBitmap();
                //float[] pixels= getPixelData(f);
                // This will feed the graph file
                //int g=bitmap.getWidth();
                //Log.d("g","g"+Float.toString(g));

                // Feeding the graph file with input.
                tensorFlowInferenceInterface.feed(INPUT_NODE,pixels,1,IMAGE_SIZE,IMAGE_SIZE,3);
                // Time to run the graph file

                tensorFlowInferenceInterface.run(new String[] {OUTPUT_NODE});
                // Get the output data from the graph
                Log.d("outputs","the output is "+outputs);

                tensorFlowInferenceInterface.fetch(OUTPUT_NODE,outputs);

                if (outputs[0]>outputs[1]){
                    str="Diabetic Retinopathy";
                    index=0;}
                else{
                    str="No Diabetic Retinopathy";
                    index=1;}
                //String str1="DR: "+ Float.toString(outputs[0]*100)+" No DR: "+Float.toString(outputs[1]*100);
                textView.setText(str+'\n'+"Score = "+Float.toString(outputs[index]*100)+"%");

            }
        });
    }

    public static Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        MainActivity.super.onActivityResult(requestCode,resultCode,data);
        switch (requestCode){

            case SELECTED_PIC:
                if (resultCode==RESULT_OK){
                    Uri uri=data.getData();
                    String[] projection={MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(projection[0]);
                    String filepath = cursor.getString(columnIndex);
                    cursor.close();
                    this.bitmap = BitmapFactory.decodeFile(filepath);

                    //Drawable drawable = new BitmapDrawable(bitmap);
                    //image.setBackground(drawable);
                    image.setImageBitmap(MainActivity.getBitmap());
                    classify.setVisibility(classify.VISIBLE);

                    }
                    break;
                default:
                    break;
            }
        }
    }
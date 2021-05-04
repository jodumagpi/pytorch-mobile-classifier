package com.example.whatisthis;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class ResultPage extends AppCompatActivity {

    ImageView imageView;
    TextView textView0, textView1, textView2, textView3, textView4;
    Uri img_uri;
    String stringUri;
    ArrayList<String> prediction;
    Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.getSupportActionBar().hide();
        setContentView(R.layout.activity_result);

        imageView = findViewById(R.id.imageView);
        textView0 = findViewById(R.id.textView0);
        textView1 = findViewById(R.id.textView1);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if(extras.containsKey("IMG_URI")){
                stringUri = extras.getString("IMG_URI");
                img_uri = Uri.parse(stringUri);
            }
            if(extras.containsKey("PREDICTION")){
                prediction = extras.getStringArrayList("PREDICTION");
                textView0.setText(prediction.get(0));
                textView1.setText(prediction.get(1));
                textView2.setText(prediction.get(2));
                textView3.setText(prediction.get(3));
                textView4.setText(prediction.get(4));
            }
        }

        try{
            image = MediaStore.Images.Media.getBitmap(getContentResolver(), img_uri);
            imageView.setImageBitmap(RotateBitmap(image, 90));
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}

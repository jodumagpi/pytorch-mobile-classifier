package com.example.whatisthis;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQ_CODE = 102;
    public static final int PICK_IMG = 104;

    ImageButton cameraBtn, uploadBtn;
    ProgressBar progressBar;
    String currentPhotoPath;
    String stringUri;

    private Bitmap bitmap = null;
    private Module module = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        try {
            module = Module.load(assetFilePath(this, "model.pt"));
        } catch (IOException e) {
            Log.e("Pytorch Error", "Error loading model.");
            finish();
        }

        cameraBtn = findViewById(R.id.CameraCapture);
        uploadBtn = findViewById(R.id.ImageUpload);
        progressBar = findViewById(R.id.progressBar);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermission();
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent();
                gallery.setType("image/*");
                gallery.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(gallery, "Select image to upload."), PICK_IMG);
            }
        });
    }

    private void askCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        } else {
            startCameraActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==CAMERA_PERM_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startCameraActivity();
            } else {
                Toast.makeText(this, "This app requires camera permissions.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCameraActivity() {
        String fileName = "pic";
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try{
            File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
            currentPhotoPath = imageFile.getAbsolutePath();
            Uri imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.example.whatisthis.fileprovider",
                    imageFile);

            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cam.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cam, CAMERA_REQ_CODE);
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> runInference(Uri imgUri){

        // get bitmap and model
        try {
            bitmap = scaledBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri), 0.5);
        } catch (IOException e) {
            Log.e("Pytorch Error", "Error loading image.");
            finish();
        }

        // convert bitmap to pytorch tensor
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        // forward pass on the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // get tensor contents as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();
        // search for the index with max scores
        //float maxScore = -Float.MAX_VALUE;
        //int maxScoreIdx = -1;
        int[] maxIdx = maxKIndex(scores, 5);
        //for(int i = 0; i < scores.length; i++){
        //    if(scores[i] > maxScore){
        //        maxScore = scores[i];
        //        maxScoreIdx = i;
        //    }
        //}

        ArrayList<String> predictions = new ArrayList<String>();

        for(int i = 0; i < 5; i++){
            predictions.add(org.pytorch.helloworld.ImageNetClasses.IMAGENET_CLASSES[maxIdx[i]]);
        }

        return predictions;
    }

    public static String assetFilePath(Context context, String assetName) throws  IOException {
        File file = new File(context.getFilesDir(), assetName);
        if(file.exists() && file.length() > 0){
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        ExampleAsyncTask task = new ExampleAsyncTask(this);

        if(requestCode == CAMERA_REQ_CODE){
            if(resultCode == RESULT_OK){
                File f = new File(currentPhotoPath);
                stringUri = Uri.fromFile(f).toString();
                task.execute(Uri.fromFile(f));
            }
        }
        if(requestCode == PICK_IMG && resultCode == RESULT_OK){
            Uri contentUri = data.getData();
            stringUri = contentUri.toString();
            task.execute(contentUri);
        }
    }

    private static class ExampleAsyncTask extends AsyncTask<Uri, Void, ArrayList<String>> {
        private WeakReference<MainActivity> activityWeakReference;
        ExampleAsyncTask(MainActivity activity) {
            activityWeakReference = new WeakReference<MainActivity>(activity);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.progressBar.setVisibility(View.VISIBLE);
            activity.cameraBtn.setVisibility(View.GONE);
            activity.uploadBtn.setVisibility(View.GONE);
        }
        @Override
        protected ArrayList<String> doInBackground(Uri... uris) {
            MainActivity activity = activityWeakReference.get();
            ArrayList<String> prediction = activity.runInference(uris[0]);
            return prediction;
        }
        @Override
        protected void onPostExecute(ArrayList<String> s) {
            super.onPostExecute(s);
            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            Intent result = new Intent(activity, ResultPage.class);
            result.putExtra("IMG_URI", activity.stringUri);
            result.putExtra("PREDICTION", s);
            activity.startActivity(result);
            activity.overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);

            activity.progressBar.setVisibility(View.GONE);
            activity.cameraBtn.setVisibility(View.VISIBLE);
            activity.uploadBtn.setVisibility(View.VISIBLE);
        }
    }

    public static int[] maxKIndex(float[] array, int top_k) {
        float[] max = new float[top_k];
        int[] maxIndex = new int[top_k];
        Arrays.fill(max, Float.NEGATIVE_INFINITY);
        Arrays.fill(maxIndex, -1);

        top: for(int i = 0; i < array.length; i++) {
            for(int j = 0; j < top_k; j++) {
                if(array[i] > max[j]) {
                    for(int x = top_k - 1; x > j; x--) {
                        maxIndex[x] = maxIndex[x-1]; max[x] = max[x-1];
                    }
                    maxIndex[j] = i; max[j] = array[i];
                    continue top;
                }
            }
        }
        return maxIndex;
    }

    private Bitmap scaledBitmap(Bitmap bitmap, double scale) {
        return Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale), (int)(bitmap.getHeight()*scale), true);
    }
}


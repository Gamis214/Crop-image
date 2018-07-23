package gamis214.com.crop_image_example;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.isseiaoki.simplecropview.CropImageView;
import com.isseiaoki.simplecropview.callback.CropCallback;
import com.isseiaoki.simplecropview.callback.LoadCallback;
import com.isseiaoki.simplecropview.callback.SaveCallback;
import com.isseiaoki.simplecropview.util.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private CropImageView cropImageView;
    private ConstraintLayout container;
    private RelativeLayout container_crop;

    private Uri imageUri;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnCamara = findViewById(R.id.btnCamara);
        Button btnGaleria = findViewById(R.id.btngaleria);
        Button btnCut = findViewById(R.id.btnCut);
        Button btnCortar = findViewById(R.id.btnCortar);
        imageView = findViewById(R.id.imageView);
        cropImageView = findViewById(R.id.cropImageView);
        container = findViewById(R.id.container);
        container_crop = findViewById(R.id.container_crop);
        btnCamara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCameraImage();
            }
        });
        btnGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageGallery();
            }
        });
        btnCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cutImage();
            }
        });
        btnCortar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageCut();
            }
        });
    }

    private void getCameraImage() {
        imageUri = createNewUri(this,mCompressFormat);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        startActivityForResult(intent, 1002);
    }

    private void saveImageCut() {
        cropImageView.crop(imageUri)
                .execute(new CropCallback() {
                    @Override public void onSuccess(Bitmap cropped) {
                        cropImageView.save(cropped)
                                .compressFormat(mCompressFormat)
                                .execute(createNewUri(getApplicationContext(), mCompressFormat), new SaveCallback() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        container_crop.setVisibility(View.GONE);
                                        container.setVisibility(View.VISIBLE);
                                        final InputStream imageStream;
                                        try {
                                            imageStream = getContentResolver().openInputStream(uri);
                                            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                                            imageView.setImageBitmap(selectedImage);
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                        Toast.makeText(MainActivity.this, "SUCCESS SAVE", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                        Toast.makeText(MainActivity.this, "ERROR SAVE", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    @Override public void onError(Throwable e) {
                    }
                });
    }

    private void cutImage() {
        container.setVisibility(View.GONE);
        container_crop.setVisibility(View.VISIBLE);
        cropImageView.setCropMode(CropImageView.CropMode.CIRCLE_SQUARE);
        cropImageView.load(imageUri).execute(new LoadCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "SUCCEES", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(MainActivity.this, "ERROR LOAD", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void getImageGallery(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode == RESULT_OK) {
            final InputStream imageStream;
            try {
                imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else if (requestCode == 1001 && resultCode == RESULT_OK) {
            try {
                imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    public static Uri createNewUri(Context context, Bitmap.CompressFormat format) {
        long currentTimeMillis = System.currentTimeMillis();
        Date today = new Date(currentTimeMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String title = dateFormat.format(today);
        String dirPath = getDirPath();
        String fileName = "scv" + title + "." + getMimeType(format);
        String path = dirPath + "/" + fileName;
        File file = new File(path);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + getMimeType(format));
        values.put(MediaStore.Images.Media.DATA, path);
        long time = currentTimeMillis / 1000;
        values.put(MediaStore.MediaColumns.DATE_ADDED, time);
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, time);
        if (file.exists()) {
            values.put(MediaStore.Images.Media.SIZE, file.length());
        }
        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Logger.i("SaveUri = " + uri);
        return uri;
    }

    public static String getDirPath() {
        String dirPath = "";
        File imageDir = null;
        File extStorageDir = Environment.getExternalStorageDirectory();
        if (extStorageDir.canWrite()) {
            imageDir = new File(extStorageDir.getPath() + "/simplecropview");
        }
        if (imageDir != null) {
            if (!imageDir.exists()) {
                imageDir.mkdirs();
            }
            if (imageDir.canWrite()) {
                dirPath = imageDir.getPath();
            }
        }
        return dirPath;
    }

    public static String getMimeType(Bitmap.CompressFormat format) {
        Logger.i("getMimeType CompressFormat = " + format);
        switch (format) {
            case JPEG:
                return "jpeg";
            case PNG:
                return "png";
        }
        return "png";
    }
}

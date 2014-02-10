package etlau.coursera.steganography;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends ActionBarActivity {

    private static final int RESULT_SECRET_IMAGE = 1;
    private static final int RESULT_PUBLIC_IMAGE = 2;
    private boolean secretDone;
    private boolean publicDone;
    private ImageView secretImageView;
    private ImageView publicImageView;
    private String secretImagePath;
    private String publicImagePath;
    private ImageView stenagraphyImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            return rootView;
        }


    }
    public void selectSecretImage(View v) {
        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_SECRET_IMAGE);
    }

    public void selectPublicImage(View v) {
        Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_PUBLIC_IMAGE);
    }

    private File getNewFile(String modificator) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/steganography");
        myDir.mkdirs();

        Date d = new Date();
        CharSequence dateString = DateFormat.format("MMMM_d_yyyy ", d.getTime());
        String filename = modificator+"_image_" + dateString + ".png";
        File file = new File (myDir, filename);
//        if(!file.exists())
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        return file;
    }
    public void decodeSteganography(View v) {
        if (publicDone) {
            File file = getNewFile("decoded");
            EncodeAndDecode.decodeImage(publicImagePath, file.getPath(), "");
            secretImagePath = file.getPath();
            showImage(file.getPath(), secretImageView);

            toastShow(getString(R.string.succ_decoded) + " " + file.getPath());
        } else {
            toastShow(getString(R.string.decode_warning));
        }
    }
    public void createSteganography(View v) {
        if (secretDone && publicDone) {
            File file = getNewFile("encoded");
            EncodeAndDecode.encodePicture(secretImagePath, publicImagePath, file.getPath());
            showImage(file.getPath(), stenagraphyImageView);

            toastShow(getString(R.string.stegano_created) + " " + file.getPath());

        } else {
            toastShow(getString(R.string.toast_check_select));
        }

    }

    private void toastShow(CharSequence text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == RESULT_SECRET_IMAGE || requestCode == RESULT_PUBLIC_IMAGE) && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView;
            secretImageView = (ImageView) findViewById(R.id.secret_image);
            publicImageView = (ImageView) findViewById(R.id.public_image);
            stenagraphyImageView = (ImageView) findViewById(R.id.steganography_image);

            if (requestCode == RESULT_SECRET_IMAGE) {
                secretDone = true;
                imageView = secretImageView;
                secretImagePath = picturePath;
            } else {
                publicDone = true;
                imageView = publicImageView;
                publicImagePath = picturePath;
            }

            showImage(picturePath, imageView);
        }
    }

    private void showImage(String picturePath, ImageView imageView) {
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int min;
        if (width > height) {
            min = height;
        }
        else {
            min = width;
        }

        imageView.setImageBitmap(decodeSampledBitmapFromResource(picturePath, min, min));
    }

    public static Bitmap decodeSampledBitmapFromResource(String path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}

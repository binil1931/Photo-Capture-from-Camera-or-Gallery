package com.polus.capturephoto;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity{

	private String mCurrentPhotoPath;
	AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	ImageView imageView;
	Camera camera;
	String imageEncoded;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		imageView = (ImageView) findViewById(R.id.imageView);

	}



	public void openGallery(View v)
	{
		Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, 2);
	}

	public void openCamera(View v)
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} 
		else 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) 
		{
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} 
		else 
		{
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}

		File f = null;

		try 
		{
			f = setUpPhotoFile();
			Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			startActivityForResult(cameraIntent, 1);
		} 
		catch (Exception e) 
		{
			Log.d("TAG", "Error while cam opening: " + e);
		}
	}

	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	private File setUpPhotoFile() throws IOException 
	{
		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "IMG_" + timeStamp + "_";
		File albumF = getAlbumDir();
		File imageF = File.createTempFile(imageFileName, ".jpg", albumF);
		return imageF;
	}

	private File getAlbumDir() 
	{
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) 
		{
			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir("SkopeiProfileCam");

			if (storageDir != null) 
			{
				if (! storageDir.mkdirs()) 
				{
					if (! storageDir.exists())
					{
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}

		} 
		else 
		{
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}

		return storageDir;
	}


	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = imageView.getWidth();
		int targetH = imageView.getHeight();


		ExifInterface exif = null;
		try 
		{
			exif = new ExifInterface(mCurrentPhotoPath);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int orientation = exif.getAttributeInt(
				ExifInterface.TAG_ORIENTATION,
				ExifInterface.ORIENTATION_NORMAL);

		int angle = 0;

		if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
			angle = 90;
			Log.e("Tag", "rotation");
		}
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
			angle = 180;
			Log.e("Tag", "rotation");
		}
		else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
			angle = 270;
			Log.e("Tag", "rotation");
		}

		Matrix mat = new Matrix();
		mat.postRotate(angle);

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */

		Bitmap bitmap_before_rotate = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		Bitmap bitmap;

		Log.e("Tag", "land angle " + angle);

		if(angle != 0)
		{
			bitmap = Bitmap.createBitmap(bitmap_before_rotate, 0, 0, bitmap_before_rotate.getWidth(),
					bitmap_before_rotate.getHeight(), mat, true);   
		}

		else
		{
			Log.e("Tag", "land scape");
			bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		}

		imageView.setImageBitmap(bitmap);
	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);



		try {
			if(requestCode == 1)
			{

				if (mCurrentPhotoPath != null)
				{
					setPic();
					galleryAddPic();
					mCurrentPhotoPath = null;
				}
			}

			else if (requestCode == 2) 
			{
				Uri selectedImage = data.getData();
				String[] filePath = { MediaStore.Images.Media.DATA };

				Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
				c.moveToFirst();
				int columnIndex = c.getColumnIndex(filePath[0]);
				String picturePath = c.getString(columnIndex);
				c.close();

				ExifInterface exif = null;
				try 
				{
					exif = new ExifInterface(picturePath);
				} 
				catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int orientation = exif.getAttributeInt(
						ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_NORMAL);

				int angle = 0;

				if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
					angle = 90;
					Log.e("Tag", "rotation");
				}
				else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
					angle = 180;
					Log.e("Tag", "rotation");
				}
				else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
					angle = 270;
					Log.e("Tag", "rotation");
				}

				Matrix mat = new Matrix();
				mat.postRotate(angle);

				//Bitmap photo = (BitmapFactory.decodeFile(picturePath));


				int targetW = imageView.getWidth();
				int targetH = imageView.getHeight();



				/* Get the size of the image */
				BitmapFactory.Options bmOptions = new BitmapFactory.Options();
				bmOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(picturePath, bmOptions);
				int photoW = bmOptions.outWidth;
				int photoH = bmOptions.outHeight;

				/* Figure out which way needs to be reduced less */
				int scaleFactor = 1;
				if ((targetW > 0) || (targetH > 0)) {
					scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
				}

				/* Set bitmap options to scale the image decode target */
				bmOptions.inJustDecodeBounds = false;
				bmOptions.inSampleSize = scaleFactor;
				bmOptions.inPurgeable = true;

				/* Decode the JPEG file into a Bitmap */

				Bitmap bitmap;
				Bitmap bitmap_before_rotate = BitmapFactory.decodeFile(picturePath, bmOptions);
				if(angle != 0)
				{
					bitmap = Bitmap.createBitmap(bitmap_before_rotate, 0, 0, bitmap_before_rotate.getWidth(),
							bitmap_before_rotate.getHeight(), mat, true);   
				}

				else
				{
					Log.e("Tag", "land scape");
					bitmap = BitmapFactory.decodeFile(picturePath, bmOptions);
				}

				imageView.setImageBitmap(bitmap);
			}
		} 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

package com.ledpixelart.pixelesque;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import com.ledpixelart.pixelesque.*;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class SaveTaskPixel extends AsyncTask<Void, Void, Boolean> {
	String name;
	int width, height;
	PixelArt data;
	ProgressDialog dialog;
	File location;
	File file;
	boolean export;
	boolean share;
	PixelArtEditor context;
	String filePath;
	
	public SaveTaskPixel(String name, int width, int height, PixelArt data, PixelArtEditor context) {
		this(name, width, height, data, context, null, false, false);			
	}
	public SaveTaskPixel(String name, int width, int height, PixelArt data, PixelArtEditor context, File location, boolean export, boolean share) {
		this.name = name; 
		this.width = width; 
		this.height = height; 
		this.data = data; 
		this.export = export;
		this.share = share;
		this.context = context;
		this.location = location;
		if (location == null) {
			this.location = StorageUtils.getSaveDirectory(context);
		}
		if (name == null) {
			String time = System.currentTimeMillis()+"";
			name = "Pixelesque-"+time.substring(time.length()-5);
		}
		this.location.mkdirs();
		file = new File(this.location, name+".png");
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		String title = context.getResources().getString(com.ledpixelart.pixelesque.R.string.saving_title);
		String text = context.getResources().getString(com.ledpixelart.pixelesque.R.string.saving_text);
		dialog = ProgressDialog.show(context, title, text);
	}
	
	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			Bitmap image;
			if (width < 0 && height < 0)
				image = data.render(context);
			else {
				if (width < 0)
					width = (height * data.width) / data.height;
				if (height < 0) 
					height = (width * data.height) / data.width;
				image = data.render(context, width, height);
			}
			
			StorageUtils.saveFile(name, file, image, context, !share && !export);
			ArtExtras.saveExtras(context, data, name);
			Log.d("SaveTask", "within class: "+file.getAbsolutePath());
		    filePath = file.getAbsolutePath();
			
			return true;
			

		} catch (Exception e) {
			e.printStackTrace();
		}				
		return false;

	}
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		dialog.dismiss();
		if (result == false) {
			Toast.makeText(context, com.ledpixelart.pixelesque.R.string.save_failed, Toast.LENGTH_SHORT).show();
		}
		
		else {
			context.artChangedName();
			//now let's write to PIXEL
			 Intent myIntent = new Intent(context.getApplicationContext(), PIXELWrite.class);
	         myIntent.putExtra("art_height", height); 
	         myIntent.putExtra("art_width", width); 
	         myIntent.putExtra("art_filename", filePath);
	         context.startActivity(myIntent);
		}
	}
	
}
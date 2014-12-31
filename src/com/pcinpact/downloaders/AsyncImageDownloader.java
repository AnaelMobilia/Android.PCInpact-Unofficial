/*
 * Copyright 2014 Anael Mobilia
 * 
 * This file is part of NextINpact-Unofficial.
 * 
 * NextINpact-Unofficial is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * NextINpact-Unofficial is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with NextINpact-Unofficial. If not, see <http://www.gnu.org/licenses/>
 */
package com.pcinpact.downloaders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import com.pcinpact.NextInpact;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * T�l�chargement asynchrone d'images
 * 
 * @author Anael
 *
 */
public class AsyncImageDownloader extends AsyncTask<String, Void, Bitmap> {
	// Types d'images
	public final static int IMAGE_MINIATURE_ARTICLE = 1;
	public final static int IMAGE_CONTENU_ARTICLE = 2;
	public final static int IMAGE_SMILEY = 3;

	// Donn�es sur l'image
	private String urlImage;
	private int typeImage;
	private Context monContext;
	private final WeakReference<ImageView> imageViewReference;

	public AsyncImageDownloader(ImageView imageView, int unTypeImage, Context unContext) {
		imageViewReference = new WeakReference<ImageView>(imageView);
		typeImage = unTypeImage;
		monContext = unContext;
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		// Les param�tres viennent de l'appel � execute() => [0] est une URL
		urlImage = params[0];

		// Je r�cup�re un OS sur l'image
		ByteArrayOutputStream monBAOS = Downloader.download(urlImage);

		// Calcul du nom de l'image (tout ce qui est apr�s le dernier "/", et avant un �ventuel "?" ou "#")
		String imgName = urlImage.substring(urlImage.lastIndexOf("/") + 1, urlImage.length()).split("\\?")[0].split("#")[0];

		File monFichier = null;
		switch (typeImage) {
			case IMAGE_CONTENU_ARTICLE:
				monFichier = new File(monContext.getFilesDir() + NextInpact.PATH_IMAGES_ILLUSTRATIONS, imgName);
				break;
			case IMAGE_MINIATURE_ARTICLE:
				monFichier = new File(monContext.getFilesDir() + NextInpact.PATH_IMAGES_MINIATURES, imgName);
				break;
			case IMAGE_SMILEY:
				monFichier = new File(monContext.getFilesDir() + NextInpact.PATH_IMAGES_SMILEYS, imgName);
				break;
		}

		// Ouverture d'un fichier en �crasement
		FileOutputStream monFOS = null;
		try {

			// Gestion de la mise � jour de l'application depuis une ancienne version
			try {
				monFOS = new FileOutputStream(monFichier, false);
			} catch (FileNotFoundException e) {
				// Cr�ation du r�pertoire...
				File leParent = new File(monFichier.getParent());
				leParent.mkdirs();
				// On retente la m�me op�ration
				monFOS = new FileOutputStream(monFichier, false);
			}

			monFOS.write(monBAOS.toByteArray());
			monFOS.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("AsyncImageDownloader", "Error while saving " + urlImage, e);
		}

		// Je d�code et renvoie le bitmap
		return BitmapFactory.decodeByteArray(monBAOS.toByteArray(), 0, monBAOS.size());
	}

	@Override
	// Post ex�cution
	protected void onPostExecute(Bitmap bitmap) {
		// J'affiche l'image dans son imageView
		if (imageViewReference != null) {
			ImageView imageView = imageViewReference.get();
			if (imageView != null) {
				imageView.setImageBitmap(bitmap);
			}
		}
	}

}

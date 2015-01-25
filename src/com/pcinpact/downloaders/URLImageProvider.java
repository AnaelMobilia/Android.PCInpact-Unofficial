/*
 * Copyright 2015 Anael Mobilia
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

import java.io.File;
import java.util.ArrayList;

import com.pcinpact.Constantes;
import com.pcinpact.R;
import com.pcinpact.adapters.DrawableRefreshable;
import com.pcinpact.items.Item;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html.ImageGetter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * T�l�chargement d'une image dans les commentaires : fournit une image vide, t�l�charge l'image, puis l'affichera
 * 
 * @author Anael
 *
 */
public class URLImageProvider implements ImageGetter, RefreshDisplayInterface {
	// Vue o� est affich�e l'image � charger
	private TextView maView;
	// Contexte de l'activit�
	private Context monContext;
	// private Drawable monRetour;
	private DrawableRefreshable monRetour = new DrawableRefreshable();

	/**
	 * Constructeur
	 * 
	 * @param laView
	 */
	public URLImageProvider(TextView uneView, Context unContext) {
		super();
		maView = uneView;
		monContext = unContext;
	}

	@SuppressLint("NewApi")
	@Override
	/**
	 * Fournir une image (URL)
	 */
	public Drawable getDrawable(String urlSource) {
		// Image de retour
		// Drawable monRetour;

		// D�termination de l'ID du smiley
		String nomSmiley = urlSource.substring(Constantes.NEXT_INPACT_URL_SMILEYS.length());

		// Le smiley existe-t-il en local ?
		File monFichier = new File(monContext.getFilesDir() + Constantes.PATH_IMAGES_SMILEYS, nomSmiley);
		if (monFichier.exists()) {
			// Je r�cup�re directement mon image
			monRetour.setImage(gestionTaille(Drawable.createFromPath(monContext.getFilesDir() + Constantes.PATH_IMAGES_SMILEYS
					+ nomSmiley)));
			// DEBUG
			if (Constantes.DEBUG) {
				Log.i("URLImageProvider", nomSmiley + " fourni depuis le cache");
			}
		} else {
			// Lancement du DL
			AsyncImageDownloader monAID = new AsyncImageDownloader(monContext, this, Constantes.IMAGE_SMILEY, urlSource);
			// Parall�lisation des t�l�chargements pour l'ensemble de l'application
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				monAID.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				monAID.execute();
			}

			// Retour d'une image g�n�rique (logo NXI)
			monRetour.setImage(gestionTaille(monContext.getResources().getDrawable(R.drawable.smiley_nextinpact)));
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("URLImageProvider", nomSmiley + " t�l�chargement en cours...");
			}
		}
		// Je retourne mon image
		return monRetour;
	}

	/**
	 * Charge et Zoome sur une image
	 * 
	 * @param pathImage
	 * @return
	 */
	private Drawable gestionTaille(Drawable uneImage) {
		// Pr�f�rences de l'utilisateur : taille du texte
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(monContext);
		// Taile par d�faut
		final int tailleDefaut = Integer.valueOf(monContext.getResources().getString(R.string.defautOptionZoomTexte));
		// L'option selectionn�e
		final int tailleOptionUtilisateur = Integer.parseInt(mesPrefs.getString(monContext.getString(R.string.idOptionZoomTexte),
				String.valueOf(tailleDefaut)));
		final float monCoeffZoom = tailleOptionUtilisateur / tailleDefaut;

		DisplayMetrics metrics = new DisplayMetrics();
		((WindowManager) monContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);

		int monCoeff;
		// Si on est sur la r�solution par d�faut, on reste � 1
		if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT) {
			monCoeff = Math.round(1 * monCoeffZoom);
		}
		// Sinon, on calcule le zoom � appliquer (avec un coeff 2 pour �viter les images trop petites)
		else {
			monCoeff = Math.round(2 * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT) * monCoeffZoom);
		}
		// On �vite un coeff inf�rieur � 1 (image non affich�e !)
		if (monCoeff < 1) {
			monCoeff = 1;
		}

		// On d�finit la taille de l'image
		uneImage.setBounds(0, 0, (uneImage.getIntrinsicWidth() * monCoeff), (uneImage.getIntrinsicHeight() * monCoeff));

		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("URLImageProvider",
					"gestionTaille : coeefZoom = " + monCoeff + " => hauteur = " + uneImage.getIntrinsicHeight()
							+ " - largeur = " + uneImage.getIntrinsicWidth());
		}

		return uneImage;
	}

	@Override
	public void downloadHTMLFini(String uneURL, ArrayList<Item> mesItems) {
		// TODO Auto-generated method stub
	}

	@Override
	public void downloadImageFini(String uneURL, Bitmap uneImage) {
		// Le smiley a �t� t�l�charg� => reaffichage de la vue
		// Chargment de l'image
		Drawable imageBrute = new BitmapDrawable(monContext.getResources(), uneImage);
		// Mise � l'�chelle de l'image
		monRetour.setImage(gestionTaille(imageBrute));

		// Rafraichissement de la vue
		maView.invalidate();

		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("URLImageProvider", "Callback DL smiley fini - " + uneURL);
		}
	}

}

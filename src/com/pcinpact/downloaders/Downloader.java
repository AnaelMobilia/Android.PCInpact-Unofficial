/*
 * Copyright 2014,2015 Anael Mobilia
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

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import com.pcinpact.Constantes;
import com.pcinpact.R;
import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * T�l�chargement des ressources.
 * 
 * @author Anael
 *
 */
public class Downloader {
	/**
	 * T�l�chargement sans �tre connect� en tant qu'abonn�.
	 * 
	 * @param uneURL URL de la ressource � t�l�charger
	 * @param unContext context de l'application
	 * @param compression true = demander au serveur une compression gzip
	 * @return ressource demand�e brute
	 */
	public static byte[] download(final String uneURL, final Context unContext, final boolean compression) {

		return download(uneURL, unContext, compression, new BasicHttpContext());
	}

	/**
	 * T�l�chargement d'une ressource en tant qu'abonn�.
	 * @param uneURL URL de la ressource � t�l�charger
	 * @param unContext context de l'application
	 * @param compression true = demander au serveur une compression gzip
	 * @param monHTTPContext HTTPcontext contenant le cookie de connexion au compte abonn� NXI
	 * @return ressource demand�e brute
	 */
	public static byte[] download(final String uneURL, final Context unContext, final boolean compression, final HttpContext monHTTPContext) {
		// Retour
		byte[] datas = null;

		// L'utilisateur demande-t-il un debug ?
		Boolean debug = Constantes.getOptionBoolean(unContext, R.string.idOptionDebug, R.bool.defautOptionDebug);

		// Inspir� de http://android-developers.blogspot.de/2010/07/multithreading-for-performance.html
		AndroidHttpClient client = AndroidHttpClient.newInstance(Constantes.getUserAgent(unContext));
		HttpGet getRequest = new HttpGet(uneURL);

		// R�ponse � la requ�te
		HttpEntity entity = null;

		if (compression) {
			// Utilisation d'une compression des datas !
			AndroidHttpClient.modifyRequestToAcceptGzipResponse(getRequest);
		}

		try {
			// Lancement de la requ�te
			HttpResponse response = client.execute(getRequest, monHTTPContext);
			final int statusCode = response.getStatusLine().getStatusCode();

			// Gestion d'un code erreur
			if (statusCode != HttpStatus.SC_OK) {
				// DEBUG
				if (Constantes.DEBUG) {
					Log.e("Downloader", "Erreur " + statusCode + " au dl de " + uneURL);
				}
				// Retour utilisateur ?
				if (debug) {
					Handler handler = new Handler(unContext.getMainLooper());
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast monToast = Toast.makeText(unContext, "[Downloader] Erreur " + statusCode + " pour  " + uneURL,
									Toast.LENGTH_LONG);
							monToast.show();
						}
					});
				}
			} else {
				// Chargement de la r�ponse du serveur
				entity = response.getEntity();

				// R�cup�ration d'un IS degzip� si requis
				InputStream monIS = AndroidHttpClient.getUngzippedContent(entity);
				// Passage en byte[]
				datas = IOUtils.toByteArray(monIS);
				// Fermeture de l'IS
				monIS.close();
			}
		} catch (Exception e) {
			// J'arr�te la requ�te
			getRequest.abort();

			// Retour utilisateur obligatoire : probable probl�me de connexion
			Handler handler = new Handler(unContext.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast monToast = Toast.makeText(unContext, unContext.getString(R.string.chargementPasInternet),
							Toast.LENGTH_LONG);
					monToast.show();
				}
			});

			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("Downloader", "Erreur pour " + uneURL, e);
			}
			// Retour utilisateur ?
			if (debug) {
				handler = new Handler(unContext.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast monToast = Toast.makeText(unContext, "[Downloader] Erreur pour " + uneURL, Toast.LENGTH_LONG);
						monToast.show();
					}
				});
			}
		} finally {
			if (entity != null) {
				// Je vide la requ�te HTTP
				try {
					entity.consumeContent();
				} catch (IOException e) {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.e("Downloader", "entity.consumeContent", e);
					}
				}
			}
			if (client != null) {
				client.close();
			}
		}
		return datas;
	}
}

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

import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;

import com.pcinpact.Constantes;
import com.pcinpact.R;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Connexion au compte abonn� et gestion du DL des articles abonn�s.
 * 
 * @author Anael
 *
 */
public class CompteAbonne {
	/**
	 * Context HTTP.
	 */
	private static BasicHttpContext monHTTPContext;
	/**
	 * Conteneur � cookies.
	 */
	private static BasicCookieStore monCookieStore;
	/**
	 * Dernier utilisateur essay�.
	 */
	private static String usernameLastTry = "";
	/**
	 * Dernier mot de passe essay�.
	 */
	private static String passwordLastTry = "";
	/**
	 * Jeton d'"utilisation en cours".
	 */
	private static Boolean isRunning = false;

	/**
	 * T�l�charge un article "Abonn�".
	 * 
	 * @param uneURL URL de la ressource
	 * @param unContext context de l'application
	 * @param compression faut-il demander au serveur de compresser la ressource ?
	 * @param uniquementSiConnecte dois-je t�l�charger uniquement si le compte abonn� est connect� ?
	 * @return code HTML de l'article brut
	 */
	public static byte[] downloadArticleAbonne(final String uneURL, final Context unContext, final boolean compression,
			final boolean uniquementSiConnecte) {
		// Retour
		byte[] monRetour;

		// Est-ce d�j� en cours d'utilisation ?
		if (!isRunning) {
			// Non : Je me lance !
			isRunning = true;
		} else {
			// Oui : J'attends
			while (isRunning) {
				try {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("compteAbonne", "Attente de la fin d'utilisation pour " + uneURL);
					}

					// Attente de 0 � 1 seconde...
					double monCoeff = Math.random();
					// Evite les r�veils trop simultan�s (les appels l'�tant...)
					int maDuree = (int) (1000 * monCoeff);
					Thread.sleep(maDuree);
				} catch (InterruptedException e) {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.e("compteAbonne", "downloadArticleAbonne : crash sur le sleep", e);
					}
				}
			}
			// Je prends la place !
			isRunning = true;
		}

		// Suis-je connect� ?
		if (estConnecte(unContext)) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.i("compteAbonne", "D�j� connect� => DL authentifi� pour " + uneURL);
			}

			// Je lance le t�l�chargement
			monRetour = Downloader.download(uneURL, unContext, compression, monHTTPContext);
		} else {
			// Non connect�... suis-je connectable ?
			// Chargement des identifiants
			String usernameOption = Constantes.getOptionString(unContext, R.string.idOptionLogin, R.string.defautOptionLogin);
			String passwordOption = Constantes.getOptionString(unContext, R.string.idOptionPassword,
					R.string.defautOptionPassword);
			Boolean isCompteAbonne = Constantes.getOptionBoolean(unContext, R.string.idOptionAbonne, R.bool.defautOptionAbonne);

			// Les options sont-elles bien saisies ? Identifiants d�j� essay�s ?
			if (isCompteAbonne.equals(false) || usernameOption.equals("") || passwordOption.equals("")
					|| (usernameOption.equals(usernameLastTry) && passwordOption.equals(passwordLastTry))) {
				// Quid de la demande ?
				if (uniquementSiConnecte) {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("compteAbonne", "Non connectable => DL non autoris� (NULL) pour " + uneURL);
					}

					monRetour = null;
				} else {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("compteAbonne", "Non connectable => DL non authentifi� pour " + uneURL);
					}

					monRetour = Downloader.download(uneURL, unContext, compression);
				}

				// Information sur l'existance du compte abonn� dans les options
				boolean infoAbonne = Constantes.getOptionBoolean(unContext, R.string.idOptionInfoCompteAbonne,
						R.bool.defautOptionInfoCompteAbonne);

				// Dois-je notifier l'utilisateur ?
				if (infoAbonne) {
					// Affichage d'un toast
					Handler handler = new Handler(unContext.getMainLooper());
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast monToast = Toast.makeText(unContext, unContext.getString(R.string.infoOptionAbonne),
									Toast.LENGTH_LONG);
							monToast.show();
						}
					});

					// Enregistrement de l'affichage
					Constantes.setOptionBoolean(unContext, R.string.idOptionInfoCompteAbonne, false);
				}
			} else {
				// Peut-�tre connectable
				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("compteAbonne", "Lancement de l'authentification pour " + uneURL);
				}

				// Je lance une authentification...
				connexionAbonne(unContext, usernameOption, passwordOption);

				// Je lib�re le jeton d'utilisation
				isRunning = false;

				// Je relance la m�thode pour avoir un r�sultat...
				monRetour = downloadArticleAbonne(uneURL, unContext, compression, uniquementSiConnecte);
			}
		}

		// Je lib�re le jeton d'utilisation
		isRunning = false;

		return monRetour;
	}

	/**
	 * Connexion au compte abonn�.
	 * 
	 * @param unContext context de l'application
	 * @param username nom d'utilisateur NXI
	 * @param password mot de passe NXI
	 */
	private static void connexionAbonne(final Context unContext, final String username, final String password) {
		// Au premier appel, j'initialise le cookie holder
		if (monHTTPContext == null) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("compteAbonne", "cr�ation du HTTPContext");
			}
			// Cr�ation du HTTPContext
			monHTTPContext = new BasicHttpContext();
			// Cr�ation du cookieStore
			monCookieStore = new BasicCookieStore();
			// On conserve les cookies
			monHTTPContext.setAttribute(ClientContext.COOKIE_STORE, monCookieStore);
		}

		// Enregistrement des identifiants "LastTry"
		usernameLastTry = username;
		passwordLastTry = password;

		// Authentification sur NXI
		try {
			// Cr�ation de la requ�te
			AndroidHttpClient client = AndroidHttpClient.newInstance(Constantes.getUserAgent(unContext));
			HttpPost postRequest = new HttpPost(Constantes.AUTHENTIFICATION_URL);

			// Param�tres de la requ�te
			ArrayList<NameValuePair> mesParametres = new ArrayList<NameValuePair>();
			mesParametres.add(new BasicNameValuePair(Constantes.AUTHENTIFICATION_USERNAME, username));
			mesParametres.add(new BasicNameValuePair(Constantes.AUTHENTIFICATION_PASSWORD, password));

			postRequest.setEntity(new UrlEncodedFormEntity(mesParametres));

			// Ex�cution de la requ�te
			HttpResponse response = client.execute(postRequest, monHTTPContext);
			int statusCode = response.getStatusLine().getStatusCode();
			// Fermeture du client
			client.close();

			// Gestion d'un code erreur
			if (statusCode != HttpStatus.SC_OK) {
				// DEBUG
				if (Constantes.DEBUG) {
					Log.e("compteAbonne", "connexionAbonne : Erreur " + statusCode + " lors de l'authentification");
				}
			} else {
				// Ai-je un cookie d'authentification ?
				if (estConnecte(unContext)) {
					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("compteAbonne", "connexionAbonne : Authentification r�ussie (cookie pr�sent)");
					}
				} else {
					// Si non connect�
					Handler handler = new Handler(unContext.getMainLooper());
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast monToast = Toast.makeText(unContext, unContext.getString(R.string.erreurAuthentification),
									Toast.LENGTH_LONG);
							monToast.show();
						}
					});
				}
			}
		} catch (Exception e) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("compteAbonne", "connexionAbonne : Crash sur l'authentification", e);
			}
		}
	}

	/**
	 * Est-on connect� (v�rification du cookie).
	 * 
	 * @param unContext context de l'application
	 * @return true si compte utilisateur connect� chez NXI
	 */
	public static boolean estConnecte(final Context unContext) {
		boolean monRetour = false;

		// Ai-je un cookieHolder ?
		if (monCookieStore != null) {
			/**
			 * V�rification des options
			 */
			// Chargement des identifiants
			String usernameOption = Constantes.getOptionString(unContext, R.string.idOptionLogin, R.string.defautOptionLogin);
			String passwordOption = Constantes.getOptionString(unContext, R.string.idOptionPassword,
					R.string.defautOptionPassword);
			Boolean isCompteAbonne = Constantes.getOptionBoolean(unContext, R.string.idOptionAbonne, R.bool.defautOptionAbonne);

			// Les options sont-elles bien saisies ?
			if (isCompteAbonne.equals(false) || usernameOption.equals("") || passwordOption.equals("")) {
				// Si non, effacement des cookies
				monCookieStore.clear();
			} else {
				// Si oui, je cherche mon cookie...
				// Suppression des cookies expir�s
				monCookieStore.clearExpired(new Date());

				// Ai-je le cookie demand� ?
				for (Cookie unCookie : monCookieStore.getCookies()) {
					// Est-le bon cookie ?
					if (unCookie.getName().equals(Constantes.AUTHENTIFICATION_COOKIE)) {
						monRetour = true;
						// Pas besoin d'aller plus loin !
						break;
					}
				}
			}
		}
		return monRetour;
	}
}

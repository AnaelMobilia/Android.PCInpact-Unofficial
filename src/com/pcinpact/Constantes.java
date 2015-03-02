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
package com.pcinpact;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Constantes et outils.
 * 
 * @author Anael
 *
 */
public class Constantes {
	/**
	 * MODE DEBUG.
	 */
	public static final Boolean DEBUG = false;
	/**
	 * COMPATIBILITE.
	 */
	/**
	 * Build Version Honeycomb (non dispo en 2.*).
	 * http://developer.android.com/reference/android/os/Build.VERSION_CODES.html
	 */
	public static final int HONEYCOMB = 11;

	/**
	 * PARAMETRES GENERAUX
	 */
	/**
	 * Encodage des pages.
	 */
	public static final String NEXT_INPACT_ENCODAGE = "UTF-8";
	/**
	 * URL de t�l�chargement.
	 */
	public static final String NEXT_INPACT_URL = "http://m.nextinpact.com";
	/**
	 * Param�tre num�ro de page (liste articles).
	 */
	public static final String NEXT_INPACT_URL_NUM_PAGE = NEXT_INPACT_URL + "/?page=";
	/**
	 * URL de t�l�chargement des commentaires.
	 */
	public static final String NEXT_INPACT_URL_COMMENTAIRES = NEXT_INPACT_URL + "/comment/";
	/**
	 * Param�tre ID d'article (commentaires).
	 */
	public static final String NEXT_INPACT_URL_COMMENTAIRES_PARAM_ARTICLE_ID = "newsId";
	/**
	 * Param�tre num�ro de page (commentaires).
	 */	
	public static final String NEXT_INPACT_URL_COMMENTAIRES_PARAM_NUM_PAGE = "page";
	/**
	 * URL des smileys.
	 */
	public static final String NEXT_INPACT_URL_SMILEYS = "http://cloudstatic.pcinpact.com/smileys/";
	/**
	 * Nb de commentaires par page.
	 */
	public static final int NB_COMMENTAIRES_PAR_PAGE = 10;
	/**
	 * Nb d'articles par page.
	 */
	public static final int NB_ARTICLES_PAR_PAGE = 30;
	/**
	 * Utilisation d'une compression pour les contenus textes.
	 */
	public static final Boolean COMPRESSION_CONTENU_TEXTES = true;
	/**
	 * Utilisation d'une compression pour les contenus image.
	 */
	public static final Boolean COMPRESSION_CONTENU_IMAGES = false;

	/**
	 * TYPES DE TELECHARGEMENTS.
	 */
	/**
	 * Type : liste des articles.
	 */
	public static final int HTML_LISTE_ARTICLES = 1;
	/**
	 * Type : contenu d'un article.
	 */
	public static final int HTML_ARTICLE = 2;
	/**
	 * Type : commentaires.
	 */
	public static final int HTML_COMMENTAIRES = 3;
	/**
	 * Type : image -> miniature.
	 */
	public static final int IMAGE_MINIATURE_ARTICLE = 4;
	/**
	 * Type : image -> du contenu d'un article.
	 */
	public static final int IMAGE_CONTENU_ARTICLE = 5;
	/**
	 * Type : image -> smiley dans commentaires.
	 */
	public static final int IMAGE_SMILEY = 6;

	/**
	 * FORMATS DU SITE POUR LE PARSEUR.
	 */
	/**
	 * Format des dates des articles sur le site.
	 */
	public static final String FORMAT_DATE_ARTICLE = "dd/MM/yyyy HH:mm:ss";
	/**
	 * Format des dates des commentaires sur le site.
	 */
	public static final String FORMAT_DATE_COMMENTAIRE = "'le' dd/MM/yyyy '�' HH:mm:ss";

	/**
	 * PATH DES FICHIERS LOCAUX.
	 */
	/**
	 * Path des miniatures des articles.
	 */
	public static final String PATH_IMAGES_MINIATURES = "/MINIATURES/";
	/**
	 * Path des images de contenu des articles. 
	 */
	public static final String PATH_IMAGES_ILLUSTRATIONS = "/ILLUSTRATIONS/";
	/**
	 * Path des smileys.
	 */
	public static final String PATH_IMAGES_SMILEYS = "/SMILEYS/";

	/**
	 * FORMATS D'AFFICHAGE.
	 */
	/**
	 * Date des sections sur la listeArticlesActivity.
	 */
	public static final String FORMAT_AFFICHAGE_SECTION_DATE = "EEEE dd MMMM yyyy";
	/**
	 * Heure de publication des articles sur la listeArticlesActivity.
	 */
	public static final String FORMAT_AFFICHAGE_ARTICLE_HEURE = "HH:mm";
	/**
	 * Date et Heure de publication d'un commentaire.
	 */
	public static final String FORMAT_AFFICHAGE_COMMENTAIRE_DATE_HEURE = FORMAT_DATE_COMMENTAIRE;
	/**
	 * Date et Heure de derni�re synchro.
	 */
	public static final String FORMAT_DATE_DERNIER_REFRESH = "dd MMM '�' HH:mm";

	/**
	 * CONSTANTES EN BDD.
	 */
	/**
	 * ID du refresh de la liste des articles.
	 */
	public static final int DB_REFRESH_ID_LISTE_ARTICLES = 0;

	/**
	 * TAILLE DES TEXTES.
	 * http://developer.android.com/design/style/typography.html
	 */
	/**
	 * Taille de texte MICRO.
	 */
	public static final int TEXT_SIZE_MICRO = 12;
	/**
	 * Taille de texte SMALL.
	 */	
	public static final int TEXT_SIZE_SMALL = 14;
	/**
	 * Taille de texte MEDIUM.
	 */
	public static final int TEXT_SIZE_MEDIUM = 18;
	/**
	 * Taille de texte LARGE.
	 */
	public static final int TEXT_SIZE_LARGE = 22;
	/**
	 * Taille de texte XLARGE.
	 */
	public static final int TEXT_SIZE_XLARGE = 26;

	/**
	 * COULEURS D'AFFICHAGE.
	 */
	/**
	 * Couleur de fond - article non lu.
	 */
	public static final int COULEUR_ARTICLE_NON_LU = Color.WHITE;
	/**
	 * Couleur de fond - article lu.
	 */
	public static final int COULEUR_ARTICLE_LU = Color.parseColor("#D3D3D3");

	/**
	 * PARAMETRES D'AUTHENTIFICATION.
	 */
	/**
	 * URL d'authentification.
	 */
	public static final String AUTHENTIFICATION_URL = NEXT_INPACT_URL + "/Account/LogOn";
	/**
	 * Param�tre utilisateur.
	 */
	public static final String AUTHENTIFICATION_USERNAME = "UserName";
	/**
	 * Param�tre mot de passe.
	 */
	public static final String AUTHENTIFICATION_PASSWORD = "Password";
	/**
	 * Nom du cookie d'authentification.
	 */
	public static final String AUTHENTIFICATION_COOKIE = "inpactstore";

	/**
	 * USER AGENT.
	 */
	private static final String USER_AGENT = "NextInpact (Unofficial) v";

	/**
	 * User agent pour les requ�tes r�seau.
	 * @param unContext context de l'application
	 * @return User-Agent
	 */
	public static String getUserAgent(Context unContext) {
		// Num�ro de version de l'application
		String numVersion = "";
		try {
			PackageInfo pInfo = unContext.getPackageManager().getPackageInfo(unContext.getPackageName(), 0);
			numVersion = pInfo.versionName;
			if (Constantes.DEBUG) {
				numVersion += " DEV";
			}
		} catch (Exception e) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("Constantes", "Erreur � la r�solution du n� de version", e);
			}
		}

		return USER_AGENT + numVersion;
	}

	/**
	 * Retourne une option de type String.
	 * @param unContext context d'application
	 * @param idOption id de l'option
	 * @param defautOption id de la valeur par d�faut de l'option
	 * @return l'option demand�e
	 */
	public static String getOptionString(Context unContext, int idOption, int defautOption) {
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

		return mesPrefs.getString(unContext.getString(idOption), unContext.getString(defautOption));
	}

	/**
	 * Retourne une option de type Boolean.
	 * 
	 * @param unContext context d'application
	 * @param idOption id de l'option
	 * @param defautOption id de la valeur par d�faut de l'option
	 * @return @return l'option demand�e
	 */
	public static Boolean getOptionBoolean(Context unContext, int idOption, int defautOption) {
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

		return mesPrefs.getBoolean(unContext.getString(idOption), unContext.getResources().getBoolean(defautOption));
	}

	/**
	 * Retourne une option de type int.
	 * 
	 * @param unContext context d'application
	 * @param idOption id de l'option
	 * @param defautOption id de la valeur par d�faut de l'option
	 * @return @return l'option demand�e
	 */
	public static int getOptionInt(Context unContext, int idOption, int defautOption) {
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

		return Integer
				.valueOf(mesPrefs.getString(unContext.getString(idOption), unContext.getResources().getString(defautOption)));
	}

	/**
	 * Enregistre un boolean dans les pr�f�rences.
	 * 
	 * @param unContext context d'application
	 * @param idOption id de l'option
	 * @param valeurOption valeur � enregistrer
	 */
	public static void setOptionBoolean(Context unContext, int idOption, boolean valeurOption) {
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

		Editor editor = mesPrefs.edit();
		editor.putBoolean(unContext.getString(idOption), valeurOption);
		editor.commit();
	}
}
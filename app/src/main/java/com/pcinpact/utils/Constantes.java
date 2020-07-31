/*
 * Copyright 2013 - 2020 Anael Mobilia and contributors
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
package com.pcinpact.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

/**
 * Constantes et outils.
 *
 * @author Anael
 */
public class Constantes {
    /**
     * MODE DEBUG.
     */
    public static final Boolean DEBUG = true;
    /**
     * Contact du développeur
     */
    public static final String MAIL_DEVELOPPEUR = "contrib@anael.eu";


    /*
     * PARAMETRES GENERAUX
     */
    /**
     * Locale à utiliser pour les timestamp
     */
    public static final Locale LOCALE = Locale.FRANCE;
    /**
     * Encodage des pages.
     */
    public static final String NEXT_INPACT_ENCODAGE = "UTF-8";
    /**
     * URL de téléchargement NXI.
     */
    //public static final String NEXT_INPACT_URL = "https://api-v1-beta.nextinpact.com/api/v1/";
    /**
     * URL de téléchargement INPACT-HARDWARE.
     */
    public static final String NEXT_INPACT_URL = "https://api-v1.inpact-hardware.com/api/v1/";
    /**
     * Paramètre numéro de page (liste articles).
     */
    public static final String NEXT_INPACT_URL_NUM_PAGE = NEXT_INPACT_URL + "SimpleContent/list?page=";
    /**
     * URL de téléchargement des commentaires.
     */
    public static final String NEXT_INPACT_URL_COMMENTAIRES = NEXT_INPACT_URL + "Commentaire/list";
    /**
     * URL d'authentification.
     */
    public static final String AUTHENTIFICATION_URL = NEXT_INPACT_URL + "/Account/LogOn";
    /**
     * Paramètre ID d'article (commentaires).
     */
    public static final String NEXT_INPACT_URL_COMMENTAIRES_PARAM_ARTICLE_ID = "ArticleId";
    /**
     * Paramètre numéro de page (commentaires).
     */
    public static final String NEXT_INPACT_URL_COMMENTAIRES_PARAM_NUM_PAGE = "page";
    /**
     * Timeout pour les téléchargements (en ms) - default = ~250000.
     */
    public static final int TIMEOUT = 15000;
    /**
     * Balise HTML pour les citations de commentaires
     */
    public static final String TAG_HTML_QUOTE = "myquote";

    /**
     * Nb de commentaires par page.
     */
    public static final int NB_COMMENTAIRES_PAR_PAGE = 10;

    /**
     * Nb d'articles par page.
     */
    public static final int NB_ARTICLES_PAR_PAGE = 30;


    /*
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


    /*
     * FORMATS DU SITE POUR LE PARSEUR.
     */
    /**
     * Format des dates sur le site.
     */
    public static final String FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS";
    /**
     * Date et heure de publication d'un commentaire.
     */
    public static final String FORMAT_AFFICHAGE_COMMENTAIRE_DATE_HEURE = "'le' dd/MM/yyyy 'à' HH:mm:ss";

    /*
     * PATH DES FICHIERS LOCAUX -- Conservation pour l'effacement en v2.4.0
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


    /*
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
     * Date et Heure de dernière synchro.
     */
    public static final String FORMAT_DATE_DERNIER_REFRESH = "dd MMM 'à' HH:mm";


    /*
     * CONSTANTES EN BDD.
     */
    /**
     * ID du refresh de la liste des articles.
     */
    public static final int DB_REFRESH_ID_LISTE_ARTICLES = 0;


    /*
     * TAILLE DES TEXTES. http://developer.android.com/design/style/typography.html
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


    /*
     * PARAMETRES D'AUTHENTIFICATION.
     */
    /**
     * Paramètre utilisateur.
     */
    public static final String AUTHENTIFICATION_USERNAME = "UserName";
    /**
     * Paramètre mot de passe.
     */
    public static final String AUTHENTIFICATION_PASSWORD = "Password";
    /**
     * Nom du cookie d'authentification.
     */
    public static final String AUTHENTIFICATION_COOKIE = "__RequestVerificationToken";
    /**
     * USER AGENT.
     */
    private static final String USER_AGENT = "NextInpact (Unofficial) v";

    /**
     * User agent pour les Requêtes réseau.
     *
     * @param unContext context de l'application
     * @return User-Agent
     */
    public static String getUserAgent(final Context unContext) {
        // Numéro de version de l'application
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
                Log.e("Constantes", "getUserAgent() - Erreur à la résolution du n° de version", e);
            }
        }

        return USER_AGENT + numVersion;
    }

    /**
     * Retourne une option de type String.
     *
     * @param unContext    context d'application
     * @param idOption     id de l'option
     * @param defautOption id de la valeur par défaut de l'option
     * @return l'option demandée
     */
    public static String getOptionString(final Context unContext, final int idOption, final int defautOption) {
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        return mesPrefs.getString(unContext.getString(idOption), unContext.getString(defautOption));
    }

    /**
     * Retourne une option de type Boolean.
     *
     * @param unContext    context d'application
     * @param idOption     id de l'option
     * @param defautOption id de la valeur par défaut de l'option
     * @return l'option demandée
     */
    public static Boolean getOptionBoolean(final Context unContext, final int idOption, final int defautOption) {
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        return mesPrefs.getBoolean(unContext.getString(idOption), unContext.getResources().getBoolean(defautOption));
    }

    /**
     * Retourne une option de type int.
     *
     * @param unContext    context d'application
     * @param idOption     id de l'option
     * @param defautOption id de la valeur par défaut de l'option
     * @return l'option demandée
     */
    public static int getOptionInt(final Context unContext, final int idOption, final int defautOption) {
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        return Integer.parseInt(
                mesPrefs.getString(unContext.getString(idOption), unContext.getResources().getString(defautOption)));
    }

    /**
     * Enregistre un boolean dans les préférences.
     *
     * @param unContext    context d'application
     * @param idOption     id de l'option
     * @param valeurOption valeur à enregistrer
     */
    public static void setOptionBoolean(final Context unContext, final int idOption, final boolean valeurOption) {
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        Editor editor = mesPrefs.edit();
        editor.putBoolean(unContext.getString(idOption), valeurOption);
        editor.apply();
    }

    /**
     * Enregistre un int dans les préférences.
     *
     * @param unContext    context d'application
     * @param idOption     id de l'option
     * @param valeurOption valeur à enregistrer
     */
    public static void setOptionInt(final Context unContext, final int idOption, final String valeurOption) {
        SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(unContext);

        Editor editor = mesPrefs.edit();
        editor.putString(unContext.getString(idOption), valeurOption);
        editor.apply();
    }
}
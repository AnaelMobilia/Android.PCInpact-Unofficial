/*
 * Copyright 2014, 2015 Anael Mobilia
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
import java.io.IOException;
import java.util.ArrayList;

import com.pcinpact.Constantes;
import com.pcinpact.database.DAO;
import com.pcinpact.items.Item;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.CommentaireItem;
import com.pcinpact.parseur.ParseurHTML;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * T�l�chargement du code HTML
 * 
 * @author Anael
 *
 */
public class AsyncHTMLDownloader extends AsyncTask<String, Void, ArrayList<Item>> {
	// Contexte parent
	private Context monContext;
	// Callback : parent + ref
	private RefreshDisplayInterface monParent;
	// Type & URL du code HTML
	private String urlPage;
	private int typeHTML;
	// Acc�s sur la DB
	private DAO monDAO;

	public AsyncHTMLDownloader(Context unContext, RefreshDisplayInterface parent, int unType, String uneURL, DAO unDAO) {
		// Mappage des attributs de cette requ�te
		monContext = unContext;
		monParent = parent;
		urlPage = uneURL;
		typeHTML = unType;
		monDAO = unDAO;
		if (Constantes.DEBUG) {
			Log.w("AsyncHTMLDownloader", urlPage);
		}
	}

	@Override
	protected ArrayList<Item> doInBackground(String... params) {
		// Retour
		ArrayList<Item> mesItems = new ArrayList<Item>();

		// Je r�cup�re mon contenu HTML
		ByteArrayOutputStream monBAOS = Downloader.download(urlPage);

		// Erreur de t�l�chargement : retour d'un fallback et pas d'enregistrement
		if (monBAOS == null) {
			return mesItems;
		}

		// Je prend mon contenu
		String monInput = monBAOS.toString();
		// Et ferme le BAOS
		try {
			monBAOS.close();
		} catch (IOException e1) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("AsyncImageDownloader", "Erreur � la fermeture du BAOS", e1);
			}
		}

		// J'ouvre une instance du parser
		ParseurHTML monParser = new ParseurHTML(monContext);

		switch (typeHTML) {
			case Constantes.HTML_LISTE_ARTICLES:
				// Je passe par le parser
				ArrayList<ArticleItem> monRetour = monParser.getListeArticles(monInput, urlPage);

				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("AsyncHTMLDownloader", "HTML_LISTE_ARTICLES : le parseur � retourn� " + monRetour.size() + " r�sultats");
				}

				// Je ne conserve que les nouveaux articles
				for (Item unItem : monRetour) {
					// Stockage en BDD
					if (monDAO.enregistrerArticleSiNouveau((ArticleItem) unItem)) {
						// Ne retourne que les nouveaux articles
						mesItems.add(unItem);
					}
				}
				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("AsyncHTMLDownloader", "Au final, " + mesItems.size() + " r�sultats");
				}
				break;

			case Constantes.HTML_ARTICLE:
				// Je passe par le parser
				ArticleItem articleParser = monParser.getArticle(monInput, urlPage);

				// Chargement de l'article depuis la BDD
				ArticleItem articleDB = monDAO.chargerArticle(articleParser.getID());

				// Ajout du contenu � l'objet charg�
				articleDB.setContenu(articleParser.getContenu());

				// Enregistrement de l'objet complet
				monDAO.enregistrerArticle(articleDB);

				// Pour le retour � l'utilisateur...
				mesItems.add(articleDB);
				break;

			case Constantes.HTML_COMMENTAIRES:
				// Je passe par le parser
				ArrayList<CommentaireItem> lesCommentaires = monParser.getCommentaires(monInput, urlPage);

				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("AsyncHTMLDownloader", "HTML_COMMENTAIRES : le parseur � retourn� " + lesCommentaires.size()
							+ " r�sultats");
				}

				// Je ne conserve que les nouveaux commentaires
				for (Item unItem : lesCommentaires) {
					// Stockage en BDD
					if (monDAO.enregistrerCommentaireSiNouveau((CommentaireItem) unItem)) {
						// Ne retourne que les nouveaux articles
						mesItems.add(unItem);
					}
				}
				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("AsyncHTMLDownloader", "HTML_COMMENTAIRES : Au final, " + mesItems.size() + " r�sultats");
				}
				break;

			default:
				if (Constantes.DEBUG) {
					Log.e("AsyncHTMLDownloader", "Type HTML incoh�rent : " + typeHTML + " - URL : " + urlPage);
				}
				break;
		}
		return mesItems;
	}

	@Override
	protected void onPostExecute(ArrayList<Item> result) {
		monParent.downloadHTMLFini(urlPage, result);
	}
}

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
package com.pcinpact.network;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.pcinpact.Constantes;
import com.pcinpact.datastorage.DAO;
import com.pcinpact.items.Item;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.CommentaireItem;
import com.pcinpact.parseur.ParseurHTML;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * T�l�chargement du code HTML.
 * 
 * @author Anael
 *
 */
public class AsyncHTMLDownloader extends AsyncTask<String, Void, ArrayList<? extends Item>> {
	/**
	 * Parent qui sera rappel� � la fin.
	 */
	private RefreshDisplayInterface monParent;
	/**
	 * URL de la page.
	 */
	private String urlPage;
	/**
	 * Type de la ressource.
	 */
	private int typeHTML;
	/**
	 * Acc�s � la BDD.
	 */
	private DAO monDAO;
	/**
	 * Context de l'application.
	 */
	private Context monContext;
	/**
	 * Est-ce du contenu abonn� ?
	 */
	private Boolean isAbonne = false;
	/**
	 * T�l�chargement uniquement si connect� ?
	 */
	private Boolean uniquementSiConnecte = false;

	/**
	 * DL avec gestion du compte abonn� et de l'�tat de la connexion.
	 * 
	 * @param parent parent � callback � la fin
	 * @param unType type de la ressource (Cf Constantes.TYPE_)
	 * @param uneURL URL de la ressource
	 * @param unDAO acc�s sur la DB
	 * @param unContext context de l'application
	 * @param contenuAbonne est-ce un contenu abonn� ?
	 * @param onlyifConnecte dois-je t�l�charger uniquement si le compte abonn� est connect� ?
	 */
	public AsyncHTMLDownloader(final RefreshDisplayInterface parent, final int unType, final String uneURL, final DAO unDAO,
			final Context unContext, final Boolean contenuAbonne, final Boolean onlyifConnecte) {
		// Mappage des attributs de cette requ�te
		monParent = parent;
		urlPage = uneURL;
		typeHTML = unType;
		monDAO = unDAO;
		monContext = unContext.getApplicationContext();
		isAbonne = contenuAbonne;
		uniquementSiConnecte = onlyifConnecte;

		// DEBUG
		if (Constantes.DEBUG) {
			Log.w("AsyncHTMLDownloader", "(Abonn�) " + urlPage + " - Uniquement si connect� : " + onlyifConnecte.toString());
		}
	}

	/**
	 * DL sans gestion du statut abonn�.
	 * 
	 * @param parent parent � callback � la fin
	 * @param unType type de la ressource (Cf Constantes.TYPE_)
	 * @param uneURL URL de la ressource
	 * @param unDAO acc�s sur la DB
	 * @param unContext context de l'application
	 */
	public AsyncHTMLDownloader(final RefreshDisplayInterface parent, final int unType, final String uneURL, final DAO unDAO,
			final Context unContext) {
		// Mappage des attributs de cette requ�te
		monParent = parent;
		urlPage = uneURL;
		typeHTML = unType;
		monDAO = unDAO;
		monContext = unContext;

		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("AsyncHTMLDownloader", "(NON Abonn�) " + urlPage);
		}
	}

	@Override
	protected ArrayList<Item> doInBackground(String... params) {
		// Retour
		ArrayList<Item> mesItems = new ArrayList<Item>();

		try {
			// Date du refresh
			long dateRefresh = new Date().getTime();

			// Retour du Downloader
			byte[] datas;
			if (isAbonne) {
				// Je r�cup�re mon contenu HTML en passant par la partie abonn�
				datas = CompteAbonne.downloadArticleAbonne(urlPage, monContext, Constantes.COMPRESSION_CONTENU_TEXTES,
						uniquementSiConnecte);
			} else {
				// Je r�cup�re mon contenu HTML directement
				datas = Downloader.download(urlPage, monContext, Constantes.COMPRESSION_CONTENU_TEXTES);
			}

			// V�rifie que j'ai bien un retour (vs erreur DL)
			if (datas != null) {
				// Je convertis mon byte[] en String
				String contenu = IOUtils.toString(datas, Constantes.NEXT_INPACT_ENCODAGE);

				switch (typeHTML) {
					case Constantes.HTML_LISTE_ARTICLES:
						// Je passe par le parser
						ArrayList<ArticleItem> monRetour = ParseurHTML.getListeArticles(contenu, urlPage);

						// DEBUG
						if (Constantes.DEBUG) {
							Log.i("AsyncHTMLDownloader", "HTML_LISTE_ARTICLES : le parseur � retourn� " + monRetour.size()
									+ " r�sultats");
						}

						// Je ne conserve que les nouveaux articles
						for (ArticleItem unArticle : monRetour) {
							// Stockage en BDD
							if (monDAO.enregistrerArticleSiNouveau(unArticle)) {
								// Ne retourne que les nouveaux articles
								mesItems.add(unArticle);
							}
						}

						// M�J de la date de M�J uniquement si DL de la premi�re page (�vite plusieurs m�j si dl de plusieurs
						// pages)
						if (urlPage.equals(Constantes.NEXT_INPACT_URL_NUM_PAGE + "1")) {
							// Mise � jour de la date de rafraichissement
							monDAO.enregistrerDateRefresh(Constantes.DB_REFRESH_ID_LISTE_ARTICLES, dateRefresh);
						}

						// DEBUG
						if (Constantes.DEBUG) {
							Log.i("AsyncHTMLDownloader", "Au final, " + mesItems.size() + " r�sultats");
						}
						break;

					case Constantes.HTML_ARTICLE:
						// Je passe par le parser
						ArticleItem articleParser = ParseurHTML.getArticle(contenu, urlPage);

						// Chargement de l'article depuis la BDD
						ArticleItem articleDB = monDAO.chargerArticle(articleParser.getId());

						// Ajout du contenu � l'objet charg�
						articleDB.setContenu(articleParser.getContenu());

						// Article abonn� ?
						if (articleDB.isAbonne()) {
							// Suis-je connect� ?
							boolean etatAbonne = CompteAbonne.estConnecte(monContext);

							// Suis-je connect� ?
							articleDB.setDlContenuAbonne(etatAbonne);
						}

						// Je viens de DL l'article => non lu
						articleDB.setLu(false);

						// Enregistrement de l'objet complet
						monDAO.enregistrerArticle(articleDB);

						// pas de retour � l'utilisateur, il s'agit d'un simple DL
						break;

					case Constantes.HTML_COMMENTAIRES:
						/**
						 * M�J des commentaires
						 */
						// Je passe par le parser
						ArrayList<CommentaireItem> lesCommentaires = ParseurHTML.getCommentaires(contenu, urlPage);

						// DEBUG
						if (Constantes.DEBUG) {
							Log.i("AsyncHTMLDownloader", "HTML_COMMENTAIRES : le parseur � retourn� " + lesCommentaires.size()
									+ " r�sultats");
						}

						// Je ne conserve que les nouveaux commentaires
						for (CommentaireItem unCommentaire : lesCommentaires) {
							// Stockage en BDD
							if (monDAO.enregistrerCommentaireSiNouveau(unCommentaire)) {
								// Ne retourne que les nouveaux articles
								mesItems.add(unCommentaire);
							}
						}
						// Calcul de l'ID de l'article concern� (entre "newsId=" et "&page=")
						int debut = urlPage.indexOf(Constantes.NEXT_INPACT_URL_COMMENTAIRES_PARAM_ARTICLE_ID + "=");
						debut += Constantes.NEXT_INPACT_URL_COMMENTAIRES_PARAM_ARTICLE_ID.length() + 1;
						int fin = urlPage.indexOf("&");
						int idArticle = Integer.valueOf(urlPage.substring(debut, fin));

						// Mise � jour de la date de rafraichissement
						monDAO.enregistrerDateRefresh(idArticle, dateRefresh);

						/**
						 * M�J du nombre de commentaires
						 */
						int nbCommentaires = ParseurHTML.getNbCommentaires(contenu, urlPage);
						monDAO.updateNbCommentairesArticle(idArticle, nbCommentaires);

						// DEBUG
						if (Constantes.DEBUG) {
							Log.i("AsyncHTMLDownloader", "HTML_COMMENTAIRES : Au final, " + mesItems.size() + " r�sultats");
						}
						break;

					default:
						if (Constantes.DEBUG) {
							Log.e("AsyncHTMLDownloader", "Type HTML incoh�rent : " + typeHTML + " - URL : " + urlPage);
						}
						break;
				}
			} else {
				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("AsyncHTMLDownloader",
							"contenu NULL pour " + urlPage + " - abonneUniquement = " + uniquementSiConnecte.toString());
				}
			}
		} catch (Exception e) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("AsyncHTMLDownloader", "Crash doInBackground", e);
			}
		}
		return mesItems;
	}

	@Override
	protected void onPostExecute(ArrayList<? extends Item> result) {
		try {
			monParent.downloadHTMLFini(urlPage, result);
		} catch (Exception e) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("AsyncHTMLDownloader", "Crash onPostExecute", e);
			}
		}
	}
}

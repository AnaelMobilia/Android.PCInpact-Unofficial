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
package com.pcinpact;

import java.util.ArrayList;
import java.util.Collections;

import com.pcinpact.adapters.ItemsAdapter;
import com.pcinpact.database.DAO;
import com.pcinpact.downloaders.AsyncHTMLDownloader;
import com.pcinpact.downloaders.RefreshDisplayInterface;
import com.pcinpact.items.CommentaireItem;
import com.pcinpact.items.Item;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;

public class CommentairesActivity extends ActionBarActivity implements RefreshDisplayInterface {
	// les commentaires
	private ArrayList<CommentaireItem> mesCommentaires = new ArrayList<CommentaireItem>();
	// ID de l'article
	private int articleID;
	// itemsAdapter
	private ItemsAdapter monItemsAdapter;
	// La BDD
	private DAO monDAO;
	// Etats
	private Boolean isLoading = false;
	private Boolean isFinCommentaires = false;

	// Ressources sur les �l�ments graphiques
	private Menu monMenu;
	private ListView monListView;
	private Button buttonDl10Commentaires;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Partie graphique
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.commentaires);
		setSupportProgressBarIndeterminateVisibility(false);

		// Liste des commentaires
		monListView = (ListView) this.findViewById(R.id.listeCommentaires);
		// Footer : bouton "Charger plus de commentaires"
		buttonDl10Commentaires = new Button(this);
		buttonDl10Commentaires.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// T�l�chargement de 10 commentaires en plus
				refreshListeCommentaires();
			}
		});
		buttonDl10Commentaires.setText(getResources().getString(R.string.commentairesPlusDeCommentaires));
		monListView.addFooterView(buttonDl10Commentaires);

		// Adapter pour l'affichage des donn�es
		monItemsAdapter = new ItemsAdapter(getApplicationContext(), new ArrayList<Item>());
		monListView.setAdapter(monItemsAdapter);

		// ID de l'article concern�
		articleID = getIntent().getExtras().getInt("ARTICLE_ID");

		// J'active la BDD
		monDAO = DAO.getInstance(getApplicationContext());
		// Je charge mes articles
		mesCommentaires.addAll(monDAO.chargerCommentairesTriParDate(articleID));
		// Mise � jour de l'affichage
		monItemsAdapter.updateListeItems(mesCommentaires);

		// Syst�me de rafraichissement de la vue
		monListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// J'affiche le dernier commentaire en cache ?
				if ((firstVisibleItem + visibleItemCount) >= ((totalItemCount - 1))) {
					// (# du 1er commentaire affich� + nb d'items affich�s) == (nb total d'item dan la liste - [bouton footer])

					// Chargement des pr�f�rences de l'utilisateur
					final SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
					// T�l�chargement automatique en continu des commentaires ?
					Boolean telecharger = mesPrefs.getBoolean(getString(R.string.idOptionCommentairesTelechargementContinu),
							getResources().getBoolean(R.bool.defautOptionCommentairesTelechargementContinu));

					// Si l'utilisateur le veut && je ne t�l�charge pas d�j� && la fin des commentaires n'est pas atteinte
					if (telecharger && !isLoading && !isFinCommentaires) {
						// T�l�chargement de 10 commentaires en plus
						refreshListeCommentaires();
					}
				}

			}
		});
	}

	/**
	 * Charge les commentaires suivants
	 */
	@SuppressLint("NewApi")
	private void refreshListeCommentaires() {
		// M�J des graphismes
		lancerAnimationTelechargement();

		int idDernierCommentaire = 0;
		// Si j'ai des commentaires, je r�cup�re l'ID du dernier dans la liste
		if (mesCommentaires.size() > 0) {
			CommentaireItem lastCommentaire = mesCommentaires.get(mesCommentaires.size() - 1);
			idDernierCommentaire = lastCommentaire.getID();
		}

		// Le cast en int supprime la partie apr�s la virgule
		int maPage = (int) Math.floor((idDernierCommentaire / Constantes.NB_COMMENTAIRES_PAR_PAGE) + 1);

		// Cr�ation de l'URL
		String monURL = Constantes.NEXT_INPACT_URL_COMMENTAIRES + "?" + Constantes.NEXT_INPACT_URL_COMMENTAIRES_PARAM_ARTICLE_ID
				+ "=" + articleID + "&" + Constantes.NEXT_INPACT_URL_COMMENTAIRES_PARAM_NUM_PAGE + "=" + maPage;

		// Ma t�che de DL
		AsyncHTMLDownloader monAHD = new AsyncHTMLDownloader(getApplicationContext(), this, Constantes.HTML_COMMENTAIRES, monURL,
				monDAO);
		// Parall�lisation des t�l�chargements pour l'ensemble de l'application
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			monAHD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			monAHD.execute();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Je garde le menu sous la main
		monMenu = menu;

		// Je charge mon menu dans l'actionBar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.commentaires_activity_actions, menu);

		// Ticket #86 : un chargement automatique a-t-il lieu (sera lanc� avant de cr�er le menu)
		if (isLoading) {
			// Je fait coincider les animations avec l'�tat r�el
			lancerAnimationTelechargement();
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem pItem) {
		switch (pItem.getItemId()) {
		// Retour
			case R.id.action_home:
				finish();
				Intent i = new Intent(getApplicationContext(), ListeArticlesActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				return true;

				// Rafraichir la liste des commentaires
			case R.id.action_refresh:
				refreshListeCommentaires();
				return true;

			default:
				return super.onOptionsItemSelected(pItem);
		}
	}

	/**
	 * Lance les animations indiquant un t�l�chargement
	 */
	private void lancerAnimationTelechargement() {
		// J'enregistre l'�tat
		isLoading = true;

		// Lance la rotation du logo dans le header
		setSupportProgressBarIndeterminateVisibility(true);

		// Supprime l'ic�ne refresh dans le header
		if (monMenu != null)
			monMenu.findItem(R.id.action_refresh).setVisible(false);

		// M�J du bouton du footer
		buttonDl10Commentaires.setText(getString(R.string.commentairesChargement));
	}

	/**
	 * Arr�t les animations indiquant un t�l�chargement
	 */
	private void arreterAnimationTelechargement() {
		// J'enregistre l'�tat
		isLoading = false;

		// Arr�t de la rotation du logo dans le header
		setSupportProgressBarIndeterminateVisibility(false);

		// Affiche l'ic�ne refresh dans le header
		if (monMenu != null)
			monMenu.findItem(R.id.action_refresh).setVisible(true);

		// M�J du bouton du footer
		buttonDl10Commentaires.setText(getString(R.string.commentairesPlusDeCommentaires));
	}

	@Override
	public void downloadHTMLFini(String uneURL, ArrayList<Item> desItems) {
		// J'enregistre en m�moire les nouveaux commentaires
		for (Item unItem : desItems) {
			// Je l'enregistre en m�moire
			mesCommentaires.add((CommentaireItem) unItem);
		}
		// Tri des commentaires par ID
		Collections.sort(mesCommentaires);

		// Arr�t des gris-gris en GUI
		arreterAnimationTelechargement();

		// Je met � jour les donn�es
		monItemsAdapter.updateListeItems(mesCommentaires);
		// Je notifie le changement pour un rafraichissement du contenu
		monItemsAdapter.notifyDataSetChanged();

	}

	@Override
	public void downloadImageFini(String uneURL, Bitmap uneImage) {
		// TODO Auto-generated method stub
	}

}

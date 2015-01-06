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

import java.util.ArrayList;
import java.util.Collections;

import com.pcinpact.adapters.ItemsAdapter;
import com.pcinpact.database.DAO;
import com.pcinpact.downloaders.AsyncHTMLDownloader;
import com.pcinpact.downloaders.AsyncImageDownloader;
import com.pcinpact.downloaders.RefreshDisplayInterface;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.Item;
import com.pcinpact.items.SectionItem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ListeArticlesActivity extends ActionBarActivity implements RefreshDisplayInterface, OnItemClickListener {
	// les articles
	ArrayList<ArticleItem> mesArticles = new ArrayList<ArticleItem>();
	// itemAdapter
	ItemsAdapter monItemsAdapter;
	// La BDD
	DAO monDAO;
	// Nombre de DL en cours
	int DLinProgress = 0;

	// Ressources sur les �l�ments graphiques
	Menu monMenu;
	ListView monListView;
	SwipeRefreshLayout monSwipeRefreshLayout;
	TextView headerTextView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// On d�finit la vue
		setContentView(R.layout.liste_articles);
		// On r�cup�re les �l�ments
		monListView = (ListView) this.findViewById(R.id.listeArticles);
		monSwipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipe_container);
		headerTextView = (TextView) findViewById(R.id.header_text);

		setSupportProgressBarIndeterminateVisibility(false);

		// onRefresh
		monSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				telechargeListeArticles();
			}
		});

		monItemsAdapter = new ItemsAdapter(this, mesArticles);
		monListView.setAdapter(monItemsAdapter);
		monListView.setOnItemClickListener(this);

		// On active le SwipeRefreshLayout uniquement si on est en haut de la listview
		monListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				int topRowVerticalPosition;

				if (monListView == null || monListView.getChildCount() == 0) {
					topRowVerticalPosition = 0;
				} else {
					topRowVerticalPosition = monListView.getChildAt(0).getTop();
				}
				monSwipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
			}
		});

		// J'active la BDD
		monDAO = new DAO(getApplicationContext());
		// Je charge mes articles
		mesArticles.addAll(monDAO.chargerArticlesTriParDate());
		// Mise � jour de l'affichage
		monItemsAdapter.updateListeItems(prepareAffichage());

		// Message d'accueil pour la premi�re utilisation

		// Chargement des pr�f�rences de l'utilisateur
		final SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Est-ce la premiere utilisation de l'application ?
		Boolean premiereUtilisation = mesPrefs.getBoolean(getString(R.string.idOptionPremierLancementApplication), getResources()
				.getBoolean(R.bool.defautOptionPremierLancementApplication));

		// Si premi�re utilisation : on affiche un disclaimer
		if (premiereUtilisation) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Titre
			builder.setTitle(getResources().getString(R.string.app_name));
			// Contenu
			builder.setMessage(getResources().getString(R.string.disclaimerContent));
			// Bouton d'action
			builder.setCancelable(false);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// Enregistrement que le message a d�j� �t� affich�
					Editor editor = mesPrefs.edit();
					editor.putBoolean(getString(R.string.idOptionPremierLancementApplication), false);
					editor.commit();

					// Affichage de l'�cran de configuration de l'application
					Intent intentOptions = new Intent(getApplicationContext(), OptionsActivity.class);
					startActivity(intentOptions);
				}
			});
			// On cr�e & affiche
			builder.create().show();

			// Lancement d'un t�l�chargement des articles
			telechargeListeArticles();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Je garde le menu pour pouvoir l'animer apr�s
		monMenu = menu;

		// Je charge mon menu dans l'actionBar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, monMenu);

		// Je lance l'animation si un DL est d�j� en cours
		if (DLinProgress != 0) {
			// Hack : il n'y avait pas d'acc�s � la GUI sur onCreate
			DLinProgress--;
			nouveauChargementGUI();
		}

		return super.onCreateOptionsMenu(monMenu);
	}

	/**
	 * Gestion du clic sur un article => l'ouvrir
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ArticleItem monArticle = (ArticleItem) monItemsAdapter.getItem(position);

		Intent monIntent = new Intent(this, ArticleActivity.class);
		monIntent.putExtra("ARTICLE_ID", monArticle.getID());
		startActivity(monIntent);
	}

	/**
	 * Ouverture du menu de l'action bar � l'utilisation du bouton menu
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				monMenu.performIdentifierAction(R.id.action_overflow, 0);
				return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Gestion des clic dans le menu d'options de l'activit�
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem pItem) {
		switch (pItem.getItemId()) {
		// Rafraichir la liste des articles
			case R.id.action_refresh:
				telechargeListeArticles();
				return true;
				// Menu Options
			case R.id.action_settings:
				// Je lance l'activit� options
				Intent intentOptions = new Intent(getApplicationContext(), OptionsActivity.class);
				startActivity(intentOptions);
				return true;
				// A propos
			case R.id.action_about:
				Intent intentAbout = new Intent(getApplicationContext(), AboutActivity.class);
				startActivity(intentAbout);
				return true;
			default:
				return super.onOptionsItemSelected(pItem);
		}
	}

	@SuppressLint("NewApi")
	private void telechargeListeArticles() {
		// Le retour en GUI
		nouveauChargementGUI();

		// Ma t�che de DL
		AsyncHTMLDownloader monAHD = new AsyncHTMLDownloader(getApplicationContext(), this, Constantes.HTML_LISTE_ARTICLES,
				Constantes.NEXT_INPACT_URL, monDAO);
		// Parall�lisation des t�l�chargements pour l'ensemble de l'application
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			monAHD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			monAHD.execute();
		}
	}

	@SuppressLint("NewApi")
	@Override
	public void downloadHTMLFini(String uneURL, ArrayList<Item> desItems) {
		// Si c'est un refresh g�n�ral
		if (uneURL.equals(Constantes.NEXT_INPACT_URL)) {
			android.util.Log.w("main", "" + mesArticles.size());
			for (Item unItem : desItems) {
				// Je l'enregistre en m�moire
				mesArticles.add((ArticleItem) unItem);

				// Je lance le t�l�chargement de sa miniature
				nouveauChargementGUI();
				AsyncImageDownloader monAID = new AsyncImageDownloader(getApplicationContext(), this,
						Constantes.IMAGE_MINIATURE_ARTICLE, ((ArticleItem) unItem).getURLIllustration());
				// Parall�lisation des t�l�chargements pour l'ensemble de l'application
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					monAID.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					monAID.execute();
				}

				// Je lance le t�l�chargement de son contenu
				nouveauChargementGUI();
				AsyncHTMLDownloader monAHD = new AsyncHTMLDownloader(getApplicationContext(), this, Constantes.HTML_ARTICLE,
						((ArticleItem) unItem).getURL(), monDAO);
				// Parall�lisation des t�l�chargements pour l'ensemble de l'application
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					monAHD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					monAHD.execute();
				}
			}

			android.util.Log.w("main", "" + mesArticles.size());
		}

		// gestion du t�l�chargement GUI
		finChargementGUI();
	}

	@Override
	public void downloadImageFini(String uneURL, Bitmap uneImage) {
		// gestion du t�l�chargement GUI
		finChargementGUI();
	}

	/**
	 * Fournit une liste d'articles tri�s par date + sections
	 * 
	 * @return
	 */
	private ArrayList<Item> prepareAffichage() {
		// Tri des Articles par timestamp
		Collections.sort(mesArticles);

		ArrayList<Item> monRetour = new ArrayList<Item>();
		String jourActuel = "";
		for (ArticleItem article : mesArticles) {
			// Si ce n'est pas la m�me journ�e que l'article pr�c�dent
			if (!article.getDatePublication().equals(jourActuel)) {
				// Je met � jour ma date
				jourActuel = article.getDatePublication();
				// J'ajoute un sectionItem
				monRetour.add(new SectionItem(jourActuel));
			}

			// J'ajoute mon article
			monRetour.add(article);
		}

		return monRetour;
	}

	/**
	 * G�re les animations de t�l�chargement
	 */
	private void nouveauChargementGUI() {
		// Si c'est le premier => activation des gri-gri GUI
		if (DLinProgress == 0) {
			// Couleurs du RefreshLayout
			monSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.refreshBleu),
					getResources().getColor(R.color.refreshOrange), getResources().getColor(R.color.refreshBleu), getResources()
							.getColor(R.color.refreshBlanc));
			// Animation du RefreshLayout
			monSwipeRefreshLayout.setRefreshing(true);

			// Lance la rotation du logo dans le header
			setSupportProgressBarIndeterminateVisibility(true);

			// Supprime l'ic�ne refresh dans le header
			if (monMenu != null)
				monMenu.findItem(R.id.action_refresh).setVisible(false);
		}

		// Je note le t�l�chargement en cours
		DLinProgress++;
	}

	/**
	 * G�re les animations de t�l�chargement
	 */
	private void finChargementGUI() {
		// Je note la fin du t�l�chargement
		DLinProgress--;

		// Si c'est le premier => activation des gri-gri GUI
		if (DLinProgress == 0) {
			// On stoppe l'animation du SwipeRefreshLayout
			monSwipeRefreshLayout.setRefreshing(false);

			// Arr�t de la rotation du logo dans le header
			setSupportProgressBarIndeterminateVisibility(false);

			// Affiche l'ic�ne refresh dans le header
			if (monMenu != null)
				monMenu.findItem(R.id.action_refresh).setVisible(true);

			// Je met � jour les donn�es
			monItemsAdapter.updateListeItems(prepareAffichage());
			// Je notifie le changement pour un rafraichissement du contenu
			monItemsAdapter.notifyDataSetChanged();
		}
	}

}

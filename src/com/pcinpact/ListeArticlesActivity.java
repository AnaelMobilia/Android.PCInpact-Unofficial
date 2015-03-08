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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import com.pcinpact.adapters.ItemsAdapter;
import com.pcinpact.database.DAO;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.Item;
import com.pcinpact.items.SectionItem;
import com.pcinpact.network.AsyncHTMLDownloader;
import com.pcinpact.network.AsyncImageDownloader;
import com.pcinpact.network.RefreshDisplayInterface;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Liste des articles.
 * 
 * @author Anael
 *
 */
public class ListeArticlesActivity extends ActionBarActivity implements RefreshDisplayInterface, OnItemClickListener {
	/**
	 * Les articles.
	 */
	private ArrayList<ArticleItem> mesArticles = new ArrayList<ArticleItem>();
	/**
	 * ItemAdapter.
	 */
	private ItemsAdapter monItemsAdapter;
	/**
	 * BDD.
	 */
	private DAO monDAO;
	/**
	 * Nombre de DL en cours.
	 */
	private int dlInProgress;
	/**
	 * Menu.
	 */
	private Menu monMenu;
	/**
	 * ListView.
	 */
	private ListView monListView;
	/**
	 * SwipeRefreshLayout.
	 */
	private SwipeRefreshLayout monSwipeRefreshLayout;
	/**
	 * TextView "Derni�re synchro...".
	 */
	private TextView headerTextView;
	/**
	 * Listener pour le changement de taille des textes.
	 */
	private SharedPreferences.OnSharedPreferenceChangeListener listenerOptionZoom;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// On d�finit la vue
		setContentView(R.layout.liste_articles);
		// On r�cup�re les �l�ments GUI
		monListView = (ListView) findViewById(R.id.listeArticles);
		monSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
		headerTextView = (TextView) findViewById(R.id.header_text);

		setSupportProgressBarIndeterminateVisibility(false);

		// Mise en place de l'itemAdapter
		monItemsAdapter = new ItemsAdapter(this, mesArticles);
		monListView.setAdapter(monItemsAdapter);
		monListView.setOnItemClickListener(this);

		// onRefresh
		monSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				telechargeListeArticles();
			}
		});

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
					topRowVerticalPosition = monListView.getFirstVisiblePosition();
				}
				// DEBUG
				if (Constantes.DEBUG) {
					Log.d("ListeArticlesActivity",
							"SwipeRefreshLayout - topRowVerticalPosition : " + String.valueOf(topRowVerticalPosition));
				}
				monSwipeRefreshLayout.setEnabled(topRowVerticalPosition <= 0);
			}
		});

		// J'active la BDD
		monDAO = DAO.getInstance(getApplicationContext());
		// Chargement des articles & M�J de l'affichage
		monItemsAdapter.updateListeItems(prepareAffichage());

		// Est-ce la premiere utilisation de l'application ?
		Boolean premiereUtilisation = Constantes.getOptionBoolean(getApplicationContext(),
				R.string.idOptionInstallationApplication, R.bool.defautOptionInstallationApplication);
		// Si premi�re utilisation : on affiche un disclaimer
		if (premiereUtilisation) {
			// Lancement d'un t�l�chargement des articles
			telechargeListeArticles();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Titre
			builder.setTitle(getResources().getString(R.string.app_name));
			// Contenu
			builder.setMessage(getResources().getString(R.string.disclaimerContent));
			// Bouton d'action
			builder.setCancelable(false);
			builder.setPositiveButton("Ok", null);
			// On cr�e & affiche
			builder.create().show();

			// Enregistrement de l'affichage
			Constantes.setOptionBoolean(getApplicationContext(), R.string.idOptionInstallationApplication, false);
		}

		// Gestion du changement de la taille des textes (option utilisateur)
		listenerOptionZoom = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

				// Modification de la taille des textes ?
				if (key.equals(getResources().getString(R.string.idOptionZoomTexte))) {
					// Rafraichissement de l'affichage
					monItemsAdapter.notifyDataSetChanged();

					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("ListeArticlesActivity",
								"changement taille des textes => "
										+ Constantes.getOptionInt(getApplicationContext(), R.string.idOptionZoomTexte,
												R.string.defautOptionZoomTexte));
					}
				}
			}
		};
		// Attachement du superviseur aux pr�f�rences
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(
				listenerOptionZoom);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Je garde le menu pour pouvoir l'animer apr�s
		monMenu = menu;

		// Je charge mon menu dans l'actionBar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.liste_articles_activity_actions, monMenu);

		// Je lance l'animation si un DL est d�j� en cours
		if (dlInProgress != 0) {
			// Hack : il n'y avait pas d'acc�s � la GUI sur onCreate
			dlInProgress--;
			nouveauChargementGUI();
		}

		return super.onCreateOptionsMenu(monMenu);
	}

	/**
	 * Gestion du clic sur un article => l'ouvrir + marquer comme lu.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// R�cup�re l'article en question
		ArticleItem monArticle = (ArticleItem) monItemsAdapter.getItem(position);

		// Lance l'ouverture de l'article
		Intent monIntent = new Intent(getApplicationContext(), ArticleActivity.class);
		monIntent.putExtra("ARTICLE_ID", monArticle.getId());
		startActivity(monIntent);

		// Marque l'article comme lu
		monArticle.setLu(true);
		// Mise � jour en DB
		monDAO.marquerArticleLu(monArticle);
		// Mise � jour graphique
		monItemsAdapter.notifyDataSetChanged();
	}

	/**
	 * Ouverture du menu de l'action bar � l'utilisation du bouton menu.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Bouton menu
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (monMenu != null) {
				monMenu.performIdentifierAction(R.id.action_overflow, 0);
			} else {
				// DEBUG
				if (Constantes.DEBUG) {
					Log.e("ListeArticlesActivity", "onKeyUp, monMenu null");
				}
				// Retour utilisateur ?
				// L'utilisateur demande-t-il un debug ?
				Boolean debug = Constantes.getOptionBoolean(getApplicationContext(), R.string.idOptionDebug,
						R.bool.defautOptionDebug);
				if (debug) {
					Toast monToast = Toast.makeText(getApplicationContext(),
							"[ListeArticlesActivity] Le menu est null (onKeyUp)", Toast.LENGTH_LONG);
					monToast.show();
				}
			}
		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Gestion des clic dans le menu d'options de l'activit�.
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

	/**
	 * Arr�t de l'activit�.
	 */
	@Override
	protected void onDestroy() {
		// D�tachement du listener pour la taille des textes
		PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(
				listenerOptionZoom);

		// Nettoyage du cache de l'application
		nettoyerCache();

		super.onDestroy();
	}

	/**
	 * Lance le t�l�chargement de la liste des articles.
	 */
	@SuppressLint("NewApi")
	private void telechargeListeArticles() {
		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("ListeArticlesActivity", "telechargeListeArticles()");
		}

		// Uniquement si on est pas d�j� en train de faire un refresh...
		if (dlInProgress == 0) {
			// GUI : activit� en cours...
			nouveauChargementGUI();

			/**
			 * Nettoyage du cache
			 */
			nettoyerCache();

			/**
			 * T�l�chargement des articles dont le contenu n'avait pas �t� t�l�charg�
			 */
			telechargeListeArticles(monDAO.chargerArticlesATelecharger());

			/**
			 * T�l�chargement des pages de liste d'articles
			 */
			int nbArticles = Constantes.getOptionInt(getApplicationContext(), R.string.idOptionNbArticles,
					R.string.defautOptionNbArticles);
			int nbPages = nbArticles / Constantes.NB_ARTICLES_PAR_PAGE;
			// T�l�chargement de chaque page...
			for (int numPage = 1; numPage <= nbPages; numPage++) {
				// Le retour en GUI
				nouveauChargementGUI();

				// Ma t�che de DL
				AsyncHTMLDownloader monAHD = new AsyncHTMLDownloader(this, Constantes.HTML_LISTE_ARTICLES,
						Constantes.NEXT_INPACT_URL_NUM_PAGE + numPage, monDAO, getApplicationContext());
				// Parall�lisation des t�l�chargements pour l'ensemble de l'application
				if (Build.VERSION.SDK_INT >= Constantes.HONEYCOMB) {
					monAHD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					monAHD.execute();
				}
			}

			/**
			 * T�l�chargement des miniatures manquantes
			 */
			// Les miniatures que je devrais avoir
			HashMap<String, String> miniaturesItem = new HashMap<>();
			int nbItems = mesArticles.size();
			for (int i = 0; i < nbItems; i++) {
				miniaturesItem.put(mesArticles.get(i).getImageName(), mesArticles.get(i).getUrlIllustration());
			}

			// Les miniatures que j'ai
			String[] miniaturesFS = new File(getApplicationContext().getFilesDir() + Constantes.PATH_IMAGES_MINIATURES).list();
			// Pour chaque miniature que j'ai...
			for (String uneMiniature : miniaturesFS) {
				// Si elle est aussi dans la liste des miniatures � avoir
				if (miniaturesItem.containsKey(uneMiniature)) {
					// Je l'efface
					miniaturesItem.remove(uneMiniature);
				}
			}

			// Miniatures restantes == miniatures manquantes
			for (String uneMiniature : miniaturesItem.values()) {
				// Je lance le t�l�chargement
				AsyncImageDownloader monAID = new AsyncImageDownloader(getApplicationContext(), this,
						Constantes.IMAGE_MINIATURE_ARTICLE, uneMiniature);
				// Parall�lisation des t�l�chargements pour l'ensemble de l'application
				if (Build.VERSION.SDK_INT >= Constantes.HONEYCOMB) {
					monAID.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					monAID.execute();
				}
				nouveauChargementGUI();
			}

			// GUI : fin de l'activit� en cours...
			finChargementGUI();
		}
	}

	/**
	 * Lance le t�l�chargement des articles.
	 * 
	 * @param desItems liste d'articles � t�l�charger
	 */
	@SuppressLint("NewApi")
	private void telechargeListeArticles(final ArrayList<? extends Item> desItems) {
		for (Item unItem : desItems) {
			// T�che de DL HTML
			AsyncHTMLDownloader monAHD;
			// DL de l'image d'illustration ?
			boolean dlIllustration = true;

			// Est-ce un article abonn� ?
			if (((ArticleItem) unItem).isAbonne()) {
				boolean isConnecteRequis = false;

				// Ai-je d�j� la version publique de l'article ?
				if (!((ArticleItem) unItem).getContenu().equals("")) {
					// Je requiert d'�tre connect� (sinon le DL ne sert � rien)
					isConnecteRequis = true;
					// Je ne veux pas DL l'image de l'article
					dlIllustration = false;
				}
				// T�l�chargement de la ressource
				monAHD = new AsyncHTMLDownloader(this, Constantes.HTML_ARTICLE, ((ArticleItem) unItem).getUrl(), monDAO,
						getApplicationContext(), true, isConnecteRequis);
			} else {
				// T�l�chargement de la ressource
				monAHD = new AsyncHTMLDownloader(this, Constantes.HTML_ARTICLE, ((ArticleItem) unItem).getUrl(), monDAO,
						getApplicationContext());
			}

			// Parall�lisation des t�l�chargements pour l'ensemble de l'application
			if (Build.VERSION.SDK_INT >= Constantes.HONEYCOMB) {
				monAHD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				monAHD.execute();
			}
			nouveauChargementGUI();

			// Pas de DL des miniatures pour les articles abonn�s dont je tente de r�cup�rer le contenu
			if (dlIllustration) {
				// Je lance le t�l�chargement de sa miniature
				AsyncImageDownloader monAID = new AsyncImageDownloader(getApplicationContext(), this,
						Constantes.IMAGE_MINIATURE_ARTICLE, ((ArticleItem) unItem).getUrlIllustration());
				// Parall�lisation des t�l�chargements pour l'ensemble de l'application
				if (Build.VERSION.SDK_INT >= Constantes.HONEYCOMB) {
					monAID.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					monAID.execute();
				}
				nouveauChargementGUI();
			}
		}
	}

	@Override
	public void downloadHTMLFini(final String uneURL, final ArrayList<? extends Item> desItems) {
		// Si c'est un refresh g�n�ral
		if (uneURL.startsWith(Constantes.NEXT_INPACT_URL_NUM_PAGE)) {
			// Le asyncDL ne me retourne que des articles non pr�sents en DB => � DL
			telechargeListeArticles(desItems);
		}

		// gestion du t�l�chargement GUI
		finChargementGUI();
	}

	@Override
	public void downloadImageFini(final String uneURL) {
		// gestion du t�l�chargement GUI
		finChargementGUI();
	}

	/**
	 * Fournit une liste d'articles tri�s par date + sections.
	 * 
	 * @return Liste d'articles
	 */
	private ArrayList<Item> prepareAffichage() {
		ArrayList<Item> monRetour = new ArrayList<Item>();
		String jourActuel = "";

		// Nombre d'articles � afficher
		int maLimite = Constantes.getOptionInt(getApplicationContext(), R.string.idOptionNbArticles,
				R.string.defautOptionNbArticles);
		// Chargement des articles depuis la BDD (tri�, limit�)
		mesArticles = monDAO.chargerArticlesTriParDate(maLimite);

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

		// Mise � jour de la date de dernier refresh
		long dernierRefresh = monDAO.chargerDateRefresh(Constantes.DB_REFRESH_ID_LISTE_ARTICLES);

		if (dernierRefresh == 0) {
			// Jamais synchro...
			headerTextView.setText(getString(R.string.lastUpdateNever));
		} else {
			// Une m�j � d�j� �t� faite
			headerTextView.setText(getString(R.string.lastUpdate)
					+ new SimpleDateFormat(Constantes.FORMAT_DATE_DERNIER_REFRESH, Locale.getDefault()).format(dernierRefresh));
		}

		return monRetour;
	}

	/**
	 * G�re les animations de t�l�chargement.
	 */
	private void nouveauChargementGUI() {
		// Si c'est le premier => activation des gri-gri GUI
		if (dlInProgress == 0) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("nouveauChargementGUI", "Lancement animation");
			}
			// Couleurs du RefreshLayout
			monSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.refreshBleu),
					getResources().getColor(R.color.refreshOrange), getResources().getColor(R.color.refreshBleu), getResources()
							.getColor(R.color.refreshBlanc));
			// Animation du RefreshLayout
			monSwipeRefreshLayout.setRefreshing(true);

			// Lance la rotation du logo dans le header
			setSupportProgressBarIndeterminateVisibility(true);

			// Supprime l'ic�ne refresh dans le header
			if (monMenu != null) {
				monMenu.findItem(R.id.action_refresh).setVisible(false);
			}
		}

		// Je note le t�l�chargement en cours
		dlInProgress++;
		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("nouveauChargementGUI", String.valueOf(dlInProgress));
		}
	}

	/**
	 * G�re les animations de t�l�chargement.
	 */
	private void finChargementGUI() {
		// Je note la fin du t�l�chargement
		dlInProgress--;

		// Si c'est le premier => activation des gri-gri GUI
		if (dlInProgress == 0) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("finChargementGUI", "Arr�t animation");
			}

			// On stoppe l'animation du SwipeRefreshLayout
			monSwipeRefreshLayout.setRefreshing(false);

			// Arr�t de la rotation du logo dans le header
			setSupportProgressBarIndeterminateVisibility(false);

			// Affiche l'ic�ne refresh dans le header
			if (monMenu != null) {
				monMenu.findItem(R.id.action_refresh).setVisible(true);
			}

			// Je met � jour les donn�es
			monItemsAdapter.updateListeItems(prepareAffichage());
			// Je notifie le changement pour un rafraichissement du contenu
			monItemsAdapter.notifyDataSetChanged();
		}
		// DEBUG
		if (Constantes.DEBUG) {
			Log.i("finChargementGUI", String.valueOf(dlInProgress));
		}
	}

	/**
	 * Nettoie le cache de l'application.
	 */
	private void nettoyerCache() {
		try {
			// Nombre d'articles � conserver
			int maLimite = Constantes.getOptionInt(getApplicationContext(), R.string.idOptionNbArticles,
					R.string.defautOptionNbArticles);

			/**
			 * Donn�es � conserver
			 */

			// Je prot�ge les images pr�sentes dans les articles � conserver
			ArrayList<String> imagesLegit = new ArrayList<String>();
			int nbArticles = mesArticles.size();
			for (int i = 0; i < nbArticles; i++) {
				imagesLegit.add(mesArticles.get(i).getImageName());
			}

			/**
			 * Donn�es � supprimer
			 */
			ArrayList<ArticleItem> articlesASupprimer = monDAO.chargerArticlesASupprimer(maLimite);

			/**
			 * Traitement
			 */
			nbArticles = articlesASupprimer.size();
			for (int i = 0; i < nbArticles; i++) {
				ArticleItem article = articlesASupprimer.get(i);

				// DEBUG
				if (Constantes.DEBUG) {
					Log.w("ListeArticlesActivity", "Cache : suppression de " + article.getTitre());
				}

				// Suppression en DB
				monDAO.supprimerArticle(article);

				// Suppression des commentaires de l'article
				monDAO.supprimerCommentaire(article.getId());

				// Suppression de la date de Refresh des commentaires
				monDAO.supprimerDateRefresh(article.getId());

				// Suppression de la miniature, uniquement si plus utilis�e
				if (!imagesLegit.contains(article.getImageName())) {
					File monFichier = new File(getApplicationContext().getFilesDir() + Constantes.PATH_IMAGES_MINIATURES,
							article.getImageName());
					monFichier.delete();
				}
			}

			/**
			 * Suppression du cache v < 1.8.0 Les fichiers sur stock�s en local
			 */
			String[] savedFiles = getApplicationContext().fileList();

			for (String file : savedFiles) {
				// Article � effacer
				getApplicationContext().deleteFile(file);
			}
		} catch (Exception e) {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.e("ListeArticlesActivity", "nettoyerCache()", e);
			}
		}
	}
}

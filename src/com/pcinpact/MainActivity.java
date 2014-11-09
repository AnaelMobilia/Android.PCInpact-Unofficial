/*
 * Copyright 2013, 2014 Sami Ferhah, Anael Mobilia, Guillaume Bour
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

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pcinpact.adapters.ArticleAdapter;
import com.pcinpact.connection.HtmlConnector;
import com.pcinpact.connection.IConnectable;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.Item;
import com.pcinpact.items.SectionItem;
import com.pcinpact.managers.ArticleManager;
import com.pcinpact.managers.CommentManager;
import com.pcinpact.models.ArticlesWrapper;
import com.pcinpact.models.INpactArticleDescription;
import com.pcinpact.parsers.HtmlParser;

public class MainActivity extends ActionBarActivity implements IConnectable, OnItemClickListener {

	ListView monListView;
	SwipeRefreshLayout monSwipeRefreshLayout;
	// INpactListAdapter adapter;
	ArticleAdapter adapter;
	TextView headerTextView;
	Menu m_menu;
	List<INpactArticleDescription> newArticles;

	final static int DL_LIST = 0;
	final static int DL_ARTICLE = 1;
	final static int DL_COMMS = 2;
	final static int DL_IMG = 3;

	AtomicInteger numberOfPendingArticles = new AtomicInteger();
	AtomicInteger numberOfPendingImages = new AtomicInteger();

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_MENU:
				m_menu.performIdentifierAction(R.id.action_overflow, 0);
				return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// On d�finit la vue
		setContentView(R.layout.main);
		// On r�cup�re les �l�ments
		monListView = (ListView) this.findViewById(R.id.listeArticles);
		monSwipeRefreshLayout = (SwipeRefreshLayout) this.findViewById(R.id.swipe_container);
		headerTextView = (TextView) findViewById(R.id.header_text);

		setSupportProgressBarIndeterminateVisibility(false);

		// onRefresh
		monSwipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshListeArticles();
			}
		});

		// adapter = new INpactListAdapter(this, null).buildData();
		adapter = new ArticleAdapter(this, new ArrayList<Item>());
		monListView.setAdapter(adapter);
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

		ArticlesWrapper w = NextInpact.getInstance(this).getArticlesWrapper();

		if (w.getArticles().size() > 0) {
			loadArticles();
			headerTextView.setText(getString(R.string.lastUpdate) + w.LastUpdate);
		} else {
			// Si pas d'articles, on lance un chargement
			refreshListeArticles();
		}

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
					Intent intentOptions = new Intent(MainActivity.this, OptionsActivity.class);
					startActivity(intentOptions);
				}
			});
			// On cr�e & affiche
			builder.create().show();
		}
	}

	/**
	 * Rafra�chir la liste des articles
	 */
	private void refreshListeArticles() {
		// V�rification de la connexion internet avant de lancer
		ConnectivityManager l_Connection = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (l_Connection.getActiveNetworkInfo() == null || !l_Connection.getActiveNetworkInfo().isConnected()) {

			// Pas de connexion -> affichage d'un toast
			CharSequence text = getString(R.string.chargementPasInternet);
			int duration = Toast.LENGTH_LONG;

			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		} else {
			// Visuels
			// Couleurs du RefreshLayout
			monSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.refreshBleu),
					getResources().getColor(R.color.refreshOrange), getResources().getColor(R.color.refreshBleu), getResources()
							.getColor(R.color.refreshBlanc));
			// Animation du RefreshLayout
			monSwipeRefreshLayout.setRefreshing(true);

			// On efface le bouton rafra�chir du header
			if (m_menu != null)
				m_menu.findItem(R.id.action_refresh).setVisible(false);
			// On fait tourner le bouton en cercle dans le header
			setSupportProgressBarIndeterminateVisibility(true);

			// Appel � la m�thode qui va faire le boulot...
			loadArticlesListFromServer();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		m_menu = null;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem pItem) {
		switch (pItem.getItemId()) {
		// Rafraichir la liste des articles
			case R.id.action_refresh:
				refreshListeArticles();
				return true;

				// Menu Options
			case R.id.action_settings:
				// Je lance l'activit� options
				Intent intentOptions = new Intent(MainActivity.this, OptionsActivity.class);
				startActivity(intentOptions);
				return true;

				// A propos
			case R.id.action_about:
				Intent intentAbout = new Intent(MainActivity.this, AboutActivity.class);
				startActivity(intentAbout);
				return true;

			default:
				return super.onOptionsItemSelected(pItem);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Je garde le menu pour pouvoir l'animer apr�s
		m_menu = menu;

		// Je charge mon menu dans l'actionBar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	void loadArticles() {
		// adapter.refreshData(NextInpact.getInstance(this).getArticlesWrapper().getArticles());

		// Passage ancien syst�me -> nouveau syst�me
		List<INpactArticleDescription> oldList = NextInpact.getInstance(this).getArticlesWrapper().getArticles();
		ArrayList<Item> mesItems = new ArrayList<Item>();

		Integer section = -1;
		for (INpactArticleDescription unOldItem : oldList) {
			// Section (calcul�e)
			if (section != unOldItem.section) {
				section = unOldItem.section;

				// C'est une section
				SectionItem maSection = new SectionItem();
				// pas joli joli...
				maSection.convertOld(unOldItem);
				mesItems.add(maSection);
			} else {
				// C'est un article
				ArticleItem monArticle = new ArticleItem();
				monArticle.convertOld(unOldItem);
				mesItems.add(monArticle);
			}
		}
		// Je emt � jour les donn�es
		adapter.updateArticles(mesItems);
		// Je notifie le changement pour un rafraichissement du contenu
		adapter.notifyDataSetChanged();
	}

	public void loadArticlesListFromServer() {
		HtmlConnector connector = new HtmlConnector(this, this);
		connector.state = DL_LIST;
		connector.sendRequest(NextInpact.NEXT_INPACT_URL + "/?page=2", "GET", null, 0, null);
	}

	public void loadArticlesFromServer(List<INpactArticleDescription> articles) {
		if (articles.size() == 0) {
			stopRefreshing();
			return;
		}

		// Formatage de la date de derni�re mise � jour des news
		DateFormat monFormatDate = DateFormat.getDateTimeInstance();
		Date maDate = Calendar.getInstance().getTime();
		NextInpact.getInstance(this).getArticlesWrapper().LastUpdate = " " + monFormatDate.format(maDate);

		NextInpact.getInstance(this).getArticlesWrapper().setArticles(articles);
		ArticleManager.saveArticlesWrapper(this, NextInpact.getInstance(this).getArticlesWrapper());

		loadArticles();

		headerTextView.setText(getString(R.string.lastUpdate) + NextInpact.getInstance(this).getArticlesWrapper().LastUpdate);

		ArticleManager.saveArticlesWrapper(this, NextInpact.getInstance(this).getArticlesWrapper());

		// R�cup�ration des contenus des articles
		numberOfPendingArticles.set(articles.size());

		for (int i = 0; i < articles.size(); i++) {
			INpactArticleDescription article = articles.get(i);

			if (fileExists(article.getID() + ".html")) {
				numberOfPendingArticles.decrementAndGet();
				stopRefreshingIfNeeded();
				continue;
			}

			HtmlConnector connector = new HtmlConnector(this, this);
			connector.state = DL_ARTICLE;
			connector.tag = article.getID();
			connector.sendRequest(NextInpact.NEXT_INPACT_URL + article.getUrl(), "GET", null, 0, null);
		}

		// R�cuperation des miniatures des articles
		numberOfPendingImages.set(articles.size());

		for (int i = 0; i < articles.size(); i++) {
			INpactArticleDescription article = articles.get(i);

			if (fileExists(article.getID() + ".jpg")) {
				numberOfPendingImages.decrementAndGet();
				stopRefreshingIfNeeded();
				continue;
			}

			HtmlConnector connector = new HtmlConnector(this, this);
			connector.state = DL_IMG;
			connector.tag = article.getID();
			connector.sendRequest(article.imgURL, "GET", null, 0, null);
		}

		// R�cup�ration des commentaires de l'article
		// Option de l'utilisateur : gestion des commentaires
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Sauf souhait contraire de l'utilisateur, on t�l�charge les commentaires
		if (mesPrefs.getBoolean(getString(R.string.idOptionTelechargerCommentaires),
				getResources().getBoolean(R.bool.defautOptionTelechargerCommentaires))) {
			for (int i = 0; i < articles.size(); i++) {
				INpactArticleDescription article = articles.get(i);

				if (fileExists(article.getID() + "_comms.html")) {
					continue;
				}

				HtmlConnector connector = new HtmlConnector(this, this);
				connector.state = DL_COMMS;
				connector.tag = article.getID();
				String data = "page=1&newsId=" + article.getID() + "&commId=0";
				connector.sendRequest(NextInpact.NEXT_INPACT_URL + "/comment/", "POST", data, null);
			}
		}

		// On nettoye le cache des articles qui ne sont plus utilis�s
		// Cr�ation des variations des noms de fichiers pour les articles
		ArrayList<String> fichiersLegitimes = new ArrayList<String>();

		// cr�ation d'un listIterator sur la liste d'articles
		ListIterator<INpactArticleDescription> it = articles.listIterator();
		while (it.hasNext()) {
			// id de l'article
			String idArticle = String.valueOf(it.next().getID());
			// Article
			fichiersLegitimes.add(idArticle + ".html");
			// Miniature
			fichiersLegitimes.add(idArticle + ".jpg");
			// Commentaires
			fichiersLegitimes.add(idArticle + "_comms.html");
		}
		// Liste des articles -> � conserver
		fichiersLegitimes.add(ArticleManager.FILE_NAME_ARTICLES);

		// Les fichiers sur stock�s en local
		String[] SavedFiles = getApplicationContext().fileList();

		for (String file : SavedFiles) {
			if (!fichiersLegitimes.contains(file)) {
				// Article � effacer
				getApplicationContext().deleteFile(file);
			}
		}
	}

	public boolean fileExists(String articleID) {

		String[] SavedFiles = getApplicationContext().fileList();
		for (String file : SavedFiles) {
			if ((articleID).equals(file)) {
				return true;
			}
		}

		return false;
	}

	// Callback Iconnectable - lorsque le chargement de la liste des articles est effectu�e
	@Override
	public void didConnectionResult(final byte[] result, final int state, final String tag) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				didConnectionResultOnUiThread(result, state, tag);
			}
		});
	}

	public void stopRefreshingIfNeeded() {
		if (numberOfPendingArticles.get() == 0 && numberOfPendingImages.get() == 0) {
			stopRefreshing();
		}
	}

	public void stopRefreshing() {
		// On arr�te la rotation du logo dans le header
		setSupportProgressBarIndeterminateVisibility(false);
		// On stoppe l'animation du SwipeRefreshLayout
		monSwipeRefreshLayout.setRefreshing(false);

		// Affiche � nouveau l'ic�ne dans le header
		if (m_menu != null)
			m_menu.findItem(R.id.action_refresh).setVisible(true);

		// On force le refraichissement de la listview
		monListView.invalidateViews();
	}

	// Parser appel� en cas de succ�s du t�l�chargement
	public void didConnectionResultOnUiThread(final byte[] result, final int state, final String tag) {
		if (state == DL_ARTICLE) {
			ArticleManager.saveArticle(this, result, tag);
			numberOfPendingArticles.decrementAndGet();
			stopRefreshingIfNeeded();
		}

		if (state == DL_COMMS) {
			CommentManager.saveComments(this, result, tag);
		}

		else if (state == DL_LIST) {
			List<INpactArticleDescription> articles = null;
			try {
				HtmlParser hh = new HtmlParser(new ByteArrayInputStream(result));
				articles = hh.getArticles();
			} catch (Exception e) {
				// TODO : # 59 ajouter un retour utilisateur pour l'erreur de rechargement de la liste d'articles !
				stopRefreshing();
			}

			if (articles != null)
				loadArticlesFromServer(articles);
		}

		else if (state == DL_IMG) {
			ArticleManager.saveImage(this, result, tag);
			numberOfPendingImages.decrementAndGet();
			stopRefreshingIfNeeded();
		}
	}

	@Override
	public void didFailWithError(final String error, final int state) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				didFailWithErrorOnUiThread(error, state);
			}
		});

	}

	public void didFailWithErrorOnUiThread(final String error, final int state) {

		if (state == DL_ARTICLE) {
			numberOfPendingArticles.decrementAndGet();
			stopRefreshingIfNeeded();
		}

		else if (state == DL_LIST) {
			stopRefreshing();
			showErrorDialog(error);
		}

		else if (state == DL_IMG) {
			numberOfPendingImages.decrementAndGet();
			stopRefreshingIfNeeded();

		} else if (state == DL_COMMS) {

		}

		// Message d'erreur, si demand� !
		// Chargement des pr�f�rences de l'utilisateur
		final SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		// Est-ce la premiere utilisation de l'application ?
		Boolean debug = mesPrefs.getBoolean(getString(R.string.idOptionDebug), getResources()
				.getBoolean(R.bool.defautOptionDebug));

		if (debug) {
			// Affichage utilisateur du message d'erreur
			CharSequence text = "Message d'erreur d�taill� : " + error;
			int duration = Toast.LENGTH_LONG;

			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		}
	}

	public void showErrorDialog(final String error) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.titleError));
		builder.setMessage(error);
		builder.setPositiveButton(getString(R.string.buttonOkError), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface pDialog, final int pWhich) {
				pDialog.dismiss();
			}
		});
		builder.create().show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ArticleItem monArticle = (ArticleItem) adapter.getItem(position);

		Intent monIntent = new Intent(this, ArticleActivity.class);
		monIntent.putExtra("ARTICLE_ID", monArticle.getID());
		startActivity(monIntent);
	}

}

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

import com.pcinpact.adapters.ItemsAdapter;
import com.pcinpact.downloaders.RefreshDisplayInterface;
import com.pcinpact.items.CommentaireItem;
import com.pcinpact.items.Item;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
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
import android.widget.Toast;

public class CommentairesActivity extends ActionBarActivity implements RefreshDisplayInterface {
	private ArrayList<CommentaireItem> lesCommentaires;
	private String articleID;
	private ListView monListView;
	private ItemsAdapter monItemsAdapter;
	private Menu monMenu;
	private Button buttonDl10Commentaires;
	private Boolean isLoading = false;
	private Boolean isFinCommentaires = false;

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
		monItemsAdapter = new ItemsAdapter(this, new ArrayList<Item>());
		monListView.setAdapter(monItemsAdapter);

		// ID de l'article concern�
		articleID = getIntent().getExtras().getString("ARTICLE_ID");
		
		// TODO : chargement des commentaires d�j� existants
		
		
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
	private void refreshListeCommentaires() {
		// V�rification de la connexion internet avant de lancer
		ConnectivityManager l_Connection = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (l_Connection.getActiveNetworkInfo() == null || !l_Connection.getActiveNetworkInfo().isConnected()) {
			// Pas de connexion -> affichage d'un toast
			CharSequence text = getString(R.string.chargementPasInternet);
			int duration = Toast.LENGTH_LONG;

			Toast toast = Toast.makeText(getApplicationContext(), text, duration);
			toast.show();
		} else {
			// M�J des graphismes
			lancerAnimationTelechargement();

			int idDernierCommentaire = 0;
			// Si j'ai des commentaires, je r�cup�re l'ID du dernier dans la liste
			if (lesCommentaires.size() > 0) {
				CommentaireItem lastCommentaire = lesCommentaires.get(lesCommentaires.size() - 1);
				idDernierCommentaire = lastCommentaire.getID();
			}

			// Le cast en int supprime la partie apr�s la virgule
			int maPage = (int) Math.floor((idDernierCommentaire / Constantes.NB_COMMENTAIRES_PAR_PAGE) + 1);

			
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
				Intent i = new Intent(this, ListeArticlesActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				this.startActivity(i);
				return true;

				// Rafraichir la liste des commentaires
			case R.id.action_refresh:
				refreshListeCommentaires();
				return true;

			default:
				return super.onOptionsItemSelected(pItem);
		}
	}

	protected void safeDidConnectionResult(byte[] result, int state, String tag) {
//		// M�J des graphismes
//		arreterAnimationTelechargement();
//
//		List<INPactComment> newComments = Old_CommentManager.getCommentsFromBytes(this, result);
//
//		// SSi nouveaux commentaires
//		if (newComments.size() != 0) {
//			// j'ajoute les commentaires juste t�l�charg�s
//			comments.addAll(newComments);
//
//			// Passage ancien syst�me -> nouveau syst�me
//			ArrayList<Item> mesItems = (ArrayList) convertOld(comments);
//
//			// Je met � jour les donn�es
//			monItemsAdapter.updateListeItems(mesItems);
//			// Je notifie le changement pour un rafraichissement du contenu
//			monItemsAdapter.notifyDataSetChanged();
//		}
//
//		// Reste-t-il des commentaires � t�l�charger ? (chargement continu automatique)
//		if (newComments.size() < NextInpact.NB_COMMENTAIRES_PAR_PAGE) {
//			isFinCommentaires = true;
//		}

	}

	protected void safeDidFailWithError(String error, int state) {
		// M�J des graphismes
		arreterAnimationTelechargement();

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
	public void downloadHTMLFini(String uneURL, ArrayList<Item> mesItems) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void downloadImageFini(String uneURL, Bitmap uneImage) {
		// TODO Auto-generated method stub
		
	}

}

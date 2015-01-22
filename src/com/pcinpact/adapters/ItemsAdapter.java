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
package com.pcinpact.adapters;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import com.pcinpact.Constantes;
import com.pcinpact.R;
import com.pcinpact.items.ArticleItem;
import com.pcinpact.items.CommentaireItem;
import com.pcinpact.items.Item;
import com.pcinpact.items.SectionItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter pour le rendu des *Item
 * 
 * @author Anael
 *
 */
public class ItemsAdapter extends BaseAdapter {
	// Ressources graphique
	private Context monContext;
	private LayoutInflater monLayoutInflater;
	private ArrayList<? extends Item> mesItems;

	public ItemsAdapter(Context unContext, ArrayList<? extends Item> desItems) {
		// Je charge le bouzin
		monContext = unContext;
		mesItems = desItems;
		monLayoutInflater = LayoutInflater.from(monContext);
	}

	/**
	 * Met � jour les donn�es de la liste d'items
	 * 
	 * @param nouveauxItems
	 */
	public void updateListeItems(ArrayList<? extends Item> nouveauxItems) {
		mesItems = nouveauxItems;
	}

	@Override
	public int getCount() {
		return mesItems.size();
	}

	@Override
	public Item getItem(int arg0) {
		return mesItems.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	/**
	 * Le nombre d'objets diff�rents pouvant exister dans l'application
	 */
	@Override
	public int getViewTypeCount() {
		return Item.nombreDeTypes;
	}

	/**
	 * Le type de l'objet � la position (pour d�finir le bon type de vue � fournir)
	 */
	@Override
	public int getItemViewType(int position) {
		return mesItems.get(position).getType();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Gestion du recyclage des vues - voir http://android.amberfog.com/?p=296
		// Pas de recyclage
		if (convertView == null) {
			// Je cr�e la vue qui va bien...
			switch (getItemViewType(position)) {
				case Item.typeSection:
					convertView = monLayoutInflater.inflate(R.layout.liste_articles_item_section, parent, false);
					convertView.setOnClickListener(null);
					convertView.setOnLongClickListener(null);
					break;
				case Item.typeArticle:
					convertView = monLayoutInflater.inflate(R.layout.liste_articles_item_article, parent, false);
					break;
				case Item.typeCommentaire:
					convertView = monLayoutInflater.inflate(R.layout.commentaires_item_commentaire, parent, false);
					break;
			}
		} else {
			// DEBUG
			if (Constantes.DEBUG) {
				Log.w("ItemsAdapter", "getView : recyclage de la vue");
			}
		}

		Item i = mesItems.get(position);

		// Pr�f�rences de l'utilisateur : taille du texte
		SharedPreferences mesPrefs = PreferenceManager.getDefaultSharedPreferences(monContext);
		// Taile par d�faut
		// http://developer.android.com/reference/android/webkit/WebSettings.html#setDefaultFontSize%28int%29
		final int tailleDefaut = Integer.valueOf(monContext.getResources().getString(R.string.defautOptionZoomTexte));
		// L'option selectionn�e
		final int tailleOptionUtilisateur = Integer.parseInt(mesPrefs.getString(monContext.getString(R.string.idOptionZoomTexte),
				String.valueOf(tailleDefaut)));
		float monCoeffZoomTexte = (float) tailleOptionUtilisateur / (float) tailleDefaut;

		if (i != null) {
			// Section
			if (i.getType() == Item.typeSection) {
				SectionItem si = (SectionItem) i;

				TextView sectionView = (TextView) convertView.findViewById(R.id.titreSection);
				sectionView.setText(si.getTitre());

				// Taille de texte personnalis�e ?
				if (tailleOptionUtilisateur != tailleDefaut) {
					// On applique la taille demand�e
					appliqueZoom(sectionView, monCoeffZoomTexte);
				}

			}
			// Article
			else if (i.getType() == Item.typeArticle) {
				ArticleItem ai = (ArticleItem) i;

				ImageView imageArticle = (ImageView) convertView.findViewById(R.id.imageArticle);
				TextView labelAbonne = (TextView) convertView.findViewById(R.id.labelAbonne);
				TextView titreArticle = (TextView) convertView.findViewById(R.id.titreArticle);
				TextView heureArticle = (TextView) convertView.findViewById(R.id.heureArticle);
				TextView sousTitreArticle = (TextView) convertView.findViewById(R.id.sousTitreArticle);
				TextView commentairesArticle = (TextView) convertView.findViewById(R.id.commentairesArticle);

				// Gestion du badge abonn�
				if (ai.isAbonne()) {
					labelAbonne.setVisibility(View.VISIBLE);
				}
				// Remplissage des textview
				titreArticle.setText(ai.getTitre());
				heureArticle.setText(ai.getHeureMinutePublication());
				sousTitreArticle.setText(ai.getSousTitre());
				commentairesArticle.setText(String.valueOf(ai.getNbCommentaires()));
				// Gestion de l'image
				FileInputStream in;
				try {
					// Ouverture du fichier en cache
					File monFichier = new File(monContext.getFilesDir() + Constantes.PATH_IMAGES_MINIATURES + ai.getImageName());
					in = new FileInputStream(monFichier);
					imageArticle.setImageBitmap(BitmapFactory.decodeStream(in));
					in.close();
				} catch (Exception e) {
					// Si le fichier n'est pas trouv�, je fournis une image par d�faut
					imageArticle.setImageDrawable(monContext.getResources().getDrawable(R.drawable.logo_nextinpact));
					// DEBUG
					if (Constantes.DEBUG) {
						Log.w("ItemsAdapter", "getView -> Article", e);
					}
				}

				// Taille de texte personnalis�e ?
				if (tailleOptionUtilisateur != tailleDefaut) {
					// On applique la taille demand�e
					appliqueZoom(titreArticle, monCoeffZoomTexte);
					appliqueZoom(heureArticle, monCoeffZoomTexte);
					appliqueZoom(sousTitreArticle, monCoeffZoomTexte);
					appliqueZoom(commentairesArticle, monCoeffZoomTexte);
					appliqueZoom(labelAbonne, monCoeffZoomTexte);
				}
			}
			// Commentaire
			else if (i.getType() == Item.typeCommentaire) {
				CommentaireItem ai = (CommentaireItem) i;

				TextView auteurDateCommentaire = (TextView) convertView.findViewById(R.id.auteurDateCommentaire);
				TextView numeroCommentaire = (TextView) convertView.findViewById(R.id.numeroCommentaire);
				TextView commentaire = (TextView) convertView.findViewById(R.id.commentaire);

				// Remplissage des textview
				auteurDateCommentaire.setText(ai.getAuteurDateCommentaire());
				numeroCommentaire.setText(String.valueOf(ai.getID()));
				// commentaire.setText(Html.fromHtml(ai.getCommentaire()));
				Spanned spannedContent = Html.fromHtml(ai.getCommentaire(), new ImageGetter() {

					@Override
					public Drawable getDrawable(String source) {
						Drawable d = null;

						try {
							URL url = new URL(source);
							Object o = url.getContent();
							InputStream src = (InputStream) o;

							d = Drawable.createFromStream(src, "src");
							if (d != null) {
								DisplayMetrics metrics = new DisplayMetrics();
								((WindowManager) monContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
										.getMetrics(metrics);

								int monCoeff;
								float monCoeffZoom = tailleOptionUtilisateur / tailleDefaut;
								// Si on est sur la r�solution par d�faut, on reste � 1
								if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT) {
									monCoeff = Math.round(1 * monCoeffZoom);
								}
								// Sinon, on calcule le zoom � appliquer (avec un coeff 2 pour �viter les images trop petites)
								else {
									monCoeff = Math.round(2 * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
											* monCoeffZoom);
								}
								// On �vite un coeff inf�rieur � 1 (image non affich�e !)
								if (monCoeff < 1) {
									monCoeff = 1;
								}

								// On d�finit la taille de l'image
								d.setBounds(0, 0, (d.getIntrinsicWidth() * monCoeff), (d.getIntrinsicHeight() * monCoeff));
							}
						} catch (Exception e) {
						}
						return d;
					}
				}, null);
				commentaire.setText(spannedContent);
				// Active les liens a href
				commentaire.setMovementMethod(LinkMovementMethod.getInstance());

				// Taille de texte personnalis�e ?
				if (tailleOptionUtilisateur != tailleDefaut) {
					// On applique la taille demand�e
					appliqueZoom(auteurDateCommentaire, monCoeffZoomTexte);
					appliqueZoom(numeroCommentaire, monCoeffZoomTexte);
					appliqueZoom(commentaire, monCoeffZoomTexte);
				}
			}
		}
		return convertView;
	}

	/**
	 * Applique le zoom sur la textview (respect des proportions originales)
	 * 
	 * @param uneTextView
	 * @param unZoom
	 */
	private void appliqueZoom(TextView uneTextView, float unZoom) {
		float tailleOrigine = uneTextView.getTextSize();
		float nouvelleTaille = tailleOrigine * unZoom;
		uneTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, nouvelleTaille);
	}

}

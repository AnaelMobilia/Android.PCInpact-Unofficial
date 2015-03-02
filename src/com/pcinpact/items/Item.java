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
package com.pcinpact.items;

/**
 * Objet g�n�rique
 * @author Anael
 *
 */
public interface Item {

	int TYPE_ARTICLE = 0;
	int TYPE_SECTION = 1;
	int TYPE_COMMENTAIRE = 2;

	// Nombre de types diff�rents existants
	int NOMBRE_DE_TYPES = 3;
	
	/**
	 * Type (Cf Item.type*) de l'item
	 * @return
	 */
	int getType();
}

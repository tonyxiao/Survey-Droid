/*---------------------------------------------------------------------------*
 * User.java                                                                 *
 *                                                                           *
 * Represents one of the web-end users.  Currently unused; left in for       *
 * future work, though this class is likely to be removed.                   *
 *---------------------------------------------------------------------------*/
package com.peoples.android.model;

/**
 * @author Diego
 * 
 * Following the database creator
 *
 */
//FIXME remove this when the class is used
@SuppressWarnings("unused")
public class User {
	
	//id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
	private int id;
	
	//username VARCHAR(20) NOT NULL UNIQUE,
	private String username;
	
	//email VARCHAR(320) NOT NULL UNIQUE,
	private String email;
	
	//password CHAR(41) NOT NULL,
	private String password;
	
	//first_name VARCHAR(255),
	private String first_name;
	
	//last_name VARCHAR(255),
	private String last_name;
	
	//admin TINYINT(1) DEFAULT 0);
	private boolean admin;
}

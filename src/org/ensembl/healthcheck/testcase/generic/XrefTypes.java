/*
 * Copyright (C) 2003 EBI, GRL
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.util.DBUtils;

/**
 * Check that all xrefs for a particular external_db map to one and only one ensembl object type.
 */

public class XrefTypes extends SingleDatabaseTestCase {

	/**
	 * Create a new XrefTypes testcase.
	 */
	public XrefTypes() {

		addToGroup("post_genebuild");
		addToGroup("release");
		addToGroup("core_xrefs");
		addToGroup("post-compara-handover");
		
		setDescription("Check that all xrefs only map to one ensembl object type.");
		setTeamResponsible(Team.CORE);
		setTeamResponsible(Team.GENEBUILD);
	}

	/**
	 * Run the test.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return true if the test passed.
	 * 
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		Connection con = dbre.getConnection();

		try {

			Statement stmt = con.createStatement();

			// Query returns all external_db_id-object type relations
			// execute it and loop over each row checking for > 1 consecutive row with same ID

			ResultSet rs = stmt
					.executeQuery("SELECT x.external_db_id, ox.ensembl_object_type, COUNT(*), e.db_name FROM xref x, object_xref ox, external_db e WHERE x.xref_id = ox.xref_id AND e.external_db_id = x.external_db_id GROUP BY x.external_db_id, ox.ensembl_object_type");

			try {
				long previousID = -1;
				String previousType = "";
	
				while (rs != null && rs.next()) {
	
					long externalDBID = rs.getLong(1);
					String objectType = rs.getString(2);
					// int count = rs.getInt(3);
					String externalDBName = rs.getString(4);
	
					if (externalDBID == previousID) {
	
						ReportManager.problem(this, con, "External DB with ID " + externalDBID + " (" + externalDBName + ") is associated with " + objectType + " as well as " + previousType);
						result = false;
	
					}
	
					previousType = objectType;
					previousID = externalDBID;
	
				} // while rs
			}
			finally {
				DBUtils.closeQuietly(rs);
			}

			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (result) {

			ReportManager.correct(this, con, "All external dbs are only associated with one Ensembl object type");

		}

		return result;

	} // run

	// ----------------------------------------------------------------------

} // XrefTypes

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;

/**
 * Base class to compare a certain set of things (e.g. biotypes, xrefs) from on
 * database with the equivalent things in the previous database.
 * 
 * Extending classes should implement the description, threshold and getCounts()
 * methods. See individual Javadocs for details.
 */

public abstract class ComparePreviousVersionBase extends SingleDatabaseTestCase {

	/**
	 * Run the test.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return true if the test pased.
	 * 
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		DatabaseRegistryEntry sec = getEquivalentFromSecondaryServer(dbre);

		if (sec == null) {
			logger.warning("Can't get equivalent database for " + dbre.getName());
			return true;
		}

		logger.finest("Equivalent database on secondary server is " + sec.getName());

		Map currentCounts = getCounts(dbre);
		Map secondaryCounts = getCounts(sec);

		// compare each of the secondary (previous release, probably) with current
		Set externalDBs = secondaryCounts.keySet();
		Iterator it = externalDBs.iterator();
		while (it.hasNext()) {

			String key = (String) it.next();

			int secondaryCount = ((Integer) (secondaryCounts.get(key))).intValue();

			// check it exists at all
			if (currentCounts.containsKey(key)) {

				int currentCount = ((Integer) (currentCounts.get(key))).intValue();

				if (((double) currentCount / (double) secondaryCount) < threshold()) {
					ReportManager.problem(this, dbre.getConnection(), sec.getName() + " contains " + secondaryCount + " " + description()
							+ " of type " + key + " but " + dbre.getName() + " only has " + currentCount);
					result = false;
				} else {

					ReportManager.correct(this, dbre.getConnection(), sec.getName() + " contains " + secondaryCount + " " + description()
							+ " of type " + key + " and " + dbre.getName() + " has " + currentCount + " - greater or within tolerance");

				}

			} else {
				ReportManager.problem(this, dbre.getConnection(), sec.getName() + " contains " + secondaryCount + " " + description
						+ " of type " + key + " but " + dbre.getName() + " has none");
				result = false;
			}
		}
		return result;

	} // run

//----------------------------------------------------------------------

	protected Map getCountsBySQL(DatabaseRegistryEntry dbre, String sql) {

		Map result = new HashMap();

		try {

			Statement stmt = dbre.getConnection().createStatement();

			logger.finest("Getting " + description() + " counts for " + dbre.getName());

			ResultSet rs = stmt.executeQuery(sql);

			while (rs != null && rs.next()) {
				result.put(rs.getString(1), new Integer(rs.getInt(2)));
				logger.finest(rs.getString(1) + " " + rs.getInt(2));
			}

			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Should return a map where the keys are the names of the entities being
	 * tested (e.g. biotypes) and the values are the counts of each type.
	 */
	protected abstract Map getCounts(DatabaseRegistryEntry dbre);

	// ------------------------------------------------------------------------
	/**
	 * Should return a text description of the type of entity being tested.
	 */
	protected abstract String description();

	// ------------------------------------------------------------------------
	/**
	 * Should return the fraction (0-1) of old/new below which a warning is
	 * generated.
	 */
	protected abstract double threshold();

	// ------------------------------------------------------------------------

} // ComparePreviousVersionBase

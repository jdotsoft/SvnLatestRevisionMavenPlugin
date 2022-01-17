/*
 * Copyright (C) 2022 JDotSoft. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jdotsoft.svnrevision;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Provide latest project SVN revision as a Maven property.
 */
@Mojo(name="run", defaultPhase=LifecyclePhase.INITIALIZE)
public class SvnLatestRevisionMojo extends AbstractMojo {
  private static final String PROPERTY_MAVEN_BUILD_TIMESTAMP_FORMAT = "maven.build.timestamp.format";
  private static final String PROPERTY_LOCAL_BUILD_TIMESTAMP = "local.build.timestamp";
  private static final String PROPERTY_SVN_LATEST_REVISION = "svn.latest.revision";
  private static final String PROPERTY_SVN_LATEST_TIMESTAMP = "svn.latest.timestamp";
  private static final String DB_TABLE_NODES = "nodes";
  private static final String DB_COLUMN_REVISION = "revision";
  private static final String DB_COLUMN_CHANGED_DATE = "changed_date";
  private static final String DB_COLUMN_REPOS_PATH = "repos_path";
  private static final String SQL = "SELECT * FROM " + DB_TABLE_NODES
      + " WHERE " + DB_COLUMN_REVISION + " = (SELECT MAX(" + DB_COLUMN_REVISION + ") FROM " + DB_TABLE_NODES + ")"
      + " ORDER BY " + DB_COLUMN_REPOS_PATH;

  @Parameter(defaultValue="${project}", required=true, readonly=true)
  private MavenProject project;

  @Parameter(defaultValue="dd-MMM-yyyy HH:mm", required=false, readonly=true)
  private String timestampFormat;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String fileDB = project.getBasedir() + File.separator + ".svn" + File.separator + "wc.db";
    Log logger = getLog();
    logger.info("Opening SQLite SVN database file " + fileDB);
    SimpleDateFormat fmt = new SimpleDateFormat(timestampFormat);
    Properties prop = project.getProperties();
    prop.setProperty(PROPERTY_LOCAL_BUILD_TIMESTAMP,
        new SimpleDateFormat(prop.getProperty(PROPERTY_MAVEN_BUILD_TIMESTAMP_FORMAT)).format(new Date()));
    prop.setProperty(PROPERTY_SVN_LATEST_REVISION, "n/a"); // default - in case of exception
    prop.setProperty(PROPERTY_SVN_LATEST_TIMESTAMP, "");
    try (
      Connection con = DriverManager.getConnection("jdbc:sqlite:" + fileDB);
      Statement stmt = con.createStatement();
      ResultSet rs = stmt.executeQuery(SQL))
    {
      logger.info("Connected to DB. Latest revision:");
      while (rs.next()) {
        // Revision and timestamp are the same for all SQL query records: the last record is effective
        int nRevision = rs.getInt(DB_COLUMN_REVISION);
        long changedDate = rs.getLong(DB_COLUMN_CHANGED_DATE); // check-in UNIX timestamp in sec
        String sTimestamp = changedDate == 0 ? "<timestamp unknown>" : fmt.format(new Date(changedDate / 1000));
        prop.setProperty(PROPERTY_SVN_LATEST_REVISION, Integer.toString(nRevision));
        if (changedDate > 0) {
          prop.setProperty(PROPERTY_SVN_LATEST_TIMESTAMP, sTimestamp);
        }
        logger.info(String.format("  %d %s | %s", nRevision, sTimestamp, rs.getString(DB_COLUMN_REPOS_PATH)));
      }
    } catch (SQLException e) {
      logger.error("Failed to query DB", e);
    }
  }

}

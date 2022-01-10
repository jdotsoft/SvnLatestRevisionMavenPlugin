# SVN Latest Revision Maven Plugin
Simple SVN Latest Revision Maven Plugin.

The **SVN Latest Revision Plugin** is used to retrieve the project latest SVN revision information from the project
`.svn` folder and store it in a Maven properties. The plugin has only one goal `svn-latest-revision:run`
executed in Maven `INITIALIZE` stage. The goal opens SVN SQLite database file in `.svn` project directory
and queries it for the latest revision.

**NOTE:** the SVN latest revision reflects the latest checked-in however the project build may have not committed changes.

After this plugin goal execution the latest revision and its timestamp are stored in the Maven properties:
- `svn.latest.revision`
- `svn.latest.timestamp`

See also [http://www.jdotsoft.com/SvnMavenPlugin.php](http://www.jdotsoft.com/SvnMavenPlugin.php)

The plugin declaration in pom-file:

    <plugin>
      <groupId>com.jdotsoft</groupId>
      <artifactId>svn-latest-revision-maven-plugin</artifactId>
      <version>1.0</version>
      <executions>
        <execution>
          <goals>
            <goal>run</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <timestampFormat>dd-MMM-yyyy HH:ss</timestampFormat>
      </configuration>
    </plugin>

The configuration `timestampFormat` is optional, the default value is `dd-MMM-yyyy HH:ss`

Example plugin console output:

    [INFO] --- svn-latest-revision-maven-plugin:1.0:run (default) @ MyProject ---
    [INFO] Opening SQLite SVN database file C:\projects\MyProject\.svn\wc.db
    [INFO] Connected to DB. Latest revision:
    [INFO]   1929 18-Dec-2021 20:09 | MyProject/src/main/java/com/jdotsoft/demo/Abc.java
    [INFO]   1929 18-Dec-2021 20:09 | MyProject/src/main/java/com/jdotsoft/demo/Xyz.java

Maven properties defined by this plugin execution could be used, for example, in a `MANIFEST.MF` file:

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-jar-plugin</artifactId>
      . . .
      <configuration>
        <archive>
          <manifestEntries>
            <Build-Time>${local.build.timestamp}</Build-Time>
            <SVN-latest-revision>${svn.latest.revision} ${svn.latest.timestamp}</SVN-latest-revision>
          </manifestEntries>
        </archive>
      </configuration>
    </plugin>

### Other features
Maven has a build-in property `maven.build.timestamp` which could be formatted with specified in a POM-file pattern:

    <properties>
      <maven.build.timestamp.format>dd-MMM-yyyy HH:mm</maven.build.timestamp.format>
    </properties>

Maven uses UTC timezone for build timestamp property `maven.build.timestamp`
while SVN latest revision timestamp is in a local timezone.
The time zone discrepancy could lead to a confusion if both timestamps are displayed to a user.

The **SVN Latest Revision Plugin** creates Maven property `local.build.timestamp` similar to `maven.build.timestamp`
reusing the Maven timestamp format `maven.build.timestamp.format`.
The property `local.build.timestamp` could be used as a `maven.build.timestamp`
replacement to make both timestamps in local time zone (see example above).

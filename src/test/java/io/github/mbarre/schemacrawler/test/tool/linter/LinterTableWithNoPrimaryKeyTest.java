/**
 * 
 */
package io.github.mbarre.schemacrawler.test.tool.linter;

import io.github.mbarre.schemacrawler.test.utils.PostgreSqlDatabase;
import io.github.mbarre.schemacrawler.tool.linter.LinterTableWithNoPrimaryKey;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.commons.io.output.StringBuilderWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.tools.executable.Executable;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.lint.LinterRegistry;
import schemacrawler.tools.options.OutputOptions;
import schemacrawler.tools.options.TextOutputFormat;

/**
 * @author mbarre
 */
public class LinterTableWithNoPrimaryKeyTest {

	Logger logger = LoggerFactory.getLogger(LinterTableWithNoPrimaryKeyTest.class);
	private static PostgreSqlDatabase database;

	@BeforeClass
	public static void  init(){
		database = new PostgreSqlDatabase();
		database.setUp(PostgreSqlDatabase.CHANGE_LOG_PRIMARY_KEY_CHECK);
	}

	@Test
	public void testLint() throws Exception{

		final LinterRegistry registry = new LinterRegistry();
		Assert.assertTrue(registry.hasLinter(LinterTableWithNoPrimaryKey.class.getName()));

		final SchemaCrawlerOptions options = new SchemaCrawlerOptions();
		// Set what details are required in the schema - this affects the
		// time taken to crawl the schema
		options.setSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
		options.setTableNamePattern("test_primary_key");
		
		Connection connection = DriverManager.getConnection(PostgreSqlDatabase.CONNECTION_STRING, 
				PostgreSqlDatabase.USER_NAME, database.getPostgresPassword());
		
		final Executable executable = new SchemaCrawlerExecutable("lint");
		try (StringBuilderWriter out = new StringBuilderWriter()) {
			OutputOptions outputOptions = new OutputOptions(TextOutputFormat.json,out);
			executable.setOutputOptions(outputOptions);
			executable.setSchemaCrawlerOptions(options);
			executable.execute(connection);

			Assert.assertNotNull(out.toString());
			JSONObject json = new JSONObject(out.toString().substring(1, out.toString().length()-1)) ;
			Assert.assertNotNull(json.getJSONObject("table_lints"));
			Assert.assertEquals("test_primary_key", json.getJSONObject("table_lints").getString("name"));

			JSONArray lints = json.getJSONObject("table_lints").getJSONArray("lints");

			boolean lintDectected = false;
			
			for (int i=0; i < lints.length(); i++) {
				if(LinterTableWithNoPrimaryKey.class.getName().equals(lints.getJSONObject(i).getString("id"))){
					if("test_primary_key".equals(lints.getJSONObject(i).getString("value").trim())){
						Assert.assertEquals("table should have a primary key", lints.getJSONObject(i).getString("description").trim());
						Assert.assertEquals("high", lints.getJSONObject(i).getString("severity").trim());
						lintDectected = true;
					}
					else{
						Assert.fail("Not expected error detected :"+lints.getJSONObject(i).getString("value").trim());
					}
				}
			}

			Assert.assertTrue("Some expected errors have not been detected.", lintDectected);
		}

	}

}

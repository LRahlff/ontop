package it.unibz.inf.ontop.rdf4j.query.impl;

import com.google.common.collect.ImmutableMultimap;
import it.unibz.inf.ontop.answering.connection.impl.SQLQuestStatement;
import it.unibz.inf.ontop.query.RDF4JQueryFactory;
import it.unibz.inf.ontop.query.SelectQuery;
import it.unibz.inf.ontop.exception.OntopQueryAnsweringException;
import it.unibz.inf.ontop.query.resultset.TupleResultSet;

import it.unibz.inf.ontop.answering.connection.OntopConnection;
import it.unibz.inf.ontop.answering.connection.OntopStatement;
import it.unibz.inf.ontop.injection.OntopSystemSettings;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.List;


public class OntopTupleQuery extends AbstractOntopQuery implements TupleQuery {

	private final RDF4JQueryFactory factory;

	public OntopTupleQuery(String queryString, ParsedQuery parsedQuery, String baseIRI, OntopConnection conn,
                           ImmutableMultimap<String, String> httpHeaders, RDF4JQueryFactory factory, OntopSystemSettings settings) {
		super(queryString, baseIRI, parsedQuery, conn, httpHeaders, settings);
		this.factory = factory;
	}

    @Override
	public TupleQueryResult evaluate() throws QueryEvaluationException {
		TupleResultSet res;
		OntopStatement stm;
		long start = System.currentTimeMillis();

		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[20];
		random.nextBytes(salt);

		try {
			stm = conn.createStatement();
			if(this.queryTimeout > 0)
				stm.setQueryTimeout(this.queryTimeout);
			long python_start = System.currentTimeMillis();
			String lastline = "";
			try{
				ProcessBuilder pb = new ProcessBuilder("python3","../python-addon/main.py", getQueryString());
				Process p = pb.start();
				BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = bri.readLine()) != null) {
//						 System.out.println(line);
					lastline = line;
				}
				bri.close();
				int exitValue = p.waitFor();
				if (exitValue != 0) {
					System.out.println("Python script didnt returned well " + exitValue);
				}
			}
			catch(Exception e){
				System.out.println("Inserting values didnt work " + e);
			}
			long prepare_start = System.currentTimeMillis();

			conn.setAutoCommit(false);
			SQLQuestStatement stmt = null;
			if (stm instanceof SQLQuestStatement && lastline.length() > 0) {
				String command = lastline.replaceAll("'", "''");
				stmt = (SQLQuestStatement) stm;
				stmt.executeSQL("SELECT create_temp_tables('" + command + "');");
//					stm.executeSQL("SELECT create_temp_tables('" + command + "');");
//					try {
//						java.sql.ResultSet rs = stm.executeSQLRequest("SELECT * FROM concr_parameter_new_only;");
//						while (rs.next()) {
//							System.out.println("  1    " + rs.getString(1) + " , " + rs.getString(2) + " , " + rs.getString(3) + " , " + rs.getString(4) + " , " + rs.getString(5) + " , " + rs.getString(6) + " , " + rs.getString(7));
//						}
//						rs = stm.executeSQLRequest("SELECT * FROM new_mat_samples;");
//						while (rs.next()) {
//							System.out.println("  11    " + rs.getString(1));
//						}
//					} catch (OntopQueryEvaluationException e) {
//						throw new RuntimeException(e);
//					} catch (SQLException e) {
//						throw new RuntimeException(e);
//					}
			}
			long execute_start = System.currentTimeMillis();

			try {
				SelectQuery inputQuery = factory.createSelectQuery(getQueryString(), getParsedQuery(), bindings);
				res = stm.execute(inputQuery, getHttpHeaders());

				conn.commit();
				long execute_end = System.currentTimeMillis();
				System.out.println("Time for getting prepare request: " + (prepare_start-python_start)/1000L + " , execute prepare request: " + (execute_start-prepare_start)/1000L + " , getting results: " + (execute_end-execute_start)/1000L);
			} catch (OntopQueryAnsweringException e) {
				long end = System.currentTimeMillis();
				if (this.queryTimeout > 0 && (end - start) >= this.queryTimeout * 1000){
					throw new QueryEvaluationException("OntopTupleQuery timed out. More than " + this.queryTimeout + " seconds passed", e);
				} else 
					throw e;
			}
			
			List<String> signature = res.getSignature();
			return new OntopTupleQueryResult(res, signature, salt);

		} catch (QueryEvaluationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}

    @Override
	public void evaluate(TupleQueryResultHandler handler) 
			throws QueryEvaluationException, TupleQueryResultHandlerException {
		TupleQueryResult result = evaluate();
		handler.startQueryResult(result.getBindingNames());
		while (result.hasNext()) {
			handler.handleSolution(result.next());
		}
		handler.endQueryResult();
	}
}
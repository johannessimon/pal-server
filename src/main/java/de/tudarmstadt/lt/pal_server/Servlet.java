package de.tudarmstadt.lt.pal_server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.tudarmstadt.lt.pal.KnowledgeBaseConnector;
import de.tudarmstadt.lt.pal.Query;
import de.tudarmstadt.lt.pal.QueryMapper;
import de.tudarmstadt.lt.pal.stanford.StanfordDependencyParser;
import de.tudarmstadt.lt.pal.stanford.StanfordPseudoQueryBuilder;
import edu.stanford.nlp.semgraph.SemanticGraph;

/**
 * Servlet implementation class Test
 */
@WebServlet(value="/ask", loadOnStartup=1)
public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	KnowledgeBaseConnector kb = new KnowledgeBaseConnector("http://localhost:8890/sparql/");
	QueryMapper tripleMapper = new QueryMapper(kb);
	StanfordPseudoQueryBuilder pseudoQueryBuilder = new StanfordPseudoQueryBuilder(kb);
	StanfordDependencyParser depParser = new StanfordDependencyParser("/Volumes/Bill/No-Backup/stanford-parser-tmp");
	JsonUtil json = new JsonUtil();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.setContentType("application/json");
    	PrintWriter writer = response.getWriter();
    	String query = request.getParameter("q");
    	JsonObjectBuilder builder = Json.createObjectBuilder();
    	if (query != null && !query.isEmpty()) {
	    	builder.add("query", query);
	
			SemanticGraph dependencies = depParser.parse(query);
			Query pseudoQuery = pseudoQueryBuilder.buildPseudoQuery(dependencies);
			System.out.println(pseudoQuery);
			if (pseudoQuery != null && pseudoQuery.focusVar != null) {
				Query queryInterpretation = tripleMapper.getBestSPARQLQuery(pseudoQuery);
				if (queryInterpretation != null && queryInterpretation.focusVar != null) {
			    	builder.add("query_interpretation", json.queryToJson(queryInterpretation));
			    	builder.add("sparql_query", kb.queryToSPARQL(queryInterpretation));
					try {
						Collection<String> answers = kb.query(queryInterpretation);
			    		builder.add("answers", json.stringListToJson(answers));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
    	}
		String json = builder.build().toString();
    	writer.println(json);
    	writer.close();			
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}

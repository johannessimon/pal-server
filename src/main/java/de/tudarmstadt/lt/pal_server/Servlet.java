package de.tudarmstadt.lt.pal_server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import de.tudarmstadt.lt.pal.KnowledgeBaseConnector;
import de.tudarmstadt.lt.pal.KnowledgeBaseConnector.Answer;
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
	private class SparqlEndpointConnector {
		KnowledgeBaseConnector kb;
		QueryMapper queryMapper;
	}
	
	Map<String, SparqlEndpointConnector> sparqlEndpointConnectors = new HashMap<String, SparqlEndpointConnector>();
	
	public Servlet() {
		try {
			List<String> files = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream("sparql_endpoints/"), "UTF-8");
			loadSparqlEndpointConnectors(files);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSparqlEndpointConnectors(Collection<String> files) {
		for (String pFileStr : files) {
			InputStream propFile = getClass().getClassLoader().getResourceAsStream("sparql_endpoints/" + pFileStr);
			try {
				SparqlEndpointConnector sep = new SparqlEndpointConnector();
				String id = pFileStr.substring(0, pFileStr.length() - ".properties".length());
				sep.kb = new KnowledgeBaseConnector(propFile);
				sep.queryMapper = new QueryMapper(sep.kb);
				sparqlEndpointConnectors.put(id, sep);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static final long serialVersionUID = 1L;
	StanfordPseudoQueryBuilder pseudoQueryBuilder = new StanfordPseudoQueryBuilder();
	StanfordDependencyParser depParser = new StanfordDependencyParser(/*"/Volumes/Bill/No-Backup/stanford-parser-tmp"*/);
	JsonUtil json = new JsonUtil();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.setContentType("application/json");
    	PrintWriter writer = response.getWriter();
    	String query = request.getParameter("q");
    	String sparqlEndpoint = request.getParameter("p");
    	query = URLDecoder.decode(query, "UTF-8");
    	sparqlEndpoint = URLDecoder.decode(sparqlEndpoint, "UTF-8");
		SemanticGraph dependencies = null;
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (query != null) {
			dependencies = depParser.parse(query);
			String dot = dependencies.toDotFormat().replace("\n", " ");
			builder.add("dependency_tree", dot);
	    	builder.add("query", query);
		}
		SparqlEndpointConnector sep = sparqlEndpointConnectors.get(sparqlEndpoint);
    	if (sparqlEndpoint != null && query != null && !query.isEmpty() && sep != null) {
			Query pseudoQuery = pseudoQueryBuilder.buildPseudoQuery(dependencies);
			System.out.println(pseudoQuery);
			boolean hasQueryInterpretation = false;
			if (pseudoQuery != null && pseudoQuery.focusVar != null) {
				Query queryInterpretation = sep.queryMapper.getBestSPARQLQuery(pseudoQuery);
				if (queryInterpretation != null && queryInterpretation.focusVar != null) {
					hasQueryInterpretation = true;
			    	builder.add("query_interpretation", json.queryToJson(queryInterpretation));
			    	builder.add("sparql_query", StringEscapeUtils.escapeHtml(sep.kb.queryToSPARQLFull(queryInterpretation)));
					try {
						Collection<Answer> answers = sep.kb.query(queryInterpretation);
			    		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
			    		for(Answer a : answers) {
			    			String s = a.value;
			    			JsonObjectBuilder b = Json.createObjectBuilder();
			    			b.add("uri", s);
			    			b.add("label", sep.kb.getResourceLabel(s));
			    			arrBuilder.add(b.build());
			    		}
			    		builder.add("answers", arrBuilder.build());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (!hasQueryInterpretation && pseudoQuery != null) {
		    	builder.add("pseudo_query", json.queryToJson(pseudoQuery));
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

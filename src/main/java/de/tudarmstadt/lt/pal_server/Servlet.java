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
//import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import de.tudarmstadt.lt.pal.KnowledgeBaseConnector;
import de.tudarmstadt.lt.pal.KnowledgeBaseConnector.Answer;
import de.tudarmstadt.lt.pal.Query;
import de.tudarmstadt.lt.pal.QueryMapper;
import de.tudarmstadt.lt.pal.stanford.StanfordDependencyParser;
import de.tudarmstadt.lt.pal.stanford.StanfordPseudoQueryBuilder;
import edu.stanford.nlp.semgraph.SemanticGraph;

/**
 * Web backend for PAL responding with JSON to POST queries
 */
//@WebServlet(value="/ask", loadOnStartup=1)
public class Servlet extends HttpServlet {
	private class SparqlEndpointConnector {
		KnowledgeBaseConnector kb;
		QueryMapper queryMapper;
	}
	
	Map<String, SparqlEndpointConnector> sparqlEndpointConnectors = new HashMap<String, SparqlEndpointConnector>();
	
	Logger log = Logger.getLogger("de.tudarmstadt.lt.pal_server");
	
	public Servlet() {
		try {
			log.info("Initializing servlet...");
			log.info("Loading sparql_endpoints/ dir in classpath...");
			InputStream is = getClass().getClassLoader().getResourceAsStream("sparql_endpoints/");
			List<String> files = IOUtils.readLines(is, "UTF-8");
			loadSparqlEndpointConnectors(files);
			log.info("Done loading sparql_endpoints/ dir. Endpoint configurations loaded: " + sparqlEndpointConnectors.keySet());
		} catch (Exception e) {
			log.error("Error loading sparql_endpoints/ dir (" + e.getClass().getCanonicalName() + "): " + e.getMessage());
			throw new RuntimeException("Error reading sparql_endpoints/ directory", e);
		}
		log.info("Servlet initialized.");
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
			} catch (Exception e) {
				throw new RuntimeException("Error reading SPARQL endpoint configuration file (sparql_endpoints/" + pFileStr + ")", e);
			}
		}
	}
	
	private static final long serialVersionUID = 1L;
	StanfordPseudoQueryBuilder pseudoQueryBuilder = new StanfordPseudoQueryBuilder();
	StanfordDependencyParser depParser = new StanfordDependencyParser(/*"/Volumes/Bill/No-Backup/stanford-parser-tmp"*/);
	JsonUtil json = new JsonUtil();
	
	private boolean stringIsUri(String candidateUri) {
		return candidateUri.startsWith("http://");
	}
	
	private String getRequestString(HttpServletRequest request) {
		String str = request.getRequestURL().toString();
		if (request.getQueryString() != null) {
			str += '?' + request.getQueryString();
		}
		return str;
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("Servlet received GET request with URL " + getRequestString(request));
    	response.setContentType("application/json");
    	PrintWriter writer = response.getWriter();
    	String query = request.getParameter("q");
    	String sparqlEndpoint = request.getParameter("p");
    	if (sparqlEndpoint != null) {
    		sparqlEndpoint = URLDecoder.decode(sparqlEndpoint, "UTF-8");
    	}
		SemanticGraph dependencies = null;
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (query != null) {
	    	query = URLDecoder.decode(query, "UTF-8");
			dependencies = depParser.parse(query);
			String dot = dependencies.toDotFormat().replace("\n", " ");
			builder.add("dependency_tree", dot);
	    	builder.add("query", query);
			log.info("Query from GET request: " + query);
		} else {
			log.warn("GET request contained no query: " + getRequestString(request));
		}
		SparqlEndpointConnector sep = sparqlEndpointConnectors.get(sparqlEndpoint);
    	if (sparqlEndpoint != null && query != null && !query.isEmpty() && sep != null) {
			Query pseudoQuery = pseudoQueryBuilder.buildPseudoQuery(dependencies);
			log.info("Pseudo query from GET request: " + pseudoQuery);
			boolean hasQueryInterpretation = false;
			if (pseudoQuery != null && pseudoQuery.focusVar != null) {
				Query queryInterpretation = sep.queryMapper.getBestSPARQLQuery(pseudoQuery);
				if (queryInterpretation != null && queryInterpretation.focusVar != null) {
					hasQueryInterpretation = true;
			    	builder.add("query_interpretation", json.queryToJson(queryInterpretation));
			    	builder.add("sparql_query", StringEscapeUtils.escapeHtml(sep.kb.queryToSPARQLFull(queryInterpretation)));
					Collection<Answer> answers = sep.kb.query(queryInterpretation);
		    		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
		    		for(Answer a : answers) {
		    			String val = a.value;
		    			String label = a.label != null ? a.label : a.value;
		    			JsonObjectBuilder b = Json.createObjectBuilder();
		    			b.add("label", label);
		    			JsonObjectBuilder _b = Json.createObjectBuilder();
		    			if (stringIsUri(val)) {
		    				b.add("uri", val);
			    			_b.add("resource", b.build());
		    			} else {
			    			_b.add("literal", b.build());
		    			}
		    			arrBuilder.add(_b.build());
		    		}
		    		builder.add("answers", arrBuilder.build());
				} else if (queryInterpretation != null && queryInterpretation.focusVar == null) {
					log.warn("Pseudo query has no focus variable.");
				}
			}
			if (!hasQueryInterpretation && pseudoQuery != null) {
		    	builder.add("pseudo_query", json.queryToJson(pseudoQuery));
			}
    	}
		String json = builder.build().toString();
    	writer.println(json);
    	writer.close();		
		log.info("Done processing GET request with URL " + getRequestString(request));	
    }
}

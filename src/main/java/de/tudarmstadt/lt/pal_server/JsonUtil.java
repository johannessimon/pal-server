package de.tudarmstadt.lt.pal_server;

import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang.StringEscapeUtils;

import de.tudarmstadt.lt.pal.MappedString.TraceElement;
import de.tudarmstadt.lt.pal.Query;
import de.tudarmstadt.lt.pal.Triple;
import de.tudarmstadt.lt.pal.Triple.Element;
import de.tudarmstadt.lt.pal.Triple.TypeConstraint;
import de.tudarmstadt.lt.pal.Triple.Variable;

/**
 * Helper class for building JSON from a collection of relevant classes from PAL
 */
public class JsonUtil {
	JsonObject queryToJson(Query pq) {
		JsonObjectBuilder builder = Json.createObjectBuilder();

		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
		for (Triple t : pq.triples) {
			arrBuilder.add(sparqlTripleToJson(t));
		}
		builder.add("triples", arrBuilder);
		JsonArrayBuilder varListBuilder = Json.createArrayBuilder();
		for (Variable var : pq.vars.values()) {
			varListBuilder.add(elementToJson(var));
		}
		builder.add("vars", varListBuilder);
		if (pq.focusVar != null) {
			builder.add("focusVar", StringEscapeUtils.escapeHtml(pq.focusVar.name));
		}
		return builder.build();
	}
	
	JsonObject elementToJson(Element e) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (e != null) {
			builder.add("name", StringEscapeUtils.escapeHtml(e.name));
			if (e.trace != null) {
				builder.add("trace", traceToJson(e.trace));
			}
			if (e instanceof Variable) {
				Variable v = (Variable)e;
				if (v.mappedType != null) {
					builder.add("type", typeToJson(v.mappedType));
				}
			}
		} else {
			builder.add("name", "null");
		}
		return builder.build();
	}
	
	JsonObject typeToJson(TypeConstraint t) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (t.typeURI != null) {
			builder.add("uri", StringEscapeUtils.escapeHtml(t.typeURI.value));
			builder.add("trace", traceToJson(t.typeURI.trace));
		}
		return builder.build();
	}
	
	JsonArray sparqlTripleToJson(Triple t) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		builder.add(elementToJson(t.subject));
		builder.add(elementToJson(t.predicate));
		builder.add(elementToJson(t.object));
		return builder.build();
	}
	
	JsonArray traceToJson(Collection<TraceElement> c) {
		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
		for(TraceElement e : c) {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			JsonObjectBuilder _builder = Json.createObjectBuilder();
			_builder.add("value", StringEscapeUtils.escapeHtml(e.value));
			if (e.url != null && !e.url.isEmpty()) {
				_builder.add("url", StringEscapeUtils.escapeHtml(e.url));
				builder.add("with-url", _builder.build());
			} else {
				builder.add("without-url", _builder.build());
			}
			arrBuilder.add(builder.build());
		}
		return arrBuilder.build();
	}
	
	JsonArray stringListToJson(Collection<String> c) {
		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
		for(String s : c) {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add("e", StringEscapeUtils.escapeHtml(s));
			arrBuilder.add(builder.build());
		}
		return arrBuilder.build();
	}
}

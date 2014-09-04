package de.tudarmstadt.lt.pal_server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.tudarmstadt.lt.pal.NLI;

/**
 * Servlet implementation class Test
 */
@WebServlet("/ask")
public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	NLI nli = new NLI();

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	PrintWriter writer = response.getWriter();
    	String query = request.getParameter("q");
    	writer.println("<html>");
    	writer.println("<head><title>Hello World Servlet</title></head>");
    	writer.println("<body>");
    	writer.println("	<h1>" + query + "</h1>");
    	if (query != null) {
    		Collection<String> answers = nli.run(query);
    		for (String url : answers) {
    			writer.println("<a href=" + url + ">" + url + "</a><br/>");
    		}
    	}
    	writer.println("<body>");
    	writer.println("</html>");
    		
    	writer.close();			
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}

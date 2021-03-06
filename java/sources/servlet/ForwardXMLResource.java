package servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Forward the content of tha url givan as the target parameter
 * Works only if the request content is text/xml and if the data length
 * is less than  100000bytes
 * @author michel
 *
 */
public class ForwardXMLResource extends RootServlet {
	private static final long serialVersionUID = 1L;

	public static final int MAX_LENGTH = 100000;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}
	private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		BufferedInputStream bis = null;
		this.printAccess(request, true);
		try {
			String target = request.getParameter("target");
			if( target.startsWith("http:")) {
				URL url = new URL(target);
				URLConnection conn = url.openConnection();
				if( conn.getContentType() != null && (conn.getContentType().startsWith("text/xml") || conn.getContentType().startsWith("application/x-votable+xml"))) {
					logger.info("Forward URL " + target + " of type " +  conn.getContentType());
					this.forwardContent(conn, response);
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Request of type " + conn.getContentType() + " cannot be forwarded"  );
				}
			} else 	if( target.startsWith("https:")) {
				URL url = new URL(target);
				HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
				if( conn.getContentType() != null && (conn.getContentType().startsWith("text/xml") || conn.getContentType().startsWith("application/x-votable+xml"))) {
					logger.info("Forward secure URL " + target + " of type " +  conn.getContentType());
					this.forwardContent(conn, response);
				} else {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Secure request of type " + conn.getContentType() + " cannot be forwarded"  );
				}
			} else if( target.startsWith("https:")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot forward secure URL  " + target);
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,
						target + " is not a valid URL: can't be forwarded"  );
			}

		} catch (Exception e2) {
			e2.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					e2.getMessage());
		} finally {
			if( bis != null ) bis.close();
		}

	}

	/**
	 * Forward the content returned by conn to response. Nothing checked here
	 * @param conn : URL connection (http or https)
	 * @param response: servlet response
	 * @throws IOException
	 */
	private final void forwardContent(URLConnection conn, HttpServletResponse response) throws IOException{
		OutputStream out = response.getOutputStream();
		BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
		response.setContentType("text/xml");
		response.setHeader("Content-Length" , conn.getHeaderField("Content-Length"));

		byte[] buffer = new byte[10000];
		int lt=0 , l;
		while ( (l = bis.read(buffer)) != -1 ) {
			lt += l;
			if( lt > MAX_LENGTH ) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Forwarding XML message longer than " + MAX_LENGTH  + " is not allowed" );
				return;
			}
			out.write(buffer, 0, l);
		}
		out.flush();
	}
}

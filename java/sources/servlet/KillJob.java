package servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import session.UserSession;
import session.UserTrap;

/**
 * Servlet implementation class KillJob
 * @version $Id$
 */
public class KillJob extends RootServlet implements Servlet {
	private static final long serialVersionUID = 1L;

 	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		printAccess(request, false);
		response.setContentType("application/json; charset=UTF-8");
		try {
			String nodeKey = this.getParameter(request, "node");
			String jobId = this.getParameter(request, "jobid");
			if( nodeKey == null || nodeKey.length() ==  0 ) {
				reportJsonError(request, response, "killjob: no node specified");
				return;
			}
			if( jobId == null || jobId.length() ==  0 ) {
				reportJsonError(request, response, "killjob: no job specified");
				return;
			}
			UserSession session = UserTrap.getUserAccount(request);
			session.deleteJob(nodeKey, jobId);
			//dumpJsonFile(session.getJobSummaryUrlPath(nodeKey, jobId), response);
		} catch (Exception e) {
			this.reportJsonError(request, response, e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}

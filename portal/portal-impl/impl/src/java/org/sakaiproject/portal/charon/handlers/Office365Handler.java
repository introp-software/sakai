package org.sakaiproject.portal.charon.handlers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sakaiproject.tool.api.ActiveTool;
import org.sakaiproject.tool.cover.ActiveToolManager;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;

public class Office365Handler extends BasePortalHandler {
	
	
	public Office365Handler() {
		// TODO Auto-generated constructor stub
		setUrlFragment("office365");
	}
	
	@Override
	public int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session)
			throws PortalHandlerException {
		// TODO Auto-generated method stub
		return doGet(parts,req,res,session);
		
	}

	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session)
			throws PortalHandlerException {
		// TODO Auto-generated method stub
		PrintWriter out;
		try {
			ActiveTool tool = ActiveToolManager.getActiveTool("sakai.oauth2");
			
			out = res.getWriter();
			out.write("<h1>Office 365v handler</h1>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}

}

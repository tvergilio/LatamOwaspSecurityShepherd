package servlets.module.challenge;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import utils.Hash;
import utils.ShepherdLogManager;
import utils.Validate;
import dbProcs.Database;

/**
 * Session Management Challenge Five
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class SessionManagement5 extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(SessionManagement5.class);
	private static String levelName = "Session Management Challenge Five";
	public static String levelHash = "7aed58f3a00087d56c844ed9474c671f8999680556c127a19ee79fa5d7a132e1";
	private static String levelResult = "a15b8ea0b8a3374a1dedc326dfbe3dbae26";
	/**
	 * Users must use this functionality to sign in as an administrator to retrieve the result key.
	 * @param userName Sub schema user name
	 * @param password Sub schema user password
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		HttpSession ses = request.getSession(true);
		if(Validate.validateSession(ses))
		{
			ShepherdLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), ses.getAttribute("userName").toString());
			log.debug(levelName + " servlet accessed by: " + ses.getAttribute("userName").toString());
			PrintWriter out = response.getWriter();  
			out.print(getServletInfo());
			Encoder encoder = ESAPI.encoder();
			String htmlOutput = new String();
			log.debug(levelName + " Servlet Accessed");
			try
			{
				log.debug("Getting Challenge Parameters");
				Object nameObj = request.getParameter("subUserName");
				Object passObj = request.getParameter("subUserPassword");
				String subName = new String();
				String subPass = new String();
				String userAddress = new String();
				if(nameObj != null)
					subName = (String) nameObj;
				if(passObj != null)
					subPass = (String) passObj;
				log.debug("subName = " + subName);
				log.debug("subPass = " + subPass);
				
				log.debug("Getting ApplicationRoot");
				String ApplicationRoot = getServletContext().getRealPath("");
				log.debug("Servlet root = " + ApplicationRoot );
				
				Connection conn = Database.getChallengeConnection(ApplicationRoot, "BrokenAuthAndSessMangChalFive");
				log.debug("Checking credentials");
				PreparedStatement callstmt;
				
				log.debug("Committing changes made to database");
				callstmt = conn.prepareStatement("COMMIT");
				callstmt.execute();
				log.debug("Changes committed.");
				
				callstmt = conn.prepareStatement("SELECT userName, userRole FROM users WHERE userName = ?");
				callstmt.setString(1, subName);
				log.debug("Executing findUser");
				ResultSet resultSet = callstmt.executeQuery();
				//Is the username valid?
				if(resultSet.next())
				{
					log.debug("User found");
					//Is the user an Admin?
					if(resultSet.getString(2).equalsIgnoreCase("admin"))
					{
						log.debug("Admin Detected");
						callstmt = conn.prepareStatement("SELECT userName, userRole FROM users WHERE userName = ? AND userPassword = SHA(?)");
						callstmt.setString(1, subName);
						callstmt.setString(2, subPass);
						log.debug("Executing Login Check");
						ResultSet resultSet2 = callstmt.executeQuery();
						if(resultSet2.next())
						{
							log.debug("Successful Admin Login");
							// Get key and add it to the output
							String userKey = Hash.generateUserSolution(levelResult, (String)ses.getAttribute("userName"));
							
							htmlOutput = "<h2 class='title'>Welcome " + encoder.encodeForHTML(resultSet2.getString(1)) + "</h2>" +
									"<p>" +
									"The result key is <a>" + userKey + "</a>" +
									"</p>";
						}
						else
						{
							userAddress = "Incorrect password for <a>" + encoder.encodeForHTML(resultSet.getString(1)) + "</a><br/>";
							htmlOutput = htmlStart + userAddress + htmlEnd;
						}
					}
					else
					{
						log.debug("Successful Pleb Login");
						htmlOutput = htmlStart + htmlEnd +
								"<h2 class='title'>Welcome Guest</h2>" +
								"<p>No further information for Guest Users currently available. " +
								"If your getting bored of the current functions available, " +
								"you'll just have to upgrade yourself to an administrator somehow.</p><br/><br/>";	
					}
				}
				else
				{
					userAddress = "User name not found.<br/>";
					htmlOutput = htmlStart + userAddress + htmlEnd;
				}
				Database.closeConnection(conn);
				log.debug("Outputting HTML");
				out.write(htmlOutput);
			}
			catch(Exception e)
			{
				out.write("An Error Occurred! You must be getting funky!");
				log.fatal(levelName + " - " + e.toString());
			}
		}
		else
		{
			log.error(levelName + " servlet accessed with no session");
		}
	}
	
	private static String htmlStart = "<table>";
	private static String htmlEnd = "<tr><td>Username:</td><td><input type='text' id='subUserName'/></td></tr>" +
			"<tr><td>Password:</td><td><input type='password' id='subUserPassword'/></td></tr>" +
			"<tr><td colspan='2'><div id='submitButton'><input type='submit' value='Sign In'/>" +
			"</div></td></tr>" +
			"</table>";
}
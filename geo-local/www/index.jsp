<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.browseengine.local.service.LocalService"%>
<%@ page import="com.browseengine.local.service.LocalRequest" %>
<%@ page import="com.browseengine.local.service.LocalResult" %>
<%@ page import="com.browseengine.local.service.LocalResource" %>
<%@ page import="com.browseengine.local.service.Locatable" %>
<%@ page import="com.browseengine.local.service.Address" %>
<%@ page import="com.browseengine.local.service.impl.SingletonLocalServiceFactory" %>

<%
final String ENC = "UTF-8";
final String target = "index.jsp";
final int selectRange = 10;

String addressStr = request.getParameter("address");
addressStr = ((addressStr != null ? addressStr.trim() : ""));
String startStr = request.getParameter("start");
int start = 0;
if (startStr != null && startStr.length() > 0) {
	try {
	start = Integer.parseInt(startStr);		
	} catch (NumberFormatException nfe) {
		// suppress
	}
}
String debugStr = request.getParameter("debug");
boolean isdebug = false;
String selectedN = " selected=\"true\" ";
String selectedY = "";
if (debugStr != null && debugStr.equals("y")) {
	isdebug = true;
	selectedY = selectedN;
	selectedN = "";
}
String rangeStr = request.getParameter("range");
float range = 5f;
String selected1 = "";
String selected5 = " selected=\"true\" ";
String selected10 = "";
String selected25 = "";
String selected50 = "";
String selected100 = "";
String selected500 = "";
String selected3000 = "";
if (rangeStr != null && rangeStr.length() > 0) {
  try {
	range = Float.parseFloat(rangeStr);
	if (1f == range) {
		selected1 = selected5;
		selected5 = "";
	} else if (10f == range) {
		selected10 = selected5;
		selected5 = "";
	} else if (25f == range) {
		selected25 = selected5;
		selected5 = "";
	} else if (50f == range) {
		selected50 = selected5;
		selected5 = "";
	} else if (100f == range) {
		selected100 = selected5;
		selected5 = "";
	} else if (500f == range) {
		selected500 = selected5;
		selected5 = "";
	} else if (3000f == range) {
		selected3000 = selected5;
		selected5 = "";
	}
  } catch (NumberFormatException nfe) {
	// suppress
  }
}

%>

<html>
<head>
<title>
Bobo Local Resource Demo
</title>
</head>
<body>
<h2>Please specify the address and range to search near</h2>

<form action="<%=target%>" method="post">
  <b>please enter an address and choose a distance.</b>
<table>
  <COLGROUP>
   <COL width="2*">
   <COL width="8*">
  <tr>
    <td>
       Address:
    </td>
    <td>
       <input type="text" name="address" size="100" value="<%=addressStr%>" />
    </td>
  </tr>
  <tr>
    <td>
      Range (mi.):
    </td>
    <td>
      <select name="range">
        <option <%=selected1 %> value="1">1</option>
        <option <%=selected5 %> value="5">5</option>
        <option <%=selected10 %> value="10">10</option>
        <option <%=selected25 %> value="25">25</option>
        <option <%=selected50 %> value="50">50</option>
        <option <%=selected100 %> value="100">100</option>
        <option <%=selected500 %> value="500">500</option>
        <option <%=selected3000 %> value="3000">3000</option>
      </select>
    </td>
  </tr>
  <tr>
    <td>
      Debug:
    </td>
    <td>
      <select name="debug">
        <option <%=selectedN %> value="n">n</option>
        <option <%=selectedY %> value="y">y</option>
      </select>
    </td>
  <tr>
    <td colspan="2">
      <input type="submit" value="Geo Search" />
    </td>
  </tr>
</table>
  </form>
  <hr>
  <%
  if (addressStr == null || addressStr.length() <= 0) {
  %>
  <i>Please fill in an address above</i>
  <%
  } else {
	  %>
	  Results for geo local search near &quot;<%=addressStr%>&quot; within a radius of <%=range%> miles.
	  <p>
	  <%
	  LocalService local = null;
	  try {
		LocalRequest localrequest = new LocalRequest();
		localrequest.setAddressStr(addressStr);
		localrequest.setRangeInMiles(range);

		local = SingletonLocalServiceFactory.getLocalServiceImpl();

	LocalResult localresult = local.search(localrequest);
		  if (localresult == null) {
			  %>
			  <b>Error:</b> &nbsp; LocalResult was null!
			  <p>
			  <%
		  } else {
			String centroidStr = ""+(isdebug ? "" : "\n<!--\n")+"Got centroid &quot;"+localresult.getCentroid()+"&quot;.<p>\n"+(isdebug ? "" : "-->\n");  
		  
			  if (localresult.getNumHits() <= 0) {
			  %>
			      <%=centroidStr %>
			      <b>No results found.</b>
			      <p>
			  <%
		      } else {
    			  StringBuilder buf = new StringBuilder();
	    		  int numHit = localresult.getNumHits();
		    	  buf.append(centroidStr);
			      buf.append("Got back <b>").append(numHit).append("</b> hits, as follows:\n<p>");
			      if (start+selectRange < numHit || start > 0) {
				      String args = "?address="+URLEncoder.encode(addressStr, ENC)+"&range="+URLEncoder.encode(rangeStr, ENC);
				      int newStart;
			  	    if (start > 0 && (newStart = start-selectRange) >= 0 
			  			    // this UI is not supporting any results past 100
			  			    && newStart <= 90) {
				      buf.append("<a href=\"").append(target).append(args).append("&start=").append(newStart).append("\">&lt;&lt; Prev</a>&nbsp; \n");
			  	    }
			  	    if ((newStart = start+selectRange) < numHit) {
			  		    buf.append("<a href=\"").append(target).append(args).append("&start=").append(newStart).append("\">Next &gt;&gt;\n");
			  	    }
			  	    buf.append("<br>");
			      }
			      buf.append("<table border=\"1\">\n<tr><b>\n<td>\nRank\n</td><td>Distance(mi.)\n</td>\n<td>Name</td>\n<td>Address</td>\n<td>Phone</td>\n</b></tr>\n");
			      LocalResource[] resources = local.fetch(localresult, start, 10);
			      for (int i = 0; i < resources.length; i++) {
				      LocalResource resource = resources[i];
				      buf.append("<tr>\n<td>").append((start+i+1)).append(".</td><td>").append(resource.getDistanceInMiles()).append("</td>\n<td>").append(resource.getName()).append("</td>\n<td>").append(resource.getAddressStr()).append("</td>\n<td>").append(resource.getPrettyPhoneNumber()).append("</td>\n</tr>\n");
			      }
			      buf.append("</table>\n<p>");
			      %>
			      <%=buf.toString() %>
			      <%
		      }
		  }		  
	  } catch (Throwable t) {
		  t.printStackTrace();
	  %>
	  <b>Geo Search Error:</b> &nbsp; <%=t.toString()%>
	  <p>
	  <%
	  if (isdebug) {
	      if (null == local) {
	    	  %>
	    	  local was null, so we can't just lookup address
	    	  <%
	      } else {
	    	  try {
       			Locatable addr = local.lookupAddress(addressStr);
       			String addrDisp = (addr != null ? addr.toString() : "null");
       			%>
       			<b>lookupAddress Found</b>: <%=addrDisp %>
       			<p>
       			<br>
       			<%
	    	  } catch (Throwable t2) {
	    		  t2.printStackTrace();
	    		  %>
	    		  <b>Lookup Address Error:</b> &nbsp; <%=t2.toString() %>
	    		  <p>
	    		  <%
	    		  try {
	    			  Address addr = local.parseAddress(addressStr);
	    			  String addrDisp = (addr != null ? addr.toString() : "null");
	    			  %>
	    			  <b>parseAddress Found</b>: <%=addrDisp %>
	    			  <p>
	    			  <br>
	    			  <%
	    		  } catch (Throwable t3) {
	    		  t3.printStackTrace();
	    		  %>
	    		  <b>Parse Address Error:</b> &nbsp; <%=t3.toString() %>
	    		  <p>
	    		  <%
	    		  }
	    	  }
	      }
	  }
	  }
  }
  %>
  
  
</body>
</html>

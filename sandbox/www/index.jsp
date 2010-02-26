<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.browseengine.local.service.LocalService"%>
<%@ page import="com.browseengine.local.service.LocalRequest" %>
<%@ page import="com.browseengine.local.service.LocalResult" %>
<%@ page import="com.browseengine.local.service.LocalResource" %>
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
String rangeStr = request.getParameter("range");
float range = 5f;
String selected1 = "";
String selected5 = " selected=\"true\" ";
String selected10 = "";
String selected25 = "";
String selected50 = "";
String selected100 = "";
String selected500 = "";
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
      </select>
    </td>
  </tr>
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
	  try {
		LocalRequest localrequest = new LocalRequest();
		localrequest.setAddressStr(addressStr);
		localrequest.setRangeInMiles(range);
		  
	  LocalService local = SingletonLocalServiceFactory.getLocalServiceImpl();
	LocalResult localresult = local.search(localrequest);
		  if (localresult == null) {
			  %>
			  <b>Error:</b> &nbsp; LocalResult was null!
			  <p>
			  <%
		  } else if (localresult.getNumHits() <= 0) {
			  %>
			  <b>No results found.</b>
			  <p>
			  <%
		  } else {
			  StringBuilder buf = new StringBuilder();
			  int numHit = localresult.getNumHits();
			  buf.append("Got back <b>").append(numHit).append("</b> hits, as follows:\n<p>");
			  if (start+selectRange < numHit || start > 0) {
				  String args = "?address="+URLEncoder.encode(addressStr, ENC)+"&range="+URLEncoder.encode(rangeStr, ENC);
				  int newStart;
			  	if (start > 0 && (newStart = start-selectRange) >= 0 
			  			// not supporting any results past 50
			  			&& newStart <= 40) {
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
		  
	  } catch (Throwable t) {
		  t.printStackTrace();
	  %>
	  <b>Error:</b> &nbsp; <%=t.toString()%>
	  <p>
	  <%
	  }
  }
  %>
  
  
</body>
</html>

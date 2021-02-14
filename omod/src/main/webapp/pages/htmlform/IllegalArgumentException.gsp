<html>
   <body>
   <p>Application has encountered an error. Please contact support on ...</p>
     <% out << "encounter.form is not an HTML Form" %>
      <!--
    Failed URL: ${url}
    Exception:  ${exception.message}
        <c:forEach items="${exception.stackTrace}" var="ste">    ${ste} 
    </c:forEach>
  -->
   </body>
</html>
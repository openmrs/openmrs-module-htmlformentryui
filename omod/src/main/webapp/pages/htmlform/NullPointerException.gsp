<html>
   <body>
   <p>Application has encountered an error. Please contact support on ...</p>
     <% out << " ooppsss!!!! Active drugs cannot be edittted!" %>
      <!--
    Failed URL: ${url}
    Exception:  ${exception.message}
        <c:forEach items="${exception.stackTrace}" var="ste">    ${ste} 
    </c:forEach>
  -->
   </body>
</html>
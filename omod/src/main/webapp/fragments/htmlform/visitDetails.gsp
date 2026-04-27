<%
    ui.includeCss("htmlformentryui", "htmlform/visitDetails.css")
%>

<% if (config.visit) { %>
    <div class="visit-location">
        <i class="icon-hospital small"></i>
        ${ui.format(config.visit.location)}
    </div>

    <div class="visit-dates">
        <span>
            <i class="icon-time small"></i>
            ${ui.format(config.visit.startDatetime)}
            <% if (config.visit.stopDatetime) { %>
            - ${ui.format(config.visit.stopDatetime)}
            <% } else { %>
            <span class="lozenge active">(${ui.message("uicommons.active")})</span>
            <% } %>
        </span>
    </div>
<% } %>
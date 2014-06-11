$(document).ready(function() {
    $('#calendar').fullCalendar({
        firstDay   : 1,
        defaultView: "agendaWeek",
        allDaySlot : false,
        minTime    : "06:00:00",
        maxTime    : "23:00:00",
    });
});

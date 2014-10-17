$(document).ready(function() {
    $('#calendar').fullCalendar({
        firstDay   : 1,
        defaultView: "agendaWeek",
        allDaySlot : false,
        minTime    : "09:00:00",
        maxTime    : "21:00:00",
        timeFormat : "HH:mm",
        header     : {
                        left:   'prev',
                        center: 'agendaWeek,month',
                        right:  'next'
                    }, 
        events     : '/api/v1/conf/all'
    });
});
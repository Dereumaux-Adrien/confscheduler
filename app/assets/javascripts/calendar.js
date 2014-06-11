$(document).ready(function() {
    $('#calendar').fullCalendar({
        firstDay   : 1,
        defaultView: "agendaWeek",
        allDaySlot : false,
        minTime    : "06:00:00",
        maxTime    : "23:00:00",
        timeFormat : "HH:mm",
        header     : {
                        left:   'prev',
                        center: 'agendaWeek,month',
                        right:  'next'
                    }, 
        events     : fixturesEvents()
    });
});

function fixturesEvents() {
    var evs = [
        {
            title  : 'event 1',
            start  : '2014-06-11T12:30:00',
            end    : '2014-06-11T13:30:00'
        },
        {
            title  : 'event 2',
            start  : '2014-06-12T15:30:00',
            end    : '2014-06-12T17:00:00'
        }
    ];

    return evs;
}

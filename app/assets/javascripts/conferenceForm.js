$(function() {
    $("#datepicker").datepicker();
    $("#timepicker").timepicker({
        timeFormat: "G:i",
        minTime: "6:00am",
        maxTime: "9:00pm",
        step:15
    });
});
$(function() {
    document.getElementById('timezoneOffset').value = new Date().getTimezoneOffset();
    $("#datepicker").datepicker({ dateFormat: "yy-mm-dd" });
    $("#timepicker").timepicker({
        timeFormat: "G:i",
        minTime: "6:00am",
        maxTime: "9:00pm",
        step:15
    });
});
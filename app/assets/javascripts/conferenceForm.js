$(function() {
    document.getElementById('timezoneOffset').value = new Date().getTimezoneOffset();
    $("#datepicker").datepicker({ dateFormat: "yy-mm-dd" });
    $("#timepicker").timepicker({
        timeFormat: "G:i",
        minTime: "6:00am",
        maxTime: "9:00pm",
        step:15
    });
    $("#length").timepicker({
        timeFormat: "G:i",
        minTime: "0:15am",
        maxTime: "10:00am",
        step:15
    });
});
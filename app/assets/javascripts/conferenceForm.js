$(function() {
    // Calendar & Time Picker set-up
    document.getElementById('timezoneOffset').value = new Date().getTimezoneOffset();

    $("#datepicker").datepicker({ minDate: 0, dateFormat: "yy-mm-dd" });

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

    // Speaker select & addition set-up
    var speakerSelect = $('#speaker\\.id');
    var newSpeaker = false;

    if(speakerSelect.val() === "-1") {
        $("#new-speaker").show();
        newSpeaker = true;
    }

    var clearNewSpeaker = function() {
        speakerSelect.find('[value=-1]').remove();
        $("#new-speaker").hide();
        newSpeaker = false;
    };
    speakerSelect.change(function(){
        if(speakerSelect.val() !== -1) {
            clearNewSpeaker();
        }
    });

    $("#toggleNewSpeaker").click(function() {
        if(!newSpeaker) {
            speakerSelect.append('<option value="-1" selected="selected">New Speaker</option>');
            $("#new-speaker").show();
            newSpeaker = true;
        } else {
            clearNewSpeaker();
            newSpeaker = false;
        }
    });

    // Location selection & addition set-up
    var newLocation = false;
    var locationSelect = $('#location\\.id');

    if(locationSelect.val() === "-1") {
        $("#new-location").show();
        newLocation = true;
    }

    var clearNewLocation = function() {
        locationSelect.find('[value=-1]').remove();
        $("#new-location").hide();
        newLocation = false;
    };

    locationSelect.change(function(){
        if(locationSelect.val() !== -1) {
            clearNewLocation();
        }
    });

    $("#toggleNewLocation").click(function() {
        if(!newLocation) {
            locationSelect.append('<option value="-1" selected="selected">New Location</option>');
            $("#new-location").show();
            newLocation = true;
        } else {
            clearNewLocation();
            newLocation = false;
        }
    });
});
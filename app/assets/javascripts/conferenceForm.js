$(function() {
    var speakerSelect = $('#speaker');
    var newSpeaker = false;

    if(speakerSelect.val() === -1) {
        newSpeaker = true;
        $("#new-speaker").show();
    }

    var clearNewSpeaker = function() {
        speakerSelect.find('[value=-1]').remove();
        $("#new-speaker").hide();
    };

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
});